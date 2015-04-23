package monitoring

import com.kenshoo.play.metrics.MetricsRegistry
import app.ConfigProperties._
object Histograms {
  def recordQueueMessageCount(size: Int) {
    MetricsRegistry.defaultRegistry.histogram(getProperty("queue.name","ingress")+"-receive-message-count").update(size)
  }
}

object Counters {
  def recordClaimReceivedCount() {
    MetricsRegistry.defaultRegistry.counter(getProperty("app.name","cr-claim-received")+"-count").inc()
  }
}
