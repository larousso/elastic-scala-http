package elastic.es6.client

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.LogSource
import akka.http.scaladsl.model.Uri.{Path, Query}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import elastic.es6.api._
import org.reactivestreams.Publisher

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

case class ElasticClientConfig(server: Uri, credentials: Option[Authorization] = None, clusterVersion: Int = 5)

object ElasticClient {

  def fromServer[JsonR](server: String, user: String = "", password: String = "")(implicit actorSystem: ActorSystem = ActorSystem("ElasticClient")): ElasticClient[JsonR] = {
    val credentials = for {
      u <- Option(user).filterNot(s => s == "")
      p <- Option(password).filterNot(s => s == "")
    } yield Authorization(BasicHttpCredentials(u, p))
    new ElasticClient[JsonR](ElasticClientConfig(Uri(server), credentials = credentials))
  }

  def apply[JsonR](host: String = "localhost", port: Int = 9200, scheme: String = "http", user: String = "", password: String = "")(implicit actorSystem: ActorSystem = ActorSystem("ElasticClient")): ElasticClient[JsonR] = {
    val credentials = for {
      u <- Option(user).filterNot(s => s == "")
      p <- Option(password).filterNot(s => s == "")
    } yield Authorization(BasicHttpCredentials(u, p))
    val server = Uri().withScheme(scheme).withHost(host).withPort(port)
    new ElasticClient[JsonR](ElasticClientConfig(server, credentials = credentials))
  }

  def apply[JsonR](config: ElasticClientConfig)(implicit actorSystem: ActorSystem): ElasticClient[JsonR] = new ElasticClient[JsonR](config)(actorSystem)
}


/**
  * Created by adelegue on 12/04/2016.
  */
class ElasticClient[JsonR](config: ElasticClientConfig)(implicit actorSystem: ActorSystem) extends Elastic[JsonR] {

  import akka.event.Logging

  private implicit val mat = ActorMaterializer()
  private implicit val logSource = new LogSource[ElasticClient[JsonR]] {
    override def genString(t: ElasticClient[JsonR]) = "ElasticClient"
  }

  private val logger = Logging(actorSystem, this)

  private val http: HttpExt = Http()

  private val baseUri = config.server

  private val _this = this

  private def notImplemented[T] = Future.failed[T](new RuntimeException("Unimplemented"))

  private def buildRequest(path: Path, method: HttpMethod, body: Option[String] = None, query: Option[Query] = None, contentType: ContentType = ContentTypes.`application/json`): HttpRequest = {
    val uri: Uri = query.fold(baseUri.withPath(path))(baseUri.withPath(path).withQuery)
    val headers: immutable.Seq[HttpHeader] = config.credentials.toList
    val httpEntity: UniversalEntity = body
      .map(b => HttpEntity.Strict(contentType, ByteString(b)))
      .getOrElse(HttpEntity.Empty)
      .withoutSizeLimit()
    HttpRequest(method, uri, entity = httpEntity, headers = headers)
  }

  private def request(path: Path, method: HttpMethod, body: Option[String] = None, query: Option[Query] = None, contentType: ContentType = ContentTypes.`application/json`, expectedStatus: Seq[StatusCode] = Seq(StatusCodes.OK, StatusCodes.Created))(implicit jsonReader: Reader[String, JsonR], ec: ExecutionContext): Future[String] = {
    val start = System.currentTimeMillis()
    val resp = http.singleRequest(buildRequest(path, method, body, query, contentType)).flatMap {
      case HttpResponse(code, _, entity, _) if expectedStatus contains code =>
        entity.dataBytes.map(_.utf8String).runFold("")((str, acc) => str + acc)
      case HttpResponse(code, _, entity, _) =>
        entity.dataBytes
          .map(_.utf8String)
          .runFold("")((str, acc) => str + acc)
          .flatMap(cause =>
            Future.failed(new EsException[JsonR](jsonReader.read(cause), code.intValue(), cause))
          )
    }

    resp.onComplete {
      case Success(_) =>
        logger.debug(s"{} {} {} Success in {}", method.value, path, query.map(q => s"query = ${q.mkString(",")}").getOrElse(""), System.currentTimeMillis() - start)
      case Failure(_) =>
        logger.debug(s"{} {} {} Failed in {}", method.value, path, query.map(q => s"query = ${q.mkString(",")}").getOrElse(""), System.currentTimeMillis() - start)
    }

    resp
  }

