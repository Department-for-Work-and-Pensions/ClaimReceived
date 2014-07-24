package ingress.submission

import org.specs2.mutable.{Tags, Specification}
import com.rabbitmq.client.{QueueingConsumer, Channel, Connection}
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import com.rabbitmq.client.AMQP.BasicProperties
import app.ConfigProperties
import scala.concurrent.{TimeoutException, ExecutionContext, Future}
import ExecutionContext.Implicits.global
import submission.SubmissionService
import submission.messaging.{ConnectionManager, MessageSenderImpl, MessageSendingService}


class SubmissionServiceIntegrationSpec extends Specification with Tags{

  "Ingress Service" should {

    "Successfully publish a message into a live broker" in new WithApplication {
      
      val service:SubmissionService = new SubmissionService {
        override def messagingService: MessageSendingService = {
          new MessageSenderImpl {
            override def getQueueName:String = "IngressServiceIntegrationSpec_1"
          }
        }
      }
      val queueName = service.messagingService.getQueueName
      var conn: Connection = ConnectionManager.factory.newConnection()
      var channel: Channel = conn.createChannel()
      val declareOk = channel.queueDeclare(queueName,true,false,false,null)
      channel.close()
      conn.close()
      val timestamp = "2013-10-23"
      val request = FakeRequest().withXmlBody(<request>{timestamp}</request>)

      val response = Future(service.xmlProcessing(request))

      try{
        status(response) mustEqual OK
      }finally{
        conn = ConnectionManager.factory.newConnection()
        channel = conn.createChannel()
        val consumer = new QueueingConsumer(channel)
        channel.basicConsume(queueName,true,consumer)
        val delivery = consumer.nextDelivery(2000)
        val body: String = new String(delivery.getBody)
        channel.queueDelete(queueName)
        channel.close()
        conn.close()
        body must contain(timestamp)
      }


    }

    "Failure for exceeded message capacity" in new WithApplication {

      import ConfigProperties._
      val service:SubmissionService = new SubmissionService {
        override def messagingService: MessageSendingService = {
          new MessageSenderImpl {
            override def getQueueName:String = "IngressServiceIntegrationSpec_2"
          }
        }
      }
      val queueName = service.messagingService.getQueueName
      val conn: Connection = ConnectionManager.factory.newConnection()
      var channel: Channel = conn.createChannel()
      val declareOk = channel.queueDeclare(queueName,true,false,false,null)
      channel.confirmSelect()

      for(i <- 0 to getProperty(RABBIT_MAX_MESSAGES,20))  {
        channel.basicPublish("",queueName,new BasicProperties().builder().deliveryMode(2).build(),("Message number "+i).getBytes)
        channel.waitForConfirms()
      }
      channel.close()

      val timestamp =  "2013-10-24"
      val request = FakeRequest().withXmlBody(<request>{timestamp}</request>)

      try{
        val response = Future(service.xmlProcessing(request))
        status(response) mustEqual SERVICE_UNAVAILABLE
      }finally {
        channel = conn.createChannel()
        channel.queuePurge(queueName)
        channel.queueDelete(queueName)
        channel.close()
        conn.close()
      }
    }

    "Failure because connection fails" in new WithApplication{
      ConnectionManager.factory.setUri("amqp://nonexistinghost")

      val service:SubmissionService = new SubmissionService {
        override def messagingService: MessageSendingService = {
          new MessageSenderImpl {
            override def getQueueName:String = "IngressServiceIntegrationSpec_3"
          }
        }
      }

      val request = FakeRequest().withXmlBody(<request></request>)
      try {
        val response = Future(service.xmlProcessing(request))

        status(response) mustEqual SERVICE_UNAVAILABLE
        ConnectionManager.factory.setUri(ConnectionManager.readUri)
      } catch {
        case e: TimeoutException => success
        case _: Throwable => failure
      }
    }

  } section "integration"
}
