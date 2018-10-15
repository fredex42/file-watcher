package actions

import io.circe.generic.auto._
import services.FilesystemEventActor

import scala.concurrent.{ExecutionContext, Future}

case class KubeJobAction (name:String) extends GenericAction {
  override def execute(evt: FilesystemEventActor.FSEvent)(implicit ec: ExecutionContext): Future[GenericActionResult] = {
    throw new RuntimeException("Not yet implemented") //this causes Future.onComplete to go to the 'Failure' path
  }
}
