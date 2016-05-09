package ingress.submission


import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import com.rabbitmq.client.{QueueingConsumer, Channel, Connection}
import play.api.libs.ws.WS
import play.api.test.{FakeApplication, FakeRequest}
import play.api.test.Helpers._
import com.rabbitmq.client.AMQP.BasicProperties
import app.ConfigProperties
import utils.WithApplication
import scala.concurrent.{Await, TimeoutException, ExecutionContext, Future}
import ExecutionContext.Implicits.global
import submission.SubmissionService
import submission.messaging._
import scala.concurrent.duration._
import app.ConfigProperties._

class SubmissionServiceIntegrationSpec extends Specification with Mockito{

  "Ingress Service" should {

    "Successfully publish a message into a live broker" in new WithServerConfig("queue.name" -> "IngresServiceIntegration1","env.name"->"Test") {
      
      val queueName = TestUtils.declareQueue

      val timestamp = "2013-10-23"

      val endpoint = s"http://localhost:$port/submission"

      val response  = Await.result(WS.url(endpoint).post(<request>{timestamp}</request>),DurationInt(4).seconds)

      try{
        response.status mustEqual OK
      }finally{
       TestUtils.consumeMessage(queueName) must contain(timestamp)
      }


    }

    "Failure for exceeded message capacity" in new WithApplication {

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

      for(i <- 0 to getIntProperty("rabbit.messages.max"))  {
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


    "Failure by createChannel throwing exceptions" in new WithApplication {

      val service:SubmissionService = new SubmissionService {

        override def messagingService: MessageSendingService = new MessageSenderImpl {
          override def getQueueName:String = "IngressServiceIntegrationSpec_4"

          override protected def createChannel(connection: Connection): Channel = {
            throw new Exception("This is a test thrown exception")
          }
        }
      }

      val request = FakeRequest().withXmlBody(<request>{"2013-10-24"}</request>)

      try{
        val response = Future(service.xmlProcessing(request))
        status(response) mustEqual SERVICE_UNAVAILABLE
      }finally {

      }
    }

    "Failure by withChannel throwing exceptions" in new WithApplication {

      val service:SubmissionService = new SubmissionService {

        override def messagingService: MessageSendingService = new MessageSenderImpl {
          override def getQueueName:String = "IngressServiceIntegrationSpec_5"


          protected override def withChannel(f: (Channel) => Result): Result = {
            throw new Exception("This is a test thrown exception")
          }
        }
      }

      val request = FakeRequest().withXmlBody(<request>{"2013-10-24"}</request>)

      try{
        val response = Future(service.xmlProcessing(request))
        status(response) mustEqual SERVICE_UNAVAILABLE
      }finally {

      }
    }

  }
  section("integration")
}
