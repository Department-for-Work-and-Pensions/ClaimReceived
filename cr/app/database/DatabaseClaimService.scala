package database

import anorm.SqlParser._
import anorm._
import play.api.Logger
import play.api.Play.current
import play.api.db.DB
import scala.language.postfixOps

trait DatabaseClaimService {

  private val DATABASE = "ingress"

  def storeMessage(msg: String, transactionId: Option[String] = None): Boolean = DB.withConnection(DATABASE) {
    implicit connection =>
      val insertSql: String = "INSERT INTO carers.inspectionstatus(status, transaction_id, message) VALUES (?,?,?);"
      val statement = connection.prepareStatement(insertSql)
      statement.setInt(1, 0)
      statement.setString(2, transactionId.getOrElse("No trans Id"))
      statement.setString(3, msg)
      val stored = statement.executeUpdate() > 0
      statement.close()
      if (!stored) Logger.error(s"Could not store message with transactionId [${transactionId.getOrElse("no transaction id available - schema validation failure?")}].")
      stored
  }

  def storeDuplicateMessage(msg: String, transactionId: Option[String] = None, signatureCheck: Int): Boolean = DB.withConnection(DATABASE) {
    implicit connection =>
      val insertSql: String = "INSERT INTO carers.duplicatemessages (transaction_id, message, signature_check) VALUES (?,?,?);"
      val statement = connection.prepareStatement(insertSql)
      statement.setString(1, transactionId.getOrElse("No trans Id"))
      statement.setString(2, msg)
      statement.setInt(3, signatureCheck)
      val stored = statement.executeUpdate() > 0
      statement.close()
      if (!stored) Logger.error(s"Could not store duplicate message with transactionId [${transactionId.getOrElse("no transaction id available - duplicate message failure?")}].")
      stored
  }

  def loadOriginalMessage(transactionId: String): String = DB.withConnection(DATABASE){
    implicit connection =>
      SQL(
        """
        SELECT message
        FROM carers.inspectionstatus
        WHERE transaction_id = {transactionId}
        """
      ).on("transactionId"->transactionId).as(str("message") single)
  }
}

object DatabaseClaim extends DatabaseClaimService

