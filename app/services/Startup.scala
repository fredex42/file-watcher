package services

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import javax.inject.{Inject, Named}
import java.time.Duration
import java.time.temporal.TemporalUnit
import java.time.temporal.ChronoUnit

import play.api.Logger

class Startup @Inject() (@Named("polling-actor") pollingActor: ActorRef, watcherConfigs:WatcherConfigs, system:ActorSystem){
  private val logger=Logger(getClass)
  import PollingActor._

  watcherConfigs.forEach { cfg=>
    logger.info(s"Starting up polling for $cfg")
    system.scheduler.schedule(
      Duration.of(cfg.initialDelaySeconds, ChronoUnit.SECONDS),
      Duration.of(cfg.pollingDelaySeconds, ChronoUnit.SECONDS),
      pollingActor,
      Poll(cfg),
      system.dispatcher,
      system.deadLetters
    )
  }

}
