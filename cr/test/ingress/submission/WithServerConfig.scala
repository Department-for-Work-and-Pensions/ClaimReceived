package ingress.submission

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{QueueingConsumer, Channel, Connection}
import play.api.test.FakeApplication
import submission.SubmissionService
import submission.messaging.{MessageSender, ConnectionManager}
import utils.WithServer
import play.api._

class WithServerConfig(params:(String,_)*) extends WithServer(app = FakeApplication(additionalConfiguration = params.toMap))

object TestUtils {

  def declareQueue = {

    val service:SubmissionService = new SubmissionService {
      override def messagingService = MessageSender
    }
    val queueName = service.messagingService.getQueueName
    Logger.info(s"cr WithServerConfig declaring queue $queueName")
    val conn: Connection = ConnectionManager.factory.newConnection()
    val channel: Channel = conn.createChannel()
    val declareOk = channel.queueDeclare(queueName,true,false,false,null)
    channel.close()
    conn.close()

    queueName
  }

  def consumeMessage(queueName:String) = {
    val conn = ConnectionManager.factory.newConnection()
    val channel = conn.createChannel()
    val consumer = new QueueingConsumer(channel)
    channel.basicConsume(queueName,true,consumer)
    val delivery = consumer.nextDelivery(2000)
    val body: String = new String(delivery.getBody)
    channel.queueDelete(queueName)
    channel.close()
    conn.close()

    body
  }

  def deleteQueue(queueName:String) = {
    val conn: Connection = ConnectionManager.factory.newConnection()
    val channel: Channel = conn.createChannel()
    val deleteOk = channel.queueDelete(queueName)
    channel.close()
    conn.close()
  }

  def publishMessages(queueName:String,messages:String*) = {
    val conn: Connection = ConnectionManager.factory.newConnection()
    val channel: Channel = conn.createChannel()
    val declareOk = channel.queueDeclare(queueName,true,false,false,null)
    channel.confirmSelect()

    messages.foreach{ message =>
      channel.basicPublish("",queueName,new BasicProperties().builder().deliveryMode(2).build(),message.getBytes)
      channel.waitForConfirms()
    }

    channel.close()
    conn.close()
  }

}

