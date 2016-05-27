package inspection

import database.{DatabaseClaim, DatabaseClaimService}
import gov.dwp.carers.xml.signing.XmlSignatureValidator
import play.api.Logger
import utils.TransactionIdExtractor
import scala.xml.NodeSeq

/**
 * Check that the XML received as a valid digital signature.
 */
trait SignatureInspector {
  def databaseClaimService: DatabaseClaimService

  def inspectAndSave(msg: NodeSeq): Boolean = {
      try {
        val databaseService: DatabaseClaimService = databaseClaimService
        val msgString = msg.toString()
        if (XmlSignatureValidator.validate(msgString)) {
          databaseService.storeMessage(msgString, TransactionIdExtractor.extractTransactionIdFrom(msg))
          true
        } else {
          Logger.error(s"Signature validator failed for transactionId [${TransactionIdExtractor.extractTransactionIdFrom(msg).get}]")
          false
        }
      }
      catch {
        case e:Exception =>
          Logger.error(s"Signature validator threw exception for transactionId [${TransactionIdExtractor.extractTransactionIdFrom(msg).get}]", e)
          false
      }
  }
}

trait SignatureInspectorImpl extends SignatureInspector {
  override def databaseClaimService: DatabaseClaimService = {
    DatabaseClaim
  }
}