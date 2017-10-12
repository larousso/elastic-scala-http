name := """elastic-scala-http"""

version := "0.0.2-SNAPSHOT"

organization := "com.adelegue"

scalaVersion := "2.12.3"

val akkaVersion = "2.5.6"

val akkaHttpVersion = "10.0.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"     % akkaVersion,
  "com.typesafe.akka" %% "akka-stream"    % akkaVersion,
  "com.typesafe.akka" %% "akka-http"      % akkaHttpVersion,
  "com.typesafe.play" %% "play-json"      % "2.6.6",
  "org.elasticsearch.client" % "rest" % "5.5.0",
  "com.typesafe.akka" %% "akka-testkit"   % akkaVersion       % "test",
  "org.scalatest"     %% "scalatest"      % "3.0.1"           % "test",
  "org.specs2"        %% "specs2-core"    % "3.9.5"           % "test",
  "org.specs2"        %% "specs2-matcher-extra"    % "3.9.5"           % "test",
  "org.elasticsearch" % "elasticsearch" % "5.5.0" % "test",
  "org.elasticsearch.plugin" % "transport-netty4-client" % "5.5.0" % "test",
  "org.elasticsearch.plugin" % "reindex-client" % "5.5.0" % "test",
  "org.slf4j" % "slf4j-api" % "1.7.25" % "test",
  "org.apache.logging.log4j" % "log4j-api" % "2.8.2" % "test",
  "org.apache.logging.log4j" % "log4j-core" % "2.8.2" % "test"
)

parallelExecution in Test := false

scalacOptions in Test ++= Seq("-Yrangepos")

publishTo := Some(Resolver.file("file",  new File("/Users/adelegue/idea/mvn-repo/releases" )))
