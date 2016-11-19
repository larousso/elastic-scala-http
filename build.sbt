name := """elastic-scala-http"""

version := "0.0.1"

organization := "com.adelegue"

scalaVersion := "2.11.6"

val akkaVersion = "2.4.12"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaVersion,
  "com.typesafe.play" %% "play-json" % "2.5.1" cross CrossVersion.binary,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test",
  "org.elasticsearch" % "elasticsearch" % "2.3.1" % "test",
  "net.java.dev.jna"  % "jna" % "4.1.0" % "test",
  "org.specs2"        %% "specs2"  % "2.4.15"  % "test"
)

publishTo := Some(Resolver.file("file",  new File("/Users/adelegue/idea/mvn-repo/releases" )))
