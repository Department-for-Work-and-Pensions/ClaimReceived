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
    val RABBIT_MAX_MESSAGES = "rabbit.messages.max"

    val WAITING_TIME_AFTER_RECOVERY = "waiting.after.recovery"

    def getAppName = getProperty("application.name", "Value not set")
    def getProperty(property:String,default:Int) = Try(Play.current.configuration.getInt(property).getOrElse(default)) match { case Success(s) => s case _ => default}
    def getProperty(property:String,default:String) = Try(Play.current.configuration.getString(property).getOrElse(default)) match { case Success(s) => s case _ => default}
    def getProperty(property:String,default:Boolean) = Try(Play.current.configuration.getBoolean(property).getOrElse(default)) match { case Success(s) => s case _ => default}

  }

  trait GlobalImpl extends GlobalSettings  with ClaimReceivedMonitorRegistration {
    override def onStart(app: Application) {
      MDC.put("httpPort", getProperty("http.port", "Value not set"))
      MDC.put("hostName", Option(InetAddress.getLocalHost.getHostName).getOrElse("Value not set"))
      MDC.put("envName", getProperty("env.name", "Value not set"))
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
      Logger.info(s"$getAppName Stopped") // used for operations, do not remove
    }
  }

}
