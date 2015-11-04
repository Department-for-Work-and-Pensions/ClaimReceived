package utils

import javax.inject.Inject

import monitor.MonitorFilter
import play.api.http.HttpFilters

class Filters @Inject() (monitorFilter: MonitorFilter) extends HttpFilters {
  val filters = Seq(monitorFilter)
}
