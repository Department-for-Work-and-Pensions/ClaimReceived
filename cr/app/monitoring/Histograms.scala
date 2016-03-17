package monitoring

import com.codahale.metrics.SharedMetricRegistries
import app.ConfigProperties._
import play.api.Play._

object Histograms {
  def recordQueueMessageCount(size: Int) {
    SharedMetricRegistries.getOrCreate(current.configuration.getString("metrics.name").getOrElse("default")).histogram(getProperty("queue.name","ingress")+"-receive-message-count").update(size)
  }
}

object Counters {
  def recordClaimReceivedCount() {
    SharedMetricRegistries.getOrCreate(current.configuration.getString("metrics.name").getOrElse("default")).counter(getProperty("application.name","cr-claim-received")+"-count").inc()
  }
}