  def get(path: Path, body: Option[String] = None, query: Option[Query] = None, contentType: ContentType = ContentTypes.`application/json`, expectedStatus: Seq[StatusCode] = Seq(StatusCodes.OK))(implicit jsonReader: Reader[String, JsonR], ec: ExecutionContext): Future[String] =
    request(path, HttpMethods.GET, body, query, contentType, expectedStatus)

  def post(path: Path, body: Option[String] = None, query: Option[Query] = None, contentType: ContentType = ContentTypes.`application/json`, expectedStatus: Seq[StatusCode] = Seq(StatusCodes.OK, StatusCodes.Created))(implicit jsonReader: Reader[String, JsonR], ec: ExecutionContext): Future[String] =
    request(path, HttpMethods.POST, body, query, contentType, expectedStatus)

  def put(path: Path, body: Option[String] = None, query: Option[Query] = None, contentType: ContentType = ContentTypes.`application/json`, expectedStatus: Seq[StatusCode] = Seq(StatusCodes.OK, StatusCodes.Created))(implicit jsonReader: Reader[String, JsonR], ec: ExecutionContext): Future[String] =
    request(path, HttpMethods.PUT, body, query, contentType, expectedStatus)

  def delete(path: Path, body: Option[String] = None, query: Option[Query] = None, contentType: ContentType = ContentTypes.`application/json`, expectedStatus: Seq[StatusCode] = Seq(StatusCodes.OK, StatusCodes.Created))(implicit jsonReader: Reader[String, JsonR], ec: ExecutionContext): Future[String] =
    request(path, HttpMethods.DELETE, body, query, contentType, expectedStatus)

  private def indexPath(names: Seq[String], types: Seq[String]) = names match {
    case Nil | Seq() => Path.Empty / "*" ++ toPath(types)
    case _ => Path.Empty / names.mkString(",") ++ toPath(types)
  }

  private def toPath(values: Seq[String]) = values match {
    case Nil | Seq() => Path.Empty
    case _ => Path.Empty / values.mkString(",")
  }


