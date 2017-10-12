package com.adelegue.elastic.client.akka

import java.io.IOException
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util
import java.util.UUID
import javafx.scene.NodeBuilder

import org.elasticsearch.common.settings.Settings
import org.elasticsearch.index.reindex.ReindexPlugin
import org.elasticsearch.node.{InternalSettingsPreparer, Node}
import org.elasticsearch.plugins.Plugin
import org.elasticsearch.transport.Netty4Plugin

/**
  * Created by adelegue on 12/04/2016.
  */
class ElasticEmbeddedServer {

  private val clusterName = s"elastic-${UUID.randomUUID.toString}"
  private val homeDir = Files.createTempDirectory(s"tmp_elastic_home").toFile
  private val dataDir = Files.createTempDirectory(s"tmp_elastic_data").toFile

  private val settings = Settings.builder()
    .put("path.data", dataDir.toString)
    .put("path.home", homeDir.toString)
    .put("cluster.name", clusterName)
    .put("http.enabled", "true")
    .put("http.port", "10901")
    .put("transport.tcp.port", "10902")
    .build

  private lazy val node = MyNode(settings, util.Arrays.asList(classOf[Netty4Plugin], classOf[ReindexPlugin]))

  def run(): Unit = {
    println("Starting elasticsearch embedded server")
    node.start()
    println("Server started")
  }

  def stop(): Unit = {
    node.close()
    try {
      DeleteDir(dataDir.toPath)
    } catch {
      case e: Exception => println(s"Data directory ${dataDir.toPath} cleanup failed")
    }
    try {
      DeleteDir(homeDir.toPath)
    } catch {
      case e: Exception => println(s"Home directory ${homeDir.toPath} cleanup failed")
    }
  }
}

import java.util

case class MyNode(
      preparedSettings: Settings,
      classpathPlugins: util.Collection[Class[_ <: Plugin]]
) extends Node(InternalSettingsPreparer.prepareEnvironment(preparedSettings, null), classpathPlugins)


object DeleteDir extends (Path => Unit) {

  override def apply(source: Path): Unit = {

    Files.walkFileTree(source, java.util.EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
      new SimpleFileVisitor[Path]() {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }
        override def postVisitDirectory(dir: Path, e: IOException): FileVisitResult = {
          if (e == null) {
            Files.delete(dir)
            FileVisitResult.CONTINUE
          } else throw e
        }
      }
    )
  }
}