package ingress.submission

import play.api.libs.json.{JsBoolean, JsString, JsArray}

import scala.concurrent.duration._
import org.specs2.mutable.Specification
import play.api.libs.ws.WS
import play.api.http.Status._

import scala.concurrent.{duration, Await}

/**
 * Created by valtechuk on 14/04/2015.
 */
class SubmissionServiceHealthCheckIntegrationSpec extends Specification{


  "Health check" should {
    "Show queue health info for successful queue" in new WithServerConfig("queue.name" -> "SubmissionServiceHealthCheckIntegration1","env.name"->"Test"){

      val queueName = TestUtils.declareQueue

      val url = s"http://localhost:$port/report/health"
      val response = Await.result(WS.url(url).get(),DurationInt(3).seconds)

      val responseObj = response.json.as[JsArray].value(0)

      val healthCheckName = "cr-queue-health"

      (responseObj \ "name").as[JsString].value mustEqual healthCheckName
      (responseObj \ "isHealthy").as[JsBoolean].value mustEqual true

      TestUtils.deleteQueue(queueName)


    }

    "Show queue health info for overflowed queue" in new WithServerConfig("queue.name" -> "SubmissionServiceHealthCheckIntegration2","env.name"->"Test","rabbit.messages.max"->20){


      val queueName = TestUtils.declareQueue

      TestUtils.publishMessages(queueName,(1 to 22).map(n => s"Message$n"):_*)

      val url = s"http://localhost:$port/report/health"
      val response = Await.result(WS.url(url).get(),DurationInt(3).seconds)

      val responseObj = response.json.as[JsArray].value(0)

      val healthCheckName = "cr-queue-health"

      (responseObj \ "name").as[JsString].value mustEqual healthCheckName
      (responseObj \ "isHealthy").as[JsBoolean].value mustEqual false

      TestUtils.deleteQueue(queueName)

    }

    "Ping the ping check" in new WithServerConfig("queue.name" -> "SubmissionServiceHealthCheckIntegration3","env.name"->"Test"){

      val url = s"http://localhost:$port/ping"
      val response = Await.result(WS.url(url).get(),DurationInt(3).seconds)

      response.status mustEqual OK
    }

    "Make sure we have metrics" in new WithServerConfig("queue.name" -> "SubmissionServiceHealthCheckIntegration4","env.name"->"Test"){
      val url = s"http://localhost:$port/report/metrics"
      val response = Await.result(WS.url(url).get(),DurationInt(3).seconds)

      response.status mustEqual OK
      response.body.size must beGreaterThan(0)
    }
  }
  section("integration")
}
