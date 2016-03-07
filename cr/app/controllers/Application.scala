package controllers

import play.api.Logger
import play.api.mvc._
import submission.SubmissionServiceImpl
import monitoring.Counters
import utils.RenameThread

object Application extends Controller with SubmissionServiceImpl {
  
  def submission = Action { request =>
    RenameThread.getTransactionIdAndRenameThread(request.body.asXml)
    Counters.recordClaimReceivedCount()
    Logger.info("Received new message.")
    xmlProcessing(request)
  }

}

