package inspection

import database.DatabaseClaimService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import utils.WithApplication

import scala.xml.XML

class SignatureInspectorSpec extends Specification with Mockito {

  section("unit","inspectors")
  "Signature Inspector" should {
    "Fail if xml hasn't a valid signature" in new WithApplication {
      val mockedDatabaseClaimService = mock[DatabaseClaimService]
      mockedDatabaseClaimService.storeMessage(anyString, Some(anyString)) returns true
      val inspector = new SignatureInspector {
        override val databaseClaimService = mockedDatabaseClaimService
      }

      val xml = XML.load (getClass getResourceAsStream "/ValidXMLWithInvalidRSASignature.xml")
      inspector.inspectAndSave(xml) match {
        case b =>
          b must beFalse
      }
    }

    "Passes successfully if xml has a DSA valid signature" in  new WithApplication {
      val mockedDatabaseClaimService = mock[DatabaseClaimService]
      mockedDatabaseClaimService.storeMessage(anyString, Some(anyString)) returns true
      val inspector = new SignatureInspector {
        override val databaseClaimService = mockedDatabaseClaimService
      }

      val xml = XML.load (getClass getResourceAsStream "/Sign2.xml")
      inspector.inspectAndSave(xml) match {
        case b =>
          b must beTrue
      }
    }

    "Passes successfully if xml has a RSA valid signature" in  new WithApplication {
      val mockedDatabaseClaimService = mock[DatabaseClaimService]
      mockedDatabaseClaimService.storeMessage(anyString, Some(anyString)) returns true
      val inspector = new SignatureInspector {
        override val databaseClaimService = mockedDatabaseClaimService
      }

      val xml = XML.load (getClass getResourceAsStream "/ValidXMLWithRSASignature.xml")
      inspector.inspectAndSave(xml) match {
        case b =>
          b must beTrue
      }
    }
  }
  section("unit","inspectors")
}
