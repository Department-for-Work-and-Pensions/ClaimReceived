package controllers

import inspection.SignatureInspectorImpl
import play.api.Logger
import play.api.mvc._
import submission.SubmissionServiceImpl
import monitoring.Counters
import utils.RenameThread
import app.ConfigProperties._

object Application extends Controller with SubmissionServiceImpl with SignatureInspectorImpl {
  
  def submission = Action { request =>
    RenameThread.getTransactionIdAndRenameThread(request.body.asXml)
    Counters.recordClaimReceivedCount()
    Logger.info("Received new message.")
    (getBooleanProperty("check.signature", true)) match {
      case true if (!inspectAndSave(request.body.asXml.get)) => Results.BadRequest
      case _ => xmlProcessing(request)
    }
  }
}

