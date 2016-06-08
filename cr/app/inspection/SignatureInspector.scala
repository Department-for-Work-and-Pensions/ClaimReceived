package inspection

import java.sql.SQLException
import database.{DatabaseClaim, DatabaseClaimService}
import gov.dwp.carers.xml.signing.XmlSignatureValidator
import play.api.Logger
import utils.TransactionIdExtractor
import scala.util.{Failure, Try, Success}
import scala.xml.{XML, NodeSeq}

/**
 * Check that the XML received as a valid digital signature.
 */
trait SignatureInspector {
  def databaseClaimService: DatabaseClaimService

  def checkSignature(msg: NodeSeq, transactionId: String): Boolean = {
    val currentSig = msg \\ "Signature"
    //load duplicate message
    val originalSig = XML.loadString(databaseClaimService.loadOriginalMessage(transactionId)) \\ "Signature"
    (currentSig == originalSig)
  }

  def checkSignatureAndSaveDuplicateMessage(msg: NodeSeq, transactionId: Option[String]) = {
    val rtn = checkSignature(msg, transactionId.get) match {
      case true => Logger.info(s"Signature check same for duplicate message [transactionId=$transactionId]"); databaseClaimService.storeDuplicateMessage(msg.toString, transactionId, 0); true
      case false => Logger.info(s"Signature check failed for duplicate message [transactionId=$transactionId]"); databaseClaimService.storeDuplicateMessage(msg.toString, transactionId, 1); false
    }
    rtn
  }

  def inspectAndSave(msg: NodeSeq): Boolean = {
    try {
      val msgString = msg.toString()
      val transactionId = TransactionIdExtractor.extractTransactionIdFrom(msg)
      val rtn = (XmlSignatureValidator.validate(msgString)) match {
        case true => storeMessage(msg, transactionId);
        case false => Logger.error(s"Signature validator failed for transactionId [${transactionId.get}]"); false
      }
      rtn
    } catch {
      case e: Exception =>
        Logger.error(s"Signature validator threw exception for transactionId [${TransactionIdExtractor.extractTransactionIdFrom(msg).get}]", e)
        false
    }
  }

  def storeMessage(msg: NodeSeq, transactionId: Option[String]): Boolean = {
    val rtn = Try(databaseClaimService.storeMessage(msg.toString(), transactionId)) match {
      case Success(t) => Logger.debug(s"Successfully saved transactionId=${transactionId.get} in inspectionstatus table"); true
      case Failure(e: SQLException) if (e.getSQLState == "23505" || e.getErrorCode == 23505) => checkSignatureAndSaveDuplicateMessage(msg, transactionId)
      case Failure(e) => Logger.error("Unable to save message in db", e); false
    }
    rtn
  }
}

trait SignatureInspectorImpl extends SignatureInspector {
  override def databaseClaimService: DatabaseClaimService = {
    DatabaseClaim
  }
}