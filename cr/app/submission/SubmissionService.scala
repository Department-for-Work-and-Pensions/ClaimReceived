package submission

import play.api.libs.json.JsValue
import play.api.mvc.{RawBuffer, Results, AnyContent, Request}
import play.api.Logger
import submission.messaging.{MessageSender, Failure, Success, MessageSendingService}
import submission.messaging.exceptions.MessageCapacityExceededException

trait SubmissionService {

  def messagingService: MessageSendingService

  /**
   * Consumes the http requests and checks the body is XML, then
   * @param request http request received. Should be XML.
   * @return  XML response: Status Code and timestamp.
   */
  def xmlProcessing(request: Request[AnyContent]) = {

    request.body.asXml.map { xml =>
      val msgService: MessageSendingService = messagingService

      msgService.sendMessage(xml.toString(),msgService.getQueueName) match {
        case Success =>
          Results.Ok("")

        case Failure(exception:MessageCapacityExceededException) =>
          Logger.error("Message capacity exceeded: "+exception.getMessage,exception)
          Results.ServiceUnavailable

        case Failure(exception) =>
          Logger.error("Could not publish. Exception thrown: "+exception.getMessage,exception)
          Results.ServiceUnavailable

        case _ =>
          Logger.error("Unexpected result")
          Results.InternalServerError

      }

    } match {
      case Some(x) => x
      case _ =>
        Logger.error(s"Received a non XML request. Rejected. Body: ${request.body.toString}")
        Results.BadRequest
    }
  }


}

trait SubmissionServiceImpl extends SubmissionService {

  override def messagingService: MessageSendingService = {
    MessageSender
  }

}