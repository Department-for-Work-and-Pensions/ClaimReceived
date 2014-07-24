import monitoring.ClaimReceivedMonitorRegistration
import play.api._
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, SimpleResult}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Success, Try}



package object app {


  object ConfigProperties {
    val RABBIT_MAX_MESSAGES = "rabbit.messages.max"

    val WAITING_TIME_AFTER_RECOVERY = "waiting.after.recovery"


    def whenProperty[T](property:String) (t:PartialFunction[Boolean,T]) (f:PartialFunction[Boolean,T]) = {
        val v: Boolean = Try(Play.current.configuration.getBoolean(property).getOrElse(true)) match{ case Success(s) => s case _ => true}
        t.applyOrElse(v, f)
    }

    def getProperty(property:String,default:Int) = Try(Play.current.configuration.getInt(property).getOrElse(default)) match { case Success(s) => s case _ => default}
    def getProperty(property:String,default:String) = Try(Play.current.configuration.getString(property).getOrElse(default)) match { case Success(s) => s case _ => default}
    def getProperty(property:String,default:Boolean) = Try(Play.current.configuration.getBoolean(property).getOrElse(default)) match { case Success(s) => s case _ => default}

  }

  trait GlobalImpl extends GlobalSettings  with ClaimReceivedMonitorRegistration {
    override def onStart(app: Application) {
      super.onStart(app)

      registerReporters()
      registerHealthChecks()

      Logger.info("ClaimReceived Started") // used for operations, do not remove
    }

    override def onHandlerNotFound(request: RequestHeader): Future[SimpleResult] = Future(NotFound(<NoRestEndpointFound/>))

    override def onStop(app: Application) {
      super.onStop(app)
      Logger.info("ClaimReceived Stopped") // used for operations, do not remove
    }
  }

}
