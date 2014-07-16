package monitoring

import com.kenshoo.play.metrics.MetricsRegistry

object Histograms {
  def recordQueueMessageCount(size: Int) {
    MetricsRegistry.default.histogram("ingress-receive-message-count").update(size)
  }
}

object Counters {
  def recordClaimReceivedCount() {
    MetricsRegistry.default.counter(s"cr-claim-received-count").inc()
  }
}
