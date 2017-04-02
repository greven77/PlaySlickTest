package utils

import java.sql.Timestamp
import org.joda.time.DateTime
import org.joda.time.DateTimeZone.UTC
import slick.driver.MySQLDriver.api._

object CustomColumnTypes {
  implicit val jodaDateTimeType =
    MappedColumnType.base[DateTime, Timestamp](
      dt => new Timestamp(dt.getMillis),
      ts => new DateTime(ts.getTime, UTC)
    )
}
