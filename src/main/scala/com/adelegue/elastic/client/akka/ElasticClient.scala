package com.adelegue.elastic.client.akka

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.{Http, HttpExt}
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.adelegue.elastic.client.api._
import org.reactivestreams.Publisher

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by adelegue on 12/04/2016.
  */
class ElasticClient[JsonR](val host: String, val port: Int, val actorMaterializer: ActorMaterializer, val actorSystem: ActorSystem) extends Elastic[JsonR] {

  implicit val sys = actorSystem
  implicit val mat = actorMaterializer

  val http: HttpExt = Http(actorSystem)

  val baseUri = Uri().withScheme("http").withHost(host).withPort(port)

  private val _this = this

  def notImplemented[T] = Future.failed[T](new RuntimeException("Unimplemented"))

  def request(path: Path, method: HttpMethod, body: Option[String] = None, query: Option[Query] = None): HttpRequest = {
    val uri: Uri = query.fold(baseUri.withPath(path))(baseUri.withPath(path).withQuery)
    HttpRequest(method, uri, entity = body.fold(HttpEntity.Empty)(HttpEntity.apply))
  }

  def simpleRequest(path: Path, method: HttpMethod, body: Option[String] = None, query: Option[Query] = None)(implicit jsonReader: Reader[String, JsonR], ec: ExecutionContext): Future[String] = {
    http.singleRequest(request(path, method, body, query)).flatMap {
      case HttpResponse(code, _, entity, _) if code == StatusCodes.OK || code == StatusCodes.Created =>
        entity.dataBytes.map(_.utf8String).runFold("")((str, acc) => str + acc)
      case HttpResponse(code, _, entity, _) =>
        entity.dataBytes
          .map(_.utf8String)
          .runFold("")((str, acc) => str + acc)
          .flatMap(cause =>
            Future.failed(new EsException[JsonR](jsonReader.read(cause), code.intValue(), cause))
          )
    }
  }

  private def indexPath(names: Seq[String], types: Seq[String]) = names match {
    case Nil | Seq() => Path.Empty / "*" ++ toPath(types)
    case _ => Path.Empty / names.mkString(",") ++ toPath(types)
  }

  def toPath(values: Seq[String]) = values match {
    case Nil | Seq() => Path.Empty
    case _ => Path.Empty / values.mkString(",")
  }


