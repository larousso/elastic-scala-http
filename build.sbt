name := """elastic-scala-http"""

version := "0.0.1"

organization := "com.adelegue"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.3",
  "com.typesafe.akka" %% "akka-http-core" % "2.4.3",
  "com.typesafe.play" %% "play-json" % "2.5.1" cross CrossVersion.binary,
  "com.typesafe.akka" %% "akka-testkit" % "2.4.3" % "test",
  "org.elasticsearch" % "elasticsearch" % "2.3.1" % "test",
  "net.java.dev.jna"  % "jna" % "4.1.0" % "test",
  "org.specs2"        %% "specs2"  % "2.4.15"  % "test"
)

publishTo := Some(Resolver.file("file",  new File("/Users/adelegue/idea/mvn-repo/releases" )))
