package ingress.submission

import org.specs2.mock.Mockito
import com.rabbitmq.client.AMQP
import submission.SubmissionService
import submission.messaging.{Success, Failure, MessageSendingService}


trait FailSubmissionServiceMocking extends SubmissionService with Mockito{

  def newException(msg:String):Exception

  override def messagingService: MessageSendingService = {
    val m = mock[MessageSendingService]
    m.sendMessage(anyString,anyString,anyString,any[AMQP.BasicProperties]) returns Failure(newException("Mock exception"))
    m
  }
}

trait SuccessSubmissionServiceMocking extends SubmissionService with Mockito{

  override def messagingService: MessageSendingService = {
    val m = mock[MessageSendingService]
    m.sendMessage(anyString,anyString,anyString,any[AMQP.BasicProperties]) returns Success
    m
  }
}
