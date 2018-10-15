package services

import java.util.UUID
import FilesystemEventActor._

case class FilesystemEventsState(events:Map[UUID,FSEvent]=Map()) {
  def updated(evt:FSEvent) = copy(events ++ Map(UUID.randomUUID()->evt))
  def removed(id:UUID) = copy(events.filter(_._1 != id))
  def size:Int = events.size

  def map[A](block: (UUID, FSEvent)=>A):Iterable[A] = events.map(tuple=>block(tuple._1,tuple._2))
  def filter(block: (UUID, FSEvent)=>Boolean):Map[UUID,FSEvent] = events.filter(tuple=>block(tuple._1,tuple._2))
}
