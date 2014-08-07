package controllers

import play.api.Logger
import play.api.mvc._
import submission.SubmissionServiceImpl
import monitoring.Counters

object Application extends Controller with SubmissionServiceImpl {
  
  def submission = Action { request =>
    Counters.recordClaimReceivedCount()
    Logger.info("Received new message.")
    xmlProcessing(request)
  }

}

