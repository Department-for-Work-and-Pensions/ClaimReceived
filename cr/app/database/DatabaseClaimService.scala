package database

import play.api.Logger
import play.api.Play.current
import play.api.db.DB

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
}

object DatabaseClaim extends DatabaseClaimService

