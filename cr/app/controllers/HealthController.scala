package controllers

import play.api.mvc.{Controller, Action}
import monitoring.ProdHealthMonitor
import play.api.libs.json._
import com.codahale.metrics.health.HealthCheck

trait HealthController {
  this: Controller =>

  val healthMonitor = ProdHealthMonitor

  implicit val healthWrites = new Writes[(String, HealthCheck.Result)] {
    def writes(healthCheck: (String, HealthCheck.Result)) = Json.obj(
      "name" -> healthCheck._1,
      "isHealthy" -> healthCheck._2.isHealthy
    )
  }

  def healthReport = Action {
    request =>
      Ok(Json.prettyPrint(Json.toJson(healthMonitor.runHealthChecks()))).as("application/json").withHeaders("Cache-Control" -> "must-revalidate,no-cache,no-store")
  }
}

object HealthController extends Controller with HealthController
