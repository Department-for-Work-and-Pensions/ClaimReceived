package inspection

import anorm.SqlParser._
import anorm._
import database.{DatabaseClaim, WithInMemoryDBApplication, DatabaseClaimService}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Play
import play.api.db.DB
import play.api.test.FakeApplication
import play.api.test.Helpers._
import utils.WithApplication
import scala.xml.XML

class SignatureInspectorSpec extends Specification with Mockito {
  sequential

  lazy val appLatest = FakeApplication(additionalConfiguration = inMemoryDatabase("ingress", options=Map("MODE" -> "PostgreSQL","DB_CLOSE_DELAY"->"-1")))

  section("unit","inspectors")
  "Signature Inspector" should {
    "Fail if xml hasn't a valid signature" in new WithApplication {
      val mockedDatabaseClaimService = mock[DatabaseClaimService]
      mockedDatabaseClaimService.storeMessage(anyString, Some(anyString)) returns true
      val inspector = new SignatureInspector {
        override val databaseClaimService = mockedDatabaseClaimService
      }

      val xml = XML.load(getClass getResourceAsStream "/ValidXMLWithInvalidRSASignature.xml")
      inspector.inspectAndSave(xml) match {
        case b =>
          b must beFalse
      }
    }

    "Passes successfully if xml has a DSA valid signature" in new WithApplication {
      val mockedDatabaseClaimService = mock[DatabaseClaimService]
      mockedDatabaseClaimService.storeMessage(anyString, Some(anyString)) returns true
      val inspector = new SignatureInspector {
        override val databaseClaimService = mockedDatabaseClaimService
      }

      val xml = XML.load(getClass getResourceAsStream "/Sign2.xml")
      inspector.inspectAndSave(xml) match {
        case b =>
          b must beTrue
      }
    }

    "Passes successfully if xml has a RSA valid signature" in new WithApplication {
      val mockedDatabaseClaimService = mock[DatabaseClaimService]
      mockedDatabaseClaimService.storeMessage(anyString, Some(anyString)) returns true
      val inspector = new SignatureInspector {
        override val databaseClaimService = mockedDatabaseClaimService
      }

      val xml = XML.load(getClass getResourceAsStream "/ValidXMLWithRSASignature.xml")
      inspector.inspectAndSave(xml) match {
        case b =>
          b must beTrue
      }
    }

    step(Play.start(appLatest))

    "Test duplicate message stores with signature_check=0" in new WithInMemoryDBApplication(app = appLatest) {
      val xml = XML.load(getClass getResourceAsStream "/ValidXMLWithRSASignature.xml")
      DatabaseClaim.storeMessage(xml.mkString, Some("16021000011")) must beTrue
      val inspector = new SignatureInspector {
        override val databaseClaimService = DatabaseClaim
      }
      inspector.checkSignatureAndSaveDuplicateMessage(xml, Some("16021000011")) must beTrue
      getSignatureCheckFromDuplicateMessages("16021000011")(appLatest) must beEqualTo(0)
    }

    "Test duplicate message stores with signature_check=1" in new WithInMemoryDBApplication(app = appLatest) {
      val xml = XML.load(getClass getResourceAsStream "/ValidXMLWithRSASignature.xml")
      DatabaseClaim.storeMessage(xml.mkString, Some("16021000011")) must beTrue
      val inspector = new SignatureInspector {
        override val databaseClaimService = DatabaseClaim
      }
      val xmlDuplicate = XML.load(getClass getResourceAsStream "/ValidXMLWithInvalidRSASignature.xml")
      inspector.checkSignatureAndSaveDuplicateMessage(xmlDuplicate, Some("16021000011")) must beFalse
      getSignatureCheckFromDuplicateMessages("16021000011")(appLatest) must beEqualTo(1)
    }

    "Test duplicate message stores with signature_check=0 using inspect and save" in new WithInMemoryDBApplication(app = appLatest) {
      val xml = XML.load(getClass getResourceAsStream "/ValidXMLWithRSASignature.xml")
      DatabaseClaim.storeMessage(xml.mkString, Some("16021000011")) must beTrue
      val inspector = new SignatureInspector {
        override val databaseClaimService = DatabaseClaim
      }
      val xmlDuplicate = XML.load(getClass getResourceAsStream "/ValidXMLWithRSASignature.xml")
      inspector.inspectAndSave(xmlDuplicate) must beTrue
      getSignatureCheckFromDuplicateMessages("16021000011")(appLatest) must beEqualTo(0)
    }
    step(Play.stop(appLatest))
  }
  section("unit","inspectors")

  private def getSignatureCheckFromDuplicateMessages(transactionId: String)(implicit app: FakeApplication): Int = {
    DB.withConnection("ingress") {
      implicit connection =>
        SQL(
          """
          SELECT signature_check
          FROM carers.duplicatemessages
          WHERE transaction_id = {transactionId}
          """
        ).on("transactionId"->transactionId).as(int("signature_check") single)
    }
  }
}
