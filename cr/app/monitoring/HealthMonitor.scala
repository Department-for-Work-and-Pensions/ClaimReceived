package monitoring

import com.codahale.metrics.health.{HealthCheckRegistry, HealthCheck}
import monitor.HealthMonitor
import collection.JavaConversions._
import scala.collection.immutable.SortedMap

object ProdHealthMonitor extends HealthMonitor