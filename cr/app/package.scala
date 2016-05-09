import java.net.InetAddress

import app.ConfigProperties._
import monitoring.ClaimReceivedMonitorRegistration
import org.slf4j.MDC
import play.api._
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}



package object app {


  object ConfigProperties {
    def getAppName = getProperty("application.name", "String", true).toString

    def getIntProperty(property: String, throwError: Boolean = true): Int = getProperty(property, "Int", throwError).toInt

    def getStringProperty(property: String, throwError: Boolean = true): String = getProperty(property, "String", throwError).toString

    def getBooleanProperty(property: String, throwError: Boolean = true): Boolean = getProperty(property, "Boolean", throwError).toBoolean

    private def getProperty(property: String, propertyType: String, throwError: Boolean): String = {
      if (!throwError && Play.unsafeApplication == null) {
        defaultValue(propertyType)
      }
      else {
        (Play.current.configuration.getString(property), throwError) match {
          case (Some(s), _) => s.toString
          case (_, false) => defaultValue(propertyType)
          case (_, _) => {
            Logger.error("ERROR - getProperty failed to retrieve application property for:" + property)
            throw new IllegalArgumentException(s"ERROR - getProperty failed to retrieve application property for:$property")
          }
        }
      }
    }

    private def defaultValue(propertyType: String) = {
      propertyType match {
        case "String" => ""
        case "Int" => "-1"
        case "Boolean" => "false"
      }
    }
  }

  trait GlobalImpl extends GlobalSettings  with ClaimReceivedMonitorRegistration {
    override def onStart(app: Application) {
      MDC.put("httpPort", getStringProperty("http.port", throwError = false))
      MDC.put("hostName", Option(InetAddress.getLocalHost.getHostName).getOrElse("Value not set"))
      MDC.put("envName", getStringProperty("env.name", throwError = false))
      MDC.put("appName", getAppName)
      Logger.info(s"$getAppName is now starting")
      super.onStart(app)

      registerReporters()
      registerHealthChecks()

      Logger.info(s"$getAppName Started") // used for operations, do not remove
    }

    override def onHandlerNotFound(request: RequestHeader): Future[Result] = Future(NotFound(<NoRestEndpointFound/>))

    override def onStop(app: Application) {
      super.onStop(app)
      val appName=getStringProperty("application.name", throwError = false)
      Logger.info(s"$getAppName Stopped") // used for operations, do not remove
    }
  }

}
