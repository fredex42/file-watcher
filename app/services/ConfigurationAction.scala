package services

import io.circe.generic.auto._
import actions.{GenericAction, GenericActionResult, KubeJobAction}
import services.EventTypes.EventTypes

import scala.concurrent.{ExecutionContext, Future}


case class ConfigurationAction (path:String, initialDelaySeconds: Int, pollingDelaySeconds: Int, eventTypes:Seq[EventTypes], actionsList:Seq[KubeJobAction], parallel:Boolean=true){
  def execute(evt:FilesystemEventActor.FSEvent)(implicit ec: ExecutionContext):Seq[Future[GenericActionResult]] = actionsList.map(_.execute(evt))
}

