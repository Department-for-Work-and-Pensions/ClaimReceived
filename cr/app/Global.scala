import app.GlobalImpl
import monitoring.MonitorFilter
import play.api.mvc.WithFilters

object Global extends WithFilters(MonitorFilter) with GlobalImpl



