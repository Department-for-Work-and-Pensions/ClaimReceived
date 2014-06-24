package controllers

import play.api.mvc._
import submission.SubmissionServiceImpl


object Application extends Controller with SubmissionServiceImpl {
  
  def submission = Action { request =>
    xmlProcessing(request)
  }

}

