package services

trait WatcherConfigs {
  def actionsForPath(fsPath:String):Seq[ConfigurationAction]

  def forEach(block: ConfigurationAction=>Unit):Unit
}
