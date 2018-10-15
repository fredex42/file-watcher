package services

import java.util.UUID

import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import javax.inject.Inject
import play.api.Logger
import services.EventTypes.EventTypes

import scala.concurrent.Future
import scala.util.{Failure, Success}

object FilesystemEventActor {
  trait MessageEvent {
    val uuid:UUID
  }

  trait Message {

  }

  case class FSEvent (uuid:UUID, path: String, eventType: EventTypes) extends Message

  case class EventHandled(eventId:UUID) extends Message

}

class FilesystemEventActor @Inject() (watcherConfigs: WatcherConfigs) extends PersistentActor {
  private val logger = Logger(getClass)
  override def persistenceId = "filesystem-events-actor"
  private var state:FilesystemEventsState = FilesystemEventsState()

  implicit val ec = context.dispatcher

  import FilesystemEventActor._

  /**
    * Logs to the journal that this event has been handled, so it won't be re-tried
    * @param eventId: UUID of the handled event
    */
  def confirmHandled(eventId:UUID):Unit = {
    persist(EventHandled(eventId)){ handledEventMarker=>
      logger.debug(s"marked event $eventId as handled")
      state = state.removed(eventId)
    }
  }

  override def receiveRecover:Receive = {
    case SnapshotOffer(_, snapshot:FilesystemEventsState)=>
      logger.debug("FilesystemEventActor got snapshot offer")
      state = snapshot
    case RecoveryCompleted=>
      logger.info("Recovery completed for FilesystemEventActor")
  }

  override def receiveCommand:Receive = {
    case msgAsObject:FSEvent=>
      persist(msgAsObject) { event=>
        state = state.updated(event)

        val parallelEventsFutures = for {
          action <- watcherConfigs.actionsForPath(msgAsObject.path)
          result <- action.execute
        } yield result
        Future.sequence(parallelEventsFutures).onComplete({
          case Success(resultsSequence)=>
            logger.info(s"All events succeeded for $msgAsObject")
          case Failure(error)=>
            parallelEventsFutures.map(_.collect({
              case err:Throwable=>
                logger.warn(s"Event step failed for $msgAsObject", err)
            }))
        })

      }
  }
}
