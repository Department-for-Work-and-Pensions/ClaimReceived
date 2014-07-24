package monitoring

import app.ConfigProperties._
import monitor.{HealthMonitor, MonitorRegistration}
import play.api.Logger

/**
 * Handles all monitoring and registration.
 */
trait ClaimReceivedMonitorRegistration extends MonitorRegistration {

  override def getFrequency: Int = getProperty("metrics.frequency", default = 1)

  override def isLogMetrics: Boolean = getProperty("metrics.slf4j", default = false)

  override def isLogHealth: Boolean = getProperty("health.logging", default = false)

  override   def getHealthMonitor : HealthMonitor = ProdHealthMonitor

  override def registerHealthChecks(): Unit = {
    Logger.info("QueueHealthCheck registered.")
    ProdHealthMonitor.register("cr-queue-health", new QueueHealthCheck)
  }


}
