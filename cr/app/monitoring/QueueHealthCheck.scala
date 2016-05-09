package monitoring

import app.ConfigProperties._
import gov.dwp.carers.CADSHealthCheck
import gov.dwp.carers.CADSHealthCheck.Result
import submission.messaging.{Failure, Success, MessageSender}
import submission.SubmissionServiceImpl

class QueueHealthCheck extends CADSHealthCheck(s"${getStringProperty("application.name", throwError = false)}", getStringProperty("application.version", throwError = false).takeWhile(_ != '-')) with SubmissionServiceImpl {
  override def check(): Result = {
    MessageSender.queueStatus() match {
      case Success =>
        Result.healthy
      case Failure(e) =>
        Result.unhealthy(e)
    }
  }
}


