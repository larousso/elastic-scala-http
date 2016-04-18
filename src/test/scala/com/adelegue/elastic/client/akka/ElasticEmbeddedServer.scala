package com.adelegue.elastic.client.akka

import java.io.IOException
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID

import org.elasticsearch.common.settings.Settings
import org.elasticsearch.node.NodeBuilder

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

  private lazy val node = NodeBuilder.nodeBuilder().local(true).settings(settings).build

  def run(): Unit = node.start()

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