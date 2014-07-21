package controllers

import play.api.mvc._
import submission.SubmissionServiceImpl
import monitoring.Counters

object Application extends Controller with SubmissionServiceImpl {
  
  def submission = Action { request =>
    Counters.recordClaimReceivedCount()
    xmlProcessing(request)
  }

}

