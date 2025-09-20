package gibbon.core

final case class Event[K,V](
    key: K,
    value: V,
    eventTime: Long, //from producer
    timestamp: Long = System.currentTimeMillis()
)
