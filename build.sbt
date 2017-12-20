import sbt.Keys.{organization, scalacOptions}
import sbtrelease.ReleaseStateTransformations._

val akkaVersion = "2.5.6"
val akkaHttpVersion = "10.0.10"

lazy val root = (project in file("."))
  .enablePlugins(BuildInfoPlugin, GitVersioning, GitBranchPrompt)
    .settings(
      name := """elastic-scala-http""",
      organization := "com.adelegue",
      scalaVersion := "2.12.3",
      crossScalaVersions := Seq("2.11.8", scalaVersion.value),
      resolvers ++= Seq(
        Resolver.jcenterRepo,
        Resolver.bintrayRepo("larousso", "maven")
      ),
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-actor"     % akkaVersion,
        "com.typesafe.akka" %% "akka-stream"    % akkaVersion,
        "com.typesafe.akka" %% "akka-http"      % akkaHttpVersion,
        "com.typesafe.play" %% "play-json"      % "2.6.6",
        "com.adelegue"             %% "playjson-extended"             % "0.0.3",
        "com.typesafe.akka" %% "akka-testkit"   % akkaVersion       % "test",
        "org.scalatest"     %% "scalatest"      % "3.0.1"           % "test",
        "org.elasticsearch" % "elasticsearch" % "5.5.0" % "test",
        "org.elasticsearch.plugin" % "transport-netty4-client" % "5.5.0" % "test",
        "org.elasticsearch.plugin" % "reindex-client" % "5.5.0" % "test",
        "org.slf4j" % "slf4j-api" % "1.7.25" % "test",
        "org.apache.logging.log4j" % "log4j-api" % "2.8.2" % "test",
        "org.apache.logging.log4j" % "log4j-core" % "2.8.2" % "test"
      ),
      parallelExecution in Test := false,
      scalacOptions in Test ++= Seq("-Yrangepos")
    )
  .settings(publishSettings:_*)


lazy val githubRepo = "larousso/elastic-scala-http"

lazy val publishSettings =
  Seq(
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
    publishMavenStyle := true,
    publishArtifact in Test := false,
    bintrayVcsUrl := Some(s"scm:git:git@github.com:$githubRepo.git"),
    bintrayCredentialsFile := file(".credentials"),
    pomIncludeRepository := { _ => false },
    pomExtra := (
      <url>http://adelegue.org</url>
        <licenses>
          <license>
            <name>Apache 2</name>
            <url>http://www.apache.org/licenses/LICENSE-2.0</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>https://github.com/larousso/playjson-extended</url>
          <connection>https://github.com/larousso/playjson-extended</connection>
        </scm>
        <developers>
          <developer>
            <id>alexandre.delegue</id>
            <name>Alexandre Delègue</name>
            <url>https://github.com/larousso</url>
          </developer>
        </developers>
      )
  )

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("publish"),
  setNextVersion,
  commitNextVersion,
  pushChanges)