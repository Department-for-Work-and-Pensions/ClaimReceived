package submission.messaging

import com.rabbitmq.client.{Channel, Connection}
import com.rabbitmq.client.AMQP.BasicProperties
import scala.util.Try
import app.ConfigProperties._
import monitoring.Histograms
import submission.messaging.exceptions.MessageCapacityExceededException

trait MessageSendingService {

  import ConnectionManager.durableProperties

  protected def withConnection(f: Connection => Result): Result

  protected def withChannel(f: Channel => Result): Result

  def sendMessage(msg: String, queue: String, exchange: String = "", properties: BasicProperties = durableProperties): Result

  def getQueueName: String

  def queueStatus(): Result

}


object MessageSender extends MessageSenderImpl

trait MessageSenderImpl extends MessageSendingService {

  import ConnectionManager._

  /**
   * This method is going to manage a connection and send it to the f function
   * @param f Function that receives a Connection and returns a result
   * @return Returns whether if the operation has been successful or a failure
   */
  protected override def withConnection(f: Connection => Result): Result = {
    Try(factory.newConnection()) match {
      case util.Success(connection) =>
        val res = f(connection)
        connection.close()
        res
      case util.Failure(exception) => Failure(exception)
    }
  }

  protected def createChannel(connection:Connection) = connection.createChannel()

  /**
   * This method calls withConnection sending an anonymous function that using a connection
   * will manage a channel and will send it to the f parameter function
   * @param f Function that receives a Channel and returns a result
   * @return Returns whether if the operation has been successful or a failure
   */
  protected override def withChannel(f: Channel => Result): Result = withConnection { connection =>
    Try(createChannel(connection)) match {
      case util.Success(channel) =>
        channel.confirmSelect()
        val res = f(channel)
        val confirmed = channel.waitForConfirms()
        channel.close()
        if (!confirmed) Failure(new RuntimeException("Publishing not confirmed by the broker"))
        else res
      case util.Failure(exception) => Failure(exception)
    }
  }

  /**
   * Sending a message to the rabbitMQ server using [[ingress.submission.messaging.ConnectionManager.durableProperties]]
   * Before sending the message we are going to make sure that the queue is not overloaded checking
   * the current message number with the limit number stored in the config file as ''rabbit.messages.max''
   * @param msg String that is going to be sent to the queue
   * @param queue Queue name or also, queuing route.
   * @param exchange Exchange name
   * @param properties Message properties, using durableProperties by default.
   * @return
   */
  override def sendMessage(msg: String, queue: String, exchange: String = "",
                           properties: BasicProperties = durableProperties): Result = {
    Try(withChannel { channel =>
      val result = channel.queueDeclare(getQueueName, true, false, false, null)
      Histograms.recordQueueMessageCount(result.getMessageCount)
      if (result.getMessageCount >= getProperty(RABBIT_MAX_MESSAGES, 500)) {
        Failure(new MessageCapacityExceededException("Exceeded message capacity in the queue named [" + getQueueName + "]"))
      } else {
        channel.basicPublish(exchange, queue, properties, msg.getBytes("UTF-8"))
        Success
      }
    }) match {

      case util.Success(s@Success) => s
      // Case for internal captured exception, with these, we can close open streams and prevent memory leaks
      case util.Success(f: Failure) => f
      // Something really odd happened and an exception was raised from the method withChannel
      case util.Failure(exception) => Failure(exception)
      case _ => Failure(new RuntimeException("Unexpected match case at sendMessage"))
    }
  }

  override def queueStatus(): Result = {
    Try(withChannel { channel =>
      val result = channel.queueDeclare(getQueueName, true, false, false, null)
      Histograms.recordQueueMessageCount(result.getMessageCount)
      if (result.getMessageCount >= getProperty(RABBIT_MAX_MESSAGES, 500)) {
        Failure(new MessageCapacityExceededException("Exceeded message capacity in the queue named [" + getQueueName + "]"))
      } else {
        Success
      }
    }) match {
      case util.Success(s@Success) => s
      // Case for internal captured exception, with these, we can close open streams and prevent memory leaks
      case util.Success(f: Failure) => f
      // Something really odd happened and an exception was raised from the method withChannel
      case util.Failure(exception) => Failure(exception)
      case _ => Failure(new RuntimeException("Unexpected match case at sendMessage"))
    }
  }

  override def getQueueName: String = getProperty("queue.name","ingress")+"_queue_" + getProperty("env.name", "default")

}
