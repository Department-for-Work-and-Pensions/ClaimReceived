package ingress.submission

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import play.api.test.FakeRequest
import play.api.test.Helpers._
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
import submission.messaging.exceptions.MessageCapacityExceededException

class SubmissionServiceUnitSpec extends Specification with Mockito{

  "Ingress Service" should {

    "publish message successfully" in {
      val service = new SuccessSubmissionServiceMocking{}

      val requestXml = <testXmlNode></testXmlNode>

      val response = Future(service.xmlProcessing(FakeRequest().withXmlBody(requestXml)))

      status(response) mustEqual OK

    }

    "fail publish with wrong message type" in {
      val service = new SuccessSubmissionServiceMocking{}

      val response = Future(service.xmlProcessing(FakeRequest().withTextBody("invalid")))

      status(response) mustEqual BAD_REQUEST

    }

    "fail publishing a message" in {

      val service = new FailSubmissionServiceMocking {
        def newException(msg: String): Exception = new Exception(msg)
      }

      val requestXml = <testXmlNode></testXmlNode>

      val response = Future(service.xmlProcessing(FakeRequest().withXmlBody(requestXml)))

      status(response) mustEqual SERVICE_UNAVAILABLE
    }

    "fail publish because of message exceeded error" in {

      val service = new FailSubmissionServiceMocking {
        def newException(msg: String): Exception = new MessageCapacityExceededException(msg)
      }

      val requestXml = <testXmlNode></testXmlNode>

      val response = Future(service.xmlProcessing(FakeRequest().withXmlBody(requestXml)))

      status(response) mustEqual SERVICE_UNAVAILABLE

    }

  }
  section("unit")
}