package monitoring

import monitor.{HealthMonitor, MonitorRegistration}
import play.api.Logger
import app.ConfigProperties._

/**
 * Handles all monitoring and registration.
 */
trait ClaimReceivedMonitorRegistration extends MonitorRegistration {

  override def getFrequency: Int = getIntProperty("metrics.frequency")

  override def isLogMetrics: Boolean = getBooleanProperty("metrics.slf4j")

  override def isLogHealth: Boolean = getBooleanProperty("health.logging")

  override   def getHealthMonitor : HealthMonitor = ProdHealthMonitor

  override def registerHealthChecks(): Unit = {
    Logger.info("QueueHealthCheck registered.")
    ProdHealthMonitor.register(getAppName+"-queue-health", new QueueHealthCheck)
  }
}
