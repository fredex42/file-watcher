package services

import java.io.File
import java.time.ZoneId
import java.util.UUID

import akka.actor.{Actor, ActorRef}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import javax.inject.{Inject, Named}
import models.FileInfo
import play.api.{Configuration, Logger}
import services.FilesystemEventActor.FSEvent

import collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object PollingActor {
  case class Poll(cfg:ConfigurationAction)
}

/**
  * This actor responds to a message Poll, which passes a single [[ConfigurationAction]] as its parameter.
  * In response to this, it will check the file metadata for the given directory and if there are any changes to the previous state
  * it will send [[FSEvent]] messages to [[FilesystemEventActor]] describing them.
  * Finally it will snapshot the current state and store it as the new "previous" state.
  * @param fsEventActor named injection of the [[FilesystemEventActor]]
  * @param config injected parameter of the app configuration
  */
class PollingActor @Inject() (@Named("filesystem-event-actor") fsEventActor:ActorRef, config:Configuration) extends PersistentActor {
  override val persistenceId = "polling-actor"
  private val logger = Logger(getClass)

  import PollingActor._
  implicit val ec:ExecutionContext = context.dispatcher

  implicit val timeZoneId:ZoneId = ZoneId.of(config.getOptional[String]("timezone").getOrElse("Europe/London"))
  var previousStates:Map[String, Map[String,FileInfo]] = Map()

  /**
    * returns the current state of a given directory
    * @param path directory to check
    * @return a Future, containing a Map of (filesystem path->FileInfo) objects
    */
  def stateForDirectory(path: String):Future[Map[String, FileInfo]] = Future {
    val dir = new File(path)
    //if(!dir.exists()) throw new RuntimeException(s"Directory $path does not exist!")
    dir.listFiles() match {
      case null=>throw new RuntimeException(s"No files exist at $dir")
      case list:Array[File]=>list.map(f=>(f.getAbsolutePath,FileInfo.fromFile(f))).toMap
    }
  }

  /**
    * computes the differences between a previous and current state for a given path, expressed as a map of (filesystem path->FileInfo)
    * objects.  The differences are expressed as [[FSEvent]] objects.
    * @param oldState previous state for the given path
    * @param newState new state for the given path
    * @return a Future, containing a Map of (filesystem path->FSEvent) for any path that has changed (been created, deleted or updated)
    */
  def stateDifferences(oldState:Map[String, FileInfo], newState:Map[String, FileInfo]):Future[Map[String, FSEvent]] = {
    val comps = oldState.map(entry=>{
      if(!newState.contains(entry._1)){
        Future(Some(entry._1,FSEvent(UUID.randomUUID(), entry._1, EventTypes.DELETED)))
      } else {
        val oldFile = entry._2
        oldFile.checkFile(newState(entry._1)).map({
          case true=>None
          case false=>Some(entry._1,FSEvent(UUID.randomUUID(), entry._1, EventTypes.UPDATED))
        })
      }
    }) ++ newState.map(entry=>Future {
      oldState.contains(entry._1) match {
        case true => None
        case false => Some(entry._1, FSEvent(UUID.randomUUID(), entry._1, EventTypes.CREATED))
      }
    })

    Future.sequence(comps).map(_.filter(_.isDefined).map(_.get).toMap)
  }

  override def receiveRecover: Receive = {
    case SnapshotOffer(_, snapshot:Map[String, Map[String,FileInfo]])=>
      logger.info("Startup - pollingActor got snapshot offer")
      previousStates = snapshot
    case RecoveryCompleted=>
      logger.info("Startup - PollingActor is ready")
  }

  override def receiveCommand: Receive = {
    case Poll(cfg)=>
      logger.debug(s"Starting poll run for $cfg")
      val futureState = stateForDirectory(cfg.path)
      futureState.recover({
        case ex:Throwable=>
          logger.error(s"Could not poll $cfg", ex)
      })
      futureState.map {currentState=>
        logger.debug(s"State for $cfg: $currentState")
        previousStates.get(cfg.path) match {
          case Some(previousState)=>
            stateDifferences(previousState, currentState).map(diffs=>{
              logger.debug(s"${cfg.path}: Changes are $diffs")
              diffs.foreach(evt=>fsEventActor ! evt._2)
            })
          case None=>
            logger.debug(s"${cfg.path}: Initial state, can't check changes")
        }
        logger.debug("Processed any changes, storing current state as old")
        val updatedState = previousStates ++ Map(cfg.path->currentState)
        saveSnapshot(updatedState)
        previousStates = updatedState
      }
  }
}
