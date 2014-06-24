package monitoring

import com.kenshoo.play.metrics.MetricsRegistry

object Histograms {
  def recordQueueMessageCount(size: Int) {
    MetricsRegistry.default.histogram("ingress-receive-message-count").update(size)
  }
}
