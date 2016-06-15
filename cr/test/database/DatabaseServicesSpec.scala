package database

import java.sql.SQLException

import anorm._
import org.specs2.execute.{AsResult, Result}
import org.specs2.mutable.{Around, Specification}
import play.api.Play
import play.api.Play.current
import play.api.db.DB
import play.api.test.Helpers._
import play.api.test.{FakeApplication, Helpers}

import scala.util.{Try, Failure, Success}

class DatabaseServicesSpec extends Specification {
  sequential

  lazy val appLatest = FakeApplication(additionalConfiguration = inMemoryDatabase("ingress", options=Map("MODE" -> "PostgreSQL","DB_CLOSE_DELAY"->"-1")))

  step(Play.start(appLatest))
  section ("database")
  "Ingress database Service" should {
    sequential

    "Store message with no transaction_id" in new WithInMemoryDBApplication(app = appLatest) {
      DatabaseClaim.storeMessage("<DWPCATransaction><TransactionId>NFM33DB</TransactionId><DateTimeGenerated>09-10-2010 10:38</DateTimeGenerated></DWPCATransaction>", None) must beTrue
    }

    "Store message with transaction_id" in new WithInMemoryDBApplication(app = appLatest) {
      DatabaseClaim.storeMessage("<DWPCATransaction><TransactionId>NFM33DB</TransactionId><DateTimeGenerated>09-10-2010 10:38</DateTimeGenerated></DWPCATransaction>", Some("NFM33DB")) must beTrue
    }

    "Store duplicate message with no transaction_id" in new WithInMemoryDBApplication(app = appLatest) {
      DatabaseClaim.storeDuplicateMessage("<DWPCATransaction><TransactionId>NFM33DB</TransactionId><DateTimeGenerated>09-10-2010 10:38</DateTimeGenerated></DWPCATransaction>", None, 0) must beTrue
    }

    "Store duplicate message with transaction_id" in new WithInMemoryDBApplication(app = appLatest) {
      DatabaseClaim.storeDuplicateMessage("<DWPCATransaction><TransactionId>NFM33DB</TransactionId><DateTimeGenerated>09-10-2010 10:38</DateTimeGenerated></DWPCATransaction>", Some("NFM33DB"), 0) must beTrue
    }

    "return false if a transaction has been processed previously" in new WithInMemoryDBApplication(app = appLatest) {
      DatabaseClaim.storeMessage("<DWPCATransaction><TransactionId>NFM33DB</TransactionId><DateTimeGenerated>09-10-2010 10:38</DateTimeGenerated></DWPCATransaction>", Some("NFM33DB")) must beTrue
      val rtn = Try(DatabaseClaim.storeMessage("<DWPCATransaction><TransactionId>NFM33DB</TransactionId><DateTimeGenerated>09-10-2010 10:38</DateTimeGenerated></DWPCATransaction>", Some("NFM33DB"))) match {
        case Success(t) => true;
        case Failure(e: SQLException) => false
        case _ => false
      }
      rtn must beFalse
    }

    step(Play.stop(appLatest))
  }
  section ("database")
}

/**
 * No used. Tried to use in memory H2 but failed. Seems to have concurrency issues.
 */
class WithInMemoryDBApplication(app: FakeApplication = FakeApplication(additionalConfiguration = inMemoryDatabase("ingress", options=Map("MODE" -> "PostgreSQL","DB_CLOSE_DELAY"->"-1")))) extends WithApplicationNoStart with Around {
  override def around[T](t: => T)(implicit evidence$1: AsResult[T]): Result = {
    DB.withConnection("ingress") { implicit c =>
      SQL(
        """
        DROP ALL OBJECTS; CREATE SCHEMA if not exists carers;
        CREATE TABLE carers.suspiciousmessages (id bigserial, createdon TIMESTAMP(6) DEFAULT now() NOT NULL, message text NOT NULL, transaction_id CHARACTER VARYING(11), CONSTRAINT pk_suspiciousmessages PRIMARY KEY(id));
        CREATE TABLE carers.inspectionstatus (createdon TIMESTAMP(6) DEFAULT now() NOT NULL, transaction_id CHARACTER VARYING(11) NOT NULL, status INTEGER NOT NULL, message TEXT, CONSTRAINT pk_inspectionstatus PRIMARY KEY(transaction_id));
        CREATE TABLE carers.messagevalidationerrors (id bigserial, createdon TIMESTAMP(6) DEFAULT now() NOT NULL, error_message text NOT NULL, transaction_id CHARACTER VARYING(11) NOT NULL, schema_version CHARACTER VARYING(6) NOT NULL, CONSTRAINT pk_messagevalidationerrors PRIMARY KEY(id));
        CREATE TABLE carers.duplicatemessages (id bigserial, message text, transaction_id CHARACTER VARYING(11), signature_check INTEGER, CONSTRAINT pk_duplicatemessages PRIMARY KEY(id));
        """.stripMargin
      ).execute()
    }
    super.around(t)
  }
}

class WithPostgreSQLDBApplication extends WithApplication(FakeApplication(additionalConfiguration = Map(
  "db.ingress.driver" -> "org.postgresql.Driver",
  "db.ingress.url" -> """jdbc:postgresql://localhost:5432/inspection_db""",
  "db.ingress.username" -> "carers_il3" ,
  "db.ingress.password" -> "klk34sNUlf0eD"))) {}

abstract class WithApplication(val app: FakeApplication = FakeApplication()) extends Around with org.specs2.matcher.MustThrownExpectations with org.specs2.matcher.ShouldThrownExpectations {
  implicit def implicitApp = app
  override def around[T: AsResult](t: => T): Result = {
    Helpers.running(app)(AsResult.effectively(t))
  }
}

abstract class WithApplicationNoStart(val app: FakeApplication = FakeApplication()) extends Around with org.specs2.matcher.MustThrownExpectations with org.specs2.matcher.ShouldThrownExpectations {
  implicit def implicitApp = app
  override def around[T: AsResult](t: => T): Result = {
    AsResult.effectively(t)
  }
}