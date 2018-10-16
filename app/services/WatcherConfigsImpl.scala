package services

import java.io.{File, FileReader}

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Logger}
import io.circe.yaml.parser
import play.api.inject.ApplicationLifecycle
import io.circe.generic.auto._
import EventTypesJson._

@Singleton
class WatcherConfigsImpl @Inject() (configuration:Configuration, lifecycle:ApplicationLifecycle) extends WatcherConfigs {
  private val logger=Logger(getClass)
  val staticConfig:Seq[ConfigurationAction] = startupLoadConfigs(configuration.getOptional[String]("actionsConfigFile").getOrElse("/etc/file-watcher/actions.yaml"))

  def startupLoadConfigs(configPath: String):Seq[ConfigurationAction] = {
    logger.info(s"Loading file at $configPath")
    val f = new File(configPath)
    if(! f.exists()){
      logger.error(s"Could not load file $configPath")
      lifecycle.stop()
      throw new RuntimeException("Could not load actions config")
    } else if(f.length()==0){
      logger.error(s"Actions configuration file $configPath is zero-length, can't continue")
      lifecycle.stop()
      throw new RuntimeException(s"Actions configuration file $configPath is zero-length, can't continue")
    }

    parser.parse(new FileReader(configPath)) match {
      case Left(parsingFailure)=>
        logger.error(s"Could not read actions configuration: ${parsingFailure.toString}")
        lifecycle.stop()
        throw new RuntimeException("Could not load actions configuration, see log for details")
      case Right(content)=>
        println(content)
        println(content.as[Seq[ConfigurationAction]])
        content.as[Seq[ConfigurationAction]].getOrElse({
          logger.error("Actions config is YAML but does not properly describe actions")
          lifecycle.stop()
          throw new RuntimeException("Actions config is YAML but does not properly describe actions")
        })
    }
  }

  override def actionsForPath(fsPath: String): Seq[ConfigurationAction] = {
    staticConfig.filter(_.path==fsPath) //this will probably need to be enhanced a bit...
  }

  override def forEach(block: ConfigurationAction => Unit): Unit = {
    staticConfig.foreach(block)
  }
}