  override def verifyIndex(name: String)(implicit jsonReader: Reader[String, JsonR], ec: ExecutionContext): Future[Boolean] = {
    http.singleRequest(request(Path.Empty / name, HttpMethods.HEAD, None)).flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Future.successful(true)
      case HttpResponse(StatusCodes.NotFound, headers, entity, _) =>
        Future.successful(false)
      case HttpResponse(code, _, entity, _) =>
        entity.dataBytes
          .map(_.utf8String)
          .runFold("")((str, acc) => str + acc)
          .flatMap(cause =>
            Future.failed(new EsException[JsonR](jsonReader.read(cause), code.intValue(), cause))
          )
    }
  }

  override def createIndex[S](name: String, settings: S)(implicit mWrites: Writer[S, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] = {
    val strSettings = mWrites.write(settings)
    simpleRequest(Path.Empty / name, HttpMethods.PUT, Some(strSettings)).map(str => jsonReader.read(sReader.read(str)))
  }

  override def getIndex(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    simpleRequest(Path.Empty / name, HttpMethods.GET, None).map(str => sReader.read(str))
  }

  override def deleteIndex(name: String, `type`: Option[String])(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] = {
    simpleRequest(indexPath(Seq(name), `type`.toList), HttpMethods.DELETE, None).map(str => jsonReader.read(sReader.read(str)))
  }

  override def createAliases[S](settings: S)(implicit mWrites: Writer[S, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] = {
    val strSettings = mWrites.write(settings)
    simpleRequest(Path.Empty / "_aliases", HttpMethods.POST, Some(strSettings)).map(str => jsonReader.read(sReader.read(str)))
  }

  override def putMapping[M](name: String, `type`: String, mapping: M, update_all_types: Boolean)(implicit mWrites: Writer[M, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] = {
    val strMapping = mWrites.write(mapping)
    val query: Option[Query] = Some(update_all_types).filter(_.booleanValue).map(_ => Query("update_all_types"))
    simpleRequest(Path.Empty / name / "_mapping" / `type`, HttpMethods.PUT, Some(strMapping), query).map(str => jsonReader.read(sReader.read(str)))
  }

  override def getMapping(index: String, `type`: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    getMappings(Seq(index), Seq(`type`))(sReader, ec)
  }

  override def getMappings(index: Seq[String], `type`: Seq[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    val indexPath: Path = if (index.isEmpty) Path./ else Path.Empty / index.mkString(",")
    val typePath = if (`type`.isEmpty) "" else `type`.mkString(",")
    simpleRequest(indexPath / "_mapping" / typePath, HttpMethods.GET, None).map(str => sReader.read(str))
  }

  //TODO
  override def analyse[Q](query: Q)(implicit mWrites: Writer[Q, JsonR], jWrites: Writer[JsonR, String], sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = notImplemented

  override def getTemplate(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    val path = Path.Empty / "_template" / name
    simpleRequest(path, HttpMethods.GET).map(sReader.read)
  }

  override def verifyTemplate(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[Boolean] = {
    http.singleRequest(request(Path.Empty / "_template" / name, HttpMethods.HEAD, None)).flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        Future.successful(true)
      case HttpResponse(StatusCodes.NotFound, headers, entity, _) =>
        Future.successful(false)
      case HttpResponse(code, _, entity, _) =>
        entity.dataBytes
          .map(_.utf8String)
          .runFold("")((str, acc) => str + acc)
          .flatMap(cause =>
            Future.failed(new EsException[JsonR](sReader.read(cause), code.intValue(), cause))
          )
    }
  }


  override def putTemplate[T](name: String, template: T)(implicit mWrites: Writer[T, JsonR], jWrites: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] = {
    val path = Path.Empty / "_template" / name
    val body = jWrites.write(mWrites.write(template))
    simpleRequest(path, HttpMethods.PUT, Some(body)).map(sReader.read).map(jsonReader.read)
  }

  override def deleteTemplate(name: String)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] = {
    val path = Path.Empty / "_template" / name
    simpleRequest(path, HttpMethods.DELETE).map(sReader.read).map(jsonReader.read)
  }

  //TODO
  override def stats(indexes: Seq[String], stats: Seq[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = notImplemented

  //TODO
  override def forceMerge(indexes: Seq[String], max_num_segments: Option[Int], only_expunge_deletes: Boolean, flush: Boolean)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = notImplemented

  //TODO
  override def shardStores(indexes: Seq[String], status: Option[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = notImplemented

  //TODO
  override def upgradeStatus(index: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = notImplemented

  //TODO
  override def flush(indexes: Seq[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = notImplemented


  override def refresh(indexes: Seq[String])(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]] = {
    simpleRequest(indexPath(indexes, Seq()) / "_refresh", HttpMethods.POST).map(s => jsonReader.read(sReader.read(s)))
  }

  //TODO
  override def upgrade(index: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = notImplemented

  //TODO
  override def segments(indexes: Seq[String], verbose: Boolean)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = notImplemented

  //TODO
  override def clearCache(indexes: Seq[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = notImplemented

  //TODO
  override def recovery(indexes: Seq[String], detailed: Boolean, active_only: Boolean)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = notImplemented

  override def mget(index: Option[String] = None, `type`: Option[String] = None, request: MGets)(implicit sWriter: Writer[JsonR, String], jsonWriter: Writer[MGets, JsonR], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, MGetResponse[JsonR]], ec: ExecutionContext): Future[MGetResponse[JsonR]] = {
    val indexPath = index.map(i => Path.Empty / i).map(p => `type`.map(t => p / t).getOrElse(p)).getOrElse(Path.Empty)
    val body = Some(sWriter.write(jsonWriter.write(request)))
    simpleRequest(indexPath / "_mget", HttpMethods.GET, body).map(sReader.read).map(jsonReader.read)
  }

  override def search[Q](index: Seq[String], `type`: Seq[String], query: Q, from: Option[Int], size: Option[Int], search_type: Option[SearchType], request_cache: Boolean, terminate_after: Option[Int], timeout: Option[Int])(implicit qWrites: Writer[Q, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Future[SearchResponse[JsonR]] = {
    val indexPath: Path = if (index.isEmpty) Path./ else Path.Empty / index.mkString(",")
    val typePath = if (`type`.isEmpty) "" else `type`.mkString(",")
    simpleRequest(indexPath / typePath / "_search", HttpMethods.GET, Some(qWrites.write(query))).map(str => jsonReader.read(sReader.read(str)))
  }

  override def bulk[D](index: Option[String], `type`: Option[String], publisher: Publisher[Bulk[D]], batchSize: Int)(implicit qWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Publisher[BulkResponse[JsonR]] = {
    val indexPath: Option[Path] = index.map(i => Path.Empty / i).map(p => `type`.map(t => p / t).getOrElse(p))
    Source.fromPublisher(publisher)
      .grouped(batchSize)
      .mapAsync(1) { group =>
        val body: String = group.flatMap(b => Seq(Some(bulkOpWriter.write(b.operation)), b.source.map(docWriter.write)).flatten).map(qWrites.write).mkString("\n") + "\n"
        simpleRequest(indexPath.getOrElse(Path.Empty) / "_bulk", HttpMethods.POST, Some(body)).map(str => bReader.read(sReader.read(str)))
      }
      .runWith(Sink.asPublisher(fanout = true))
  }

  override def scrollSearch[Q](index: Seq[String], `type`: Seq[String], query: Q, scroll: String = "1m", size: Option[Int] = None)(implicit qWrites: Writer[Q, String], jsonWriter: Writer[Scroll, JsonR], sWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Publisher[SearchResponse[JsonR]] = {
    val path = indexPath(index, `type`)
    val querys: Seq[(String, String)] = Seq(
      Some(scroll).map(_ => "scroll" -> scroll),
      size.map(s => "size" -> s.toString)
    ).flatten

    Source
      .fromFuture(simpleRequest(path / "_search", HttpMethods.GET, Some(qWrites.write(query)), Some(Query(querys: _*))))
      .map(str => jsonReader.read(sReader.read(str)))
      .flatMapConcat { resp =>
        val Some(scroll_id) = resp.scroll_id
        Source.single(resp).merge(nextScroll(scroll_id, scroll)(jsonWriter, sWriter, sReader, jsonReader, ec))
      }
      .runWith(Sink.asPublisher(fanout = true))
  }

  private def nextScroll(scroll_id: String, scroll: String)(implicit jsonWriter: Writer[Scroll, JsonR], sWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Source[SearchResponse[JsonR], NotUsed] = {
    val scrollRequest = Scroll(scroll, scroll_id)
    Source.fromFuture(simpleRequest(Path.Empty / "_search" / "scroll", HttpMethods.POST, Some(sWriter.write(jsonWriter.write(scrollRequest)))))
      .map(str => jsonReader.read(sReader.read(str)))
      .flatMapConcat { resp =>
        val single: Source[SearchResponse[JsonR], NotUsed] = Source.single(resp)
        if (resp.hits.hits.isEmpty) {
          single
        } else {
          single.merge(nextScroll(scroll_id, scroll))
        }
      }
  }

  override def aggregation[Q](index: Seq[String], `type`: Seq[String], query: Q, from: Option[Int], size: Option[Int], search_type: Option[SearchType], request_cache: Boolean, terminate_after: Option[Int], timeout: Option[Int])(implicit qWrites: Writer[Q, String], respReads: Reader[String, SearchResponse[JsonR]], jsonReader: Reader[String, JsonR], ec: ExecutionContext): Future[SearchResponse[JsonR]] = notImplemented[SearchResponse[JsonR]]


  /**
    * Ops at index level
    *
    * @param name   name of the index
    * @param `type` type of the index
    * @return
    */
  override def index(name: String, `type`: Option[String] = None): Index[JsonR] = new Index[JsonR] {

    private val indexPath = _this.indexPath(Seq(name), `type`.toList)


    override def get(id: String, routing: Option[String], fields: Seq[String], _source: Boolean)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, GetResponse[JsonR]], ec: ExecutionContext): Future[GetResponse[JsonR]] = {
      if (`type`.isEmpty) throw new IllegalArgumentException("type is required to get document")
      val querys: Seq[(String, String)] = Seq(
        Some(_source).filterNot(_.booleanValue()).map(_ => "_source" -> "false"),
        Some(fields).filterNot(_.isEmpty).map(f => "fields" -> f.mkString(",")),
        routing.map(r => "routing" -> r)
      ).flatten
      val query: Option[Query] = if (querys.isEmpty) None else Some(Query(querys: _*))
      simpleRequest(indexPath / id, HttpMethods.GET, None, query).map(str => jsonReader.read(sReader.read(str)))
    }

    override def index[D](data: D, id: Option[String], version: Option[Int], versionType: Option[VersionType], create: Boolean, routing: Option[String], parent: Option[String], refresh: Boolean, timeout: Option[String], consistency: Option[Consistency], detectNoop: Boolean)(implicit writer: Writer[D, JsonR], strWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]] = {
      if (`type`.isEmpty) throw new IllegalArgumentException("type is required to index document")
      val querys: Seq[(String, String)] = Seq(
        Some(refresh).filter(_.booleanValue()).map(_ => "refresh" -> "true"),
        consistency.map(c => "consistency" -> c.value),
        version.map(v => "version" -> v.toString),
        versionType.map(v => "version_type" -> v.value),
        parent.map(p => "parent" -> p),
        timeout.map(t => "timeout" -> t),
        routing.map(r => "routing" -> r)
      ).flatten

      val query: Option[Query] = if (querys.isEmpty) None else Some(Query(querys: _*))

      id match {
        case Some(_id) if create =>
          simpleRequest(indexPath / _id / "_create", HttpMethods.PUT, Some(strWriter.write(writer.write(data))), query).map(str => jsonReader.read(sReader.read(str)))
        case Some(_id) =>
          simpleRequest(indexPath / _id, HttpMethods.PUT, Some(strWriter.write(writer.write(data))), query).map(str => jsonReader.read(sReader.read(str)))
        case None =>
          simpleRequest(indexPath, HttpMethods.POST, Some(strWriter.write(writer.write(data))), query).map(str => jsonReader.read(sReader.read(str)))
      }
    }

    override def delete(id: String)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]] = {
      if (`type`.isEmpty) throw new IllegalArgumentException("type is required to delete document")
      simpleRequest(indexPath / id, HttpMethods.DELETE, None).map(str => jsonReader.read(sReader.read(str)))
    }

    override def mget(request: MGets)(implicit sWriter: Writer[JsonR, String], jsonWriter: Writer[MGets, JsonR], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, MGetResponse[JsonR]], ec: ExecutionContext): Future[MGetResponse[JsonR]] =
      _this.mget(Some(name), `type`, request)(sWriter, jsonWriter, sReader, jsonReader, ec)

    override def search[Q](query: Q)(implicit qWrites: Writer[Q, String], sReads: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Future[SearchResponse[JsonR]] =
      _this.search[Q](Seq(name), `type`.toList, query)(qWrites, sReads, jsonReader, ec)

    override def delete(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] = {
      _this.deleteIndex(name, `type`)(sReader, jsonReader, ec)
    }

    override def bulk[D](publisher: Publisher[Bulk[D]], batchSize: Int)(implicit qWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Publisher[BulkResponse[JsonR]] =
      _this.bulk(Some(name), `type`, publisher, batchSize)(qWrites, docWriter, bulkOpWriter, sReader, bReader, ec)

    override def scrollSearch[Q](query: Q, scroll: String, size: Option[Int])(implicit qWrites: Writer[Q, String], jsonWriter: Writer[Scroll, JsonR], sWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Publisher[SearchResponse[JsonR]] =
      _this.scrollSearch(Seq(name), `type`.toSeq, query, scroll, size)(qWrites, jsonWriter, sWriter, sReader, jsonReader, ec)
  }
}

class ElasticClientBuilder(
                            host: Option[String] = None,
                            port: Option[Int] = None,
                            actorMaterializer: Option[ActorMaterializer] = None,
                            actorSystem: Option[ActorSystem] = None) {

  def withHost(host: String) = new ElasticClientBuilder(Some(host), port, actorMaterializer, actorSystem)

  def withPort(port: Int) = new ElasticClientBuilder(host, Some(port), actorMaterializer, actorSystem)

  def withActorMaterializer(actorMaterializer: ActorMaterializer)() = new ElasticClientBuilder(host, port, Some(actorMaterializer), actorSystem)

  def withActorSystem(actorSystem: ActorSystem) = new ElasticClientBuilder(host, port, actorMaterializer, Some(actorSystem))

  def build[JsonR](): ElasticClient[JsonR] = {
    implicit val actorSys = actorSystem.getOrElse(ActorSystem("ElasticClient"))
    new ElasticClient[JsonR](host.getOrElse("localhost"), port.getOrElse(9200), actorMaterializer.getOrElse(ActorMaterializer()), actorSys)
  }

}

object ElasticClientBuilder {
  def apply(): ElasticClientBuilder = new ElasticClientBuilder()
}

object ElasticClient {

  def apply[JsonR](host: String = "localhost", port: Int = 9200)(implicit actorSystem: ActorSystem = ActorSystem("ElasticClient"), actorMaterializer: ActorMaterializer): ElasticClient[JsonR] = {
    new ElasticClient[JsonR](host, port, actorMaterializer, actorSystem)
  }
}
