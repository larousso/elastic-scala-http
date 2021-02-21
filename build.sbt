import sbt.Keys.{organization, scalacOptions}
import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Sonatype._

val akkaVersion = "2.6.3"
val akkaHttpVersion = "10.1.11"

lazy val root = (project in file("."))
  .disablePlugins(disabledPlugins: _*)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    name := """elastic-scala-http""",
    organization := "com.adelegue",
    scalaVersion := "2.13.0",
    crossScalaVersions := List("2.12.9", "2.13.1"),
    resolvers ++= Seq(
      Resolver.jcenterRepo
    ),
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.play" %% "play-json" % "2.8.1",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.4",
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.8" % Test,
      "org.elasticsearch" % "elasticsearch" % "5.5.0" % Test,
      "org.elasticsearch.plugin" % "transport-netty4-client" % "5.5.0" % Test,
      "org.elasticsearch.plugin" % "reindex-client" % "5.5.0" % Test,
      "org.slf4j" % "slf4j-api" % "1.7.25" % Test,
      "org.apache.logging.log4j" % "log4j-api" % "2.8.2" % Test,
      "org.apache.logging.log4j" % "log4j-core" % "2.8.2" % Test
    ),
    parallelExecution in Test := false,
    scalacOptions in Test ++= Seq("-Yrangepos", "-deprecation")
  )
  .settings(publishCommonsSettings: _*)


lazy val githubRepo = "larousso/elastic-scala-http"

lazy val publishCommonsSettings = Seq(
  homepage := Some(url(s"https://github.com/$githubRepo")),
  startYear := Some(2017),
  licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
  scmInfo := Some(
    ScmInfo(
      url(s"https://github.com/$githubRepo"),
      s"scm:git:https://github.com/$githubRepo.git",
      Some(s"scm:git:git@github.com:$githubRepo.git")
    )
  ),
  developers := List(
    Developer("alexandre.delegue", "Alexandre Delègue", "", url(s"https://github.com/larousso"))
  ),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  bintrayVcsUrl := Some(s"scm:git:git@github.com:$githubRepo.git"),
  sonatypeProfileName := "com.adelegue",
  sonatypeProjectHosting := Some(GitHubHosting("larousso", "elastic-scala-http", "user@example.com"))
)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
