package models

import java.io.File
import java.nio.file.attribute.BasicFileAttributes
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.nio.file.{Files, Path}

import scala.concurrent.{ExecutionContext, Future}

case class FileInfo (fullPath:String, length:Long, ctime:ZonedDateTime, mtime:ZonedDateTime, atime:ZonedDateTime) {
  /**
    * checks for differences to another FileInfo instance.  This involves reading from disk so is implemented as a Future.
    * @param f - other FileInfo instance to check
    * @return a Future containing True if both FileInfos match or False if they don't.
    */
  def checkFile(f:FileInfo)(implicit ec:ExecutionContext):Future[Boolean] = Future {
    val jFile = new File(f.fullPath)
    val attributes = Files.readAttributes(jFile.toPath, classOf[BasicFileAttributes])

    if(!jFile.exists()){
      false
    } else if(f.atime!=this.atime){
      false
    } else if(f.length!=this.length){
      false
    } else if(f.ctime!=this.ctime){
      false
    } else if(f.mtime!=this.mtime){
      false
    } else {
      true
    }
  }
}

object FileInfo extends ((String,Long,ZonedDateTime,ZonedDateTime,ZonedDateTime)=>FileInfo) {
  def fromFile(f:File)(implicit timeZoneId:ZoneId):FileInfo = {
    val attrs = Files.readAttributes(f.toPath, classOf[BasicFileAttributes])
    new FileInfo(f.getAbsolutePath,
      f.length(),
      ZonedDateTime.ofInstant(attrs.creationTime().toInstant,timeZoneId),
      ZonedDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), timeZoneId),
      ZonedDateTime.ofInstant(attrs.lastAccessTime().toInstant, timeZoneId)
    )
  }
}