  override def verifyIndex(name: String)(implicit jsonReader: Reader[String, JsonR], ec: ExecutionContext): Future[Boolean] = {
    http.singleRequest(buildRequest(Path.Empty / name, HttpMethods.HEAD, None)).flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        entity.dataBytes.runWith(Sink.ignore).map(_ => true)
      case HttpResponse(StatusCodes.NotFound, headers, entity, _) =>
        entity.dataBytes.runWith(Sink.ignore).map(_ => false)
      case HttpResponse(code, _, entity, _) =>
        entity.dataBytes
          .map(_.utf8String)
          .runFold("")((str, acc) => str + acc)
          .flatMap(cause =>
            Future.failed(new EsException[JsonR](jsonReader.read(cause), code.intValue(), cause))
          )
    }
  }

  override def createIndex[S](name: String, settings: Option[S])(implicit mWrites: Writer[S, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] = {
    val strSettings = settings.map(s => mWrites.write(s))
    put(Path.Empty / name, strSettings).map(str => jsonReader.read(sReader.read(str)))
  }

  override def getIndex(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    get(Path.Empty / name, None).map(str => sReader.read(str))
  }

  override def deleteIndex(name: String, `type`: Option[String])(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] = {
    delete(indexPath(Seq(name), `type`.toList), None).map(str => jsonReader.read(sReader.read(str)))
  }

  override def createAliases[S](settings: S)(implicit mWrites: Writer[S, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] = {
    val strSettings = mWrites.write(settings)
    post(Path.Empty / "_aliases", Some(strSettings)).map(str => jsonReader.read(sReader.read(str)))
  }

  override def putMapping[M](name: String, `type`: String, mapping: M, update_all_types: Boolean)(implicit mWrites: Writer[M, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] = {
    val strMapping = mWrites.write(mapping)
    val query: Option[Query] = Some(update_all_types).filter(_.booleanValue).map(_ => Query("update_all_types"))
    put(Path.Empty / name / "_mapping" / `type`, Some(strMapping), query).map(str => jsonReader.read(sReader.read(str)))
  }

  override def getMapping(index: String, `type`: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    getMappings(Seq(index), Seq(`type`))(sReader, ec)
  }

  override def getMappings(index: Seq[String], `type`: Seq[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    val indexPath: Path = if (index.isEmpty) Path./ else Path.Empty / index.mkString(",")
    val typePath = if (`type`.isEmpty) "" else `type`.mkString(",")
    get(indexPath / "_mapping" / typePath).map(str => sReader.read(str))
  }

  override def analyse[Q](index: Option[String], query: Q)(implicit mWrites: Writer[Q, JsonR], jWrites: Writer[JsonR, String], sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    val path = index.map(i => Path.Empty / i / "_analyze").getOrElse(Path.Empty / "_analyze")
    get(path, Some(jWrites.write(mWrites.write(query)))).map(sReader.read)
  }

  override def getTemplate(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    val path = Path.Empty / "_template" / name
    get(path).map(sReader.read)
  }

  override def verifyTemplate(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[Boolean] = {
    http.singleRequest(buildRequest(Path.Empty / "_template" / name, HttpMethods.HEAD, None)).flatMap {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        entity.dataBytes.runWith(Sink.ignore).map(_ => true)
      case HttpResponse(StatusCodes.NotFound, headers, entity, _) =>
        entity.dataBytes.runWith(Sink.ignore).map(_ => false)
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
    put(path, Some(body)).map(sReader.read).map(jsonReader.read)
  }

  override def deleteTemplate(name: String)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] = {
    val path = Path.Empty / "_template" / name
    delete(path).map(sReader.read).map(jsonReader.read)
  }

  override def stats(indexes: Seq[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    get(Path.Empty / indexes.mkString(",") / "_stats").map(sReader.read)
  }

  override def forceMerge(indexes: Seq[String], max_num_segments: Option[Int], only_expunge_deletes: Boolean, flush: Boolean)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]] = {
    val query = Some(Query(Seq(
      max_num_segments.map(n => "max_num_segments" -> n.toString),
      Some(only_expunge_deletes).filter(_.booleanValue).map(_ => "only_expunge_deletes" -> "true"),
      Some(flush).filterNot(_.booleanValue).map(_ => "flush" -> "true")
    ).flatten: _*))
    val path: Path = Path.Empty / indexes.mkString(",") / "_forcemerge"
    post(path, query = query).map(sReader.read).map(jsonReader.read)
  }

  override def shardStores(indexes: Seq[String], status: Option[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    val path = Path.Empty / indexes.mkString(",") / "_shard_stores"
    get(path).map(sReader.read)
  }

  override def upgradeStatus(index: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    val path = Path.Empty / index / "_upgrade"
    get(path).map(sReader.read)
  }

  override def upgrade(index: String, only_ancient_segments: Boolean)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    val path = Path.Empty / index / "_upgrade"
    post(path, query = Some(Query("only_ancient_segments" -> only_ancient_segments.toString))).map(sReader.read)
  }

  override def flush(indexes: Seq[String], wait_if_ongoing: Boolean = false, force: Boolean = false)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]] = {
    val query = Some(Query(Seq(
      Some(wait_if_ongoing).filter(_.booleanValue).map(_ => "wait_if_ongoing" -> "true"),
      Some(force).filter(_.booleanValue).map(_ => "force" -> "true")
    ).flatten: _*))
    post(Path.Empty / indexes.mkString(",") / "_flush", query = query).map(sReader.read).map(jsonReader.read)
  }

  override def refresh(indexes: String*)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]] = {
    post(indexPath(indexes, Seq()) / "_refresh").map(s => jsonReader.read(sReader.read(s)))
  }

  override def segments(indexes: Seq[String], verbose: Boolean)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    get(Path.Empty / indexes.mkString(",") / "_segments", query = Some(Query("verbose" -> verbose.toString))).map(sReader.read)
  }

  override def clearCache(indexes: Seq[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    get(Path.Empty / indexes.mkString(",") / "_cache" / "clear").map(sReader.read)
  }

  override def recovery(indexes: Seq[String], detailed: Boolean, active_only: Boolean)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] = {
    val query = Some(Query(
      "detailed" -> detailed.toString,
      "active_only" -> active_only.toString
    ))
    get(Path.Empty / indexes.mkString(",") / "_recovery", query = query).map(sReader.read)
  }

  override def clusterHealth()(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] =
    get(Path.Empty / "_cluster" / "health").map(sReader.read)

  override def mget(index: Option[String] = None, `type`: Option[String] = None, request: MGets)(implicit sWriter: Writer[JsonR, String], jsonWriter: Writer[MGets, JsonR], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, MGetResponse[JsonR]], ec: ExecutionContext): Future[MGetResponse[JsonR]] = {
    val indexPath = index.map(i => Path.Empty / i).map(p => `type`.map(t => p / t).getOrElse(p)).getOrElse(Path.Empty)
    val body = Some(sWriter.write(jsonWriter.write(request)))
    get(indexPath / "_mget", body).map(sReader.read).map(jsonReader.read)
  }

  override def search[Q](index: Seq[String] = Seq.empty[String], `type`: Seq[String] = Seq.empty[String], query: Q, from: Int = 0, size: Int = 20, search_type: SearchType = QUERY_THEN_FETCH, request_cache: Boolean, terminate_after: Option[Int], timeout: Option[Int])(implicit qWrites: Writer[Q, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Future[SearchResponse[JsonR]] = {
    val indexPath: Path = if (index.isEmpty) Path./ else Path.Empty / index.mkString(",")
    val typePath = if (`type`.isEmpty) "" else `type`.mkString(",")
    get(indexPath / typePath / "_search", Some(qWrites.write(query))).map(str => jsonReader.read(sReader.read(str)))
  }

  override def bulk[D](index: Option[String], `type`: Option[String], publisher: Publisher[Bulk[D]], batchSize: Int)(implicit qWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Publisher[BulkResponse[JsonR]] = {
    Source.fromPublisher(publisher)
      .grouped(batchSize)
      .mapAsync(1) { group => oneBulk(index, `type`, group)}
      .runWith(Sink.asPublisher(fanout = true))
  }

  override def bulkFlow[D](index: Option[String], `type`: Option[String], batchSize: Int)(implicit sWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Flow[Bulk[D], BulkResponse[JsonR], NotUsed] = {
    Flow[Bulk[D]].grouped(batchSize).mapAsync(1) { group => oneBulk(index, `type`, group)}
  }

  override def oneBulk[D](index: Option[String], `type`: Option[String], bulk: Seq[Bulk[D]])(implicit sWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Future[BulkResponse[JsonR]] = {
    val indexPath: Option[Path] = index.map(i => Path.Empty / i).map(p => `type`.map(t => p / t).getOrElse(p))
    val body: String = bulk.flatMap(b => Seq(Some(bulkOpWriter.write(b.operation)), b.source.map(docWriter.write)).flatten).map(sWrites.write).mkString("\n") + "\n"
    post(indexPath.getOrElse(Path.Empty) / "_bulk", Some(body)).map(str => bReader.read(sReader.read(str)))
  }

  override def scroll[Q](index: Seq[String] = Seq.empty, `type`: Seq[String] = Seq.empty, query: Q, scroll: String = "1m", size: Int = 20)(implicit qWrites: Writer[Q, String], jsonWriter: Writer[Scroll, JsonR], sWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Source[SearchResponse[JsonR], NotUsed] = {
    val path = indexPath(index, `type`)
    val querys: Seq[(String, String)] = Seq(
      "scroll" -> scroll,
      "size" -> size.toString
    )

    Source
      .fromFuture(request(path / "_search", HttpMethods.GET, Some(qWrites.write(query)), Some(Query(querys: _*))))
      .map(str => jsonReader.read(sReader.read(str)))
      .flatMapConcat { resp =>
        resp.scroll_id match {
          case Some(id) =>
            Source.single(resp).concat(
              Source.unfoldAsync(id) { scroll_id =>
                post(Path.Empty / "_search" / "scroll", Some(sWriter.write(jsonWriter.write(Scroll(scroll, scroll_id)))))
                  .map { str => jsonReader.read(sReader.read(str)) }
                  .map { resp =>
                    if (resp.hits.hits.isEmpty) {
                      None
                    } else {
                      resp.scroll_id.map(id => (id, resp))
                    }
                  }
              }
            )
          case None => Source.empty
        }
      }
  }

  override def scrollPublisher[Q](index: Seq[String] = Seq.empty, `type`: Seq[String] = Seq.empty, query: Q, scroll: String = "1m", size: Int = 20)(implicit qWrites: Writer[Q, String], jsonWriter: Writer[Scroll, JsonR], sWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Publisher[SearchResponse[JsonR]] = {
    val s: Source[SearchResponse[JsonR], NotUsed] = this.scroll(index, `type`, query, scroll, size)
    s.runWith(Sink.asPublisher(fanout = true))
  }

  override def aggregation[Q](index: Seq[String], `type`: Seq[String], query: Q, from: Option[Int], size: Option[Int], search_type: Option[SearchType], request_cache: Boolean, terminate_after: Option[Int], timeout: Option[Int])(implicit qWrites: Writer[Q, String], respReads: Reader[String, SearchResponse[JsonR]], jsonReader: Reader[String, JsonR], ec: ExecutionContext): Future[SearchResponse[JsonR]] = notImplemented[SearchResponse[JsonR]]

  override def index(name: String, `type`: Option[String] = None): Index[JsonR] = EsIndex(name, `type`)

  override def index(typeDefinition: TypeDefinition): Index[JsonR] = EsIndex(typeDefinition.name, Some(typeDefinition.`type`))

  case class EsIndex(name: String, `type`: Option[String] = None) extends Index[JsonR] {

    private val indexPath = _this.indexPath(Seq(name), `type`.toList)

    override def /(`type`: String): Index[JsonR] = EsIndex(name, Some(`type`))

    override def get(id: String, routing: Option[String], fields: Seq[String], _source: Boolean)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, GetResponse[JsonR]], ec: ExecutionContext): Future[GetResponse[JsonR]] = {
      if (`type`.isEmpty) throw new IllegalArgumentException("type is required to get document")
      val querys: Seq[(String, String)] = Seq(
        Some(_source).filterNot(_.booleanValue()).map(_ => "_source" -> "false"),
        Some(fields).filterNot(_.isEmpty).map(f => "fields" -> f.mkString(",")),
        routing.map(r => "routing" -> r)
      ).flatten
      val query: Option[Query] = if (querys.isEmpty) None else Some(Query(querys: _*))
      _this.get(indexPath / id, None, query).map(str => jsonReader.read(sReader.read(str)))
    }

    override def index[D](data: D, id: Option[String], version: Option[Int], versionType: Option[VersionType], create: Boolean, routing: Option[String], parent: Option[String], refresh: Boolean, timeout: Option[String], consistency: Option[Consistency], detectNoop: Boolean)(implicit writer: Writer[D, JsonR], strWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]] = {
      if (`type`.isEmpty) throw new IllegalArgumentException("type is required to index document")
      val querys: Seq[(String, String)] = Seq(
        Some(refresh).filter(b => b).map(_ => "refresh" -> "true"),
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
          put(indexPath / _id / "_create", Some(strWriter.write(writer.write(data))), query).map(str => jsonReader.read(sReader.read(str)))
        case Some(_id) =>
          put(indexPath / _id, Some(strWriter.write(writer.write(data))), query).map(str => jsonReader.read(sReader.read(str)))
        case None =>
          post(indexPath, Some(strWriter.write(writer.write(data))), query).map(str => jsonReader.read(sReader.read(str)))
      }
    }

    override def update[D](data: D, id: String, version: Option[Int], versionType: Option[VersionType], retry_on_conflict: Option[Int], routing: Option[String], parent: Option[String], timeout: Option[String], refresh: Boolean, wait_for_active_shards: Option[Boolean])(implicit writer: Writer[D, JsonR], strWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]] = {
      if (`type`.isEmpty) throw new IllegalArgumentException("type is required to index document")
      val querys: Seq[(String, String)] = Seq(
        Some(refresh).filter(b => b).map(_ => "refresh" -> "true"),
        retry_on_conflict.map(c => "retry_on_conflict" -> c.toString),
        wait_for_active_shards.map(c => "wait_for_active_shards" -> c.toString),
        version.map(v => "version" -> v.toString),
        versionType.map(v => "version_type" -> v.value),
        parent.map(p => "parent" -> p),
        timeout.map(t => "timeout" -> t),
        routing.map(r => "routing" -> r)
      ).flatten

      val query: Option[Query] = if (querys.isEmpty) None else Some(Query(querys: _*))
      post(indexPath / id / "_update", Some(strWriter.write(writer.write(data))), query).map(str => jsonReader.read(sReader.read(str)))
    }

    override def delete(id: String, refresh: Boolean = false, routing: Option[String] = None)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]] = {
      if (`type`.isEmpty) throw new IllegalArgumentException("type is required to delete document")

      val querys: Seq[(String, String)] = Seq(
        Some(refresh).filter(b => b).map(_ => "refresh" -> "true"),
        routing.map(r => "routing" -> r)
      ).flatten

      val query: Option[Query] = if (querys.isEmpty) None else Some(Query(querys: _*))

      _this.delete(indexPath / id, query = query).map(str => jsonReader.read(sReader.read(str)))
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

    override def bulkFlow[D](batchSize: Int)(implicit sWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Flow[Bulk[D], BulkResponse[JsonR], NotUsed] =
      _this.bulkFlow(Some(name), `type`, batchSize)(sWrites, docWriter, bulkOpWriter, sReader, bReader, ec)

    override def oneBulk[D](bulk: Seq[Bulk[D]])(implicit sWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Future[BulkResponse[JsonR]] =
      _this.oneBulk(Some(name), `type`, bulk: Seq[Bulk[D]])(sWrites, docWriter, bulkOpWriter, sReader, bReader, ec)

    override def scrollPublisher[Q](query: Q, scroll: String, size: Int = 20)(implicit qWrites: Writer[Q, String], jsonWriter: Writer[Scroll, JsonR], sWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Publisher[SearchResponse[JsonR]] =
      _this.scrollPublisher(Seq(name), `type`.toSeq, query, scroll, size)(qWrites, jsonWriter, sWriter, sReader, jsonReader, ec)

    override def scroll[Q](query: Q, scroll: String, size: Int)(implicit qWrites: Writer[Q, String], jsonWriter: Writer[Scroll, JsonR], sWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Source[SearchResponse[JsonR], NotUsed] =
      _this.scroll(Seq(name), `type`.toSeq, query, scroll, size)(qWrites, jsonWriter, sWriter, sReader, jsonReader, ec)

  }
}
