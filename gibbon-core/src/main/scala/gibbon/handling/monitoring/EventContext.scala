package gibbon.handling.monitoring

import java.time.Instant

final case class EventContext(
  timestamp: Instant,
  partition: Option[Int] = None,
  offset: Long
)
