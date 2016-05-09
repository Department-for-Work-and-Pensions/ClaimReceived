package monitoring

import com.codahale.metrics.SharedMetricRegistries
import app.ConfigProperties._
import play.api.Play._

object Histograms {
  def recordQueueMessageCount(size: Int) {
    SharedMetricRegistries.getOrCreate(current.configuration.getString("metrics.name").getOrElse("default")).histogram(getStringProperty("queue.name")+"-receive-message-count").update(size)
  }
}

object Counters {
  def recordClaimReceivedCount() {
    SharedMetricRegistries.getOrCreate(current.configuration.getString("metrics.name").getOrElse("default")).counter(getStringProperty("application.name")+"-count").inc()
  }
}
