package services

import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._

object EventTypesJson {
  implicit val eventTypesDecoder = Decoder.enumDecoder(EventTypes)
  implicit val eventTypesEnvoder = Encoder.enumEncoder(EventTypes)
}

object EventTypes extends Enumeration {
  type EventTypes = Value
  val CREATED,OPENED,UPDATED,DELETED = Value
}
