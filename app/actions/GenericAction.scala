package actions

import services.FilesystemEventActor.FSEvent

import scala.concurrent.{ExecutionContext, Future}

trait GenericAction {
  val name:String
  def execute(evt:FSEvent)(implicit ec:ExecutionContext):Future[GenericActionResult]
}
