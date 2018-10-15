name := "file-watcher"
 
version := "1.0"

enablePlugins(UniversalPlugin, LinuxPlugin, JavaServerAppPackaging, DockerPlugin)

lazy val `file-watcher` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

libraryDependencies ++= Seq( jdbc , ehcache , ws , specs2 % Test , guice )

unmanagedResourceDirectories in Test +=  {{ baseDirectory ( _ /"target/web/public/test" ) }.value}

//messaging persistence and clustering
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence" % "2.5.11",
  "com.typesafe.akka" %% "akka-cluster" % "2.5.11",
  "com.typesafe.akka" %% "akka-cluster-metrics" % "2.5.11",
  "com.typesafe.akka" %% "akka-cluster-tools" % "2.5.11",
  "org.iq80.leveldb"            % "leveldb"          % "0.7",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % Test
)

//Sentry
libraryDependencies += "io.sentry" % "sentry-logback" % "1.7.2"

val circeVersion = "0.8.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-yaml" % circeVersion,
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion
)