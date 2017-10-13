name := """elastic-scala-http"""

version := "0.0.3"

organization := "com.adelegue"

scalaVersion := "2.12.3"

crossScalaVersions := Seq("2.11.8", scalaVersion.value)

val akkaVersion = "2.5.6"

val akkaHttpVersion = "10.0.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"     % akkaVersion,
  "com.typesafe.akka" %% "akka-stream"    % akkaVersion,
  "com.typesafe.akka" %% "akka-http"      % akkaHttpVersion,
  "com.typesafe.play" %% "play-json"      % "2.6.6",
  "com.typesafe.akka" %% "akka-testkit"   % akkaVersion       % "test",
  "org.scalatest"     %% "scalatest"      % "3.0.1"           % "test",
  "org.elasticsearch" % "elasticsearch" % "5.5.0" % "test",
  "org.elasticsearch.plugin" % "transport-netty4-client" % "5.5.0" % "test",
  "org.elasticsearch.plugin" % "reindex-client" % "5.5.0" % "test",
  "org.slf4j" % "slf4j-api" % "1.7.25" % "test",
  "org.apache.logging.log4j" % "log4j-api" % "2.8.2" % "test",
  "org.apache.logging.log4j" % "log4j-core" % "2.8.2" % "test"
)

parallelExecution in Test := false

scalacOptions in Test ++= Seq("-Yrangepos")

publishTo := {
  val localPublishRepo = "./repository"
  if (isSnapshot.value) {
    Some(Resolver.file("snapshots", new File(localPublishRepo + "/snapshots")))
  } else {
    Some(Resolver.file("releases", new File(localPublishRepo + "/releases")))
  }
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <developers>
      <developer>
        <id>alexandre.delegue</id>
        <name>Alexandre Delègue</name>
        <url>https://github.com/larousso</url>
      </developer>
    </developers>
  )