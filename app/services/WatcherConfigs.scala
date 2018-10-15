package services

trait WatcherConfigs {
  def actionsForPath(fsPath:String):Seq[ConfigurationAction]
}
