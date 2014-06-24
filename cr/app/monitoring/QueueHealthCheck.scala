package monitoring

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheck.Result
import submission.messaging.{Failure, Success, MessageSender}
import submission.SubmissionServiceImpl

class QueueHealthCheck extends HealthCheck with SubmissionServiceImpl {
  override def check(): Result = {
    MessageSender.queueStatus() match {
      case Success =>
        Result.healthy
      case Failure(e) =>
        Result.unhealthy(e)
    }
  }
}


