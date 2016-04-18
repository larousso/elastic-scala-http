package com.adelegue.elastic.client.api

import org.reactivestreams.Publisher

import scala.concurrent.{ExecutionContext, Future}

/**
  * Api to access root operation on elasticsearch.
  *
  * @tparam JsonR a representation of json
  */
trait Elastic[JsonR] {

  /**
    * Référence to the index
    *
    * @param name of the index
    * @param `type` of the index
    * @return an Index référence.
    */
  def index(name: String, `type`: Option[String] = None): Index[JsonR]

  /**
    * Verify if the index exists
    *
    * @param name of the index
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return Boolean
    */
  def verifyIndex(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[Boolean]

  /**
    * Return the mapping of the index and type
    *
    * @param index
    * @param `type`
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def getMapping(index: String, `type`: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * Return the mappings
    *
    * @param index
    * @param `type`
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def getMappings(index: Seq[String], `type`: Seq[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * Get the index
    *
    * @param name
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def getIndex(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * Index creation
    *
    * @param name of the index
    * @param settings of the index
    * @param mWrites settings to string conversion
    * @param sReader json string to json object conversion
    * @param jsonReader json to IndexOps conversion
    * @param ec ExecutionContext for future execution
    * @tparam S settings type
    * @return IndexOps
    */
  def createIndex[S](name: String, settings: S)(implicit mWrites: Writer[S, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps]

  /**
    * Aliases creation
    *
    * @param settings of the aliases
    * @param mWrites settings to string conversion
    * @param sReader json string to json object conversion
    * @param jsonReader json to IndexOps conversion
    * @param ec ExecutionContext for future execution
    * @tparam S alias settings type
    * @return IndexOps
    */
  def createAliases[S](settings: S)(implicit mWrites: Writer[S, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps]

  /**
    * Delete index operation
    *
    * @param name name of the index
    * @param `type` the name of the type
    * @param sReader json string to json object conversion
    * @param jsonReader json to IndexOps conversion
    * @param ec ExecutionContext for future execution
    * @return IndexOps
    */
  def deleteIndex(name: String, `type`: Option[String] = None)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps]

  /**
    * Mapping modification
    *
    * @param name name of the index
    * @param `type` the name of the type
    * @param mapping the mapping to update
    * @param update_all_types to update all type if conflicts
    * @param mWrites mapping to json string conversion
    * @param sReader json string to json object conversion
    * @param jsonReader json object to IndexOps conversion
    * @param ec ExecutionContext for future execution
    * @tparam M mapping type
    * @return IndexOps
    */
  def putMapping[M](name: String, `type`: String, mapping: M, update_all_types: Boolean = false)(implicit mWrites: Writer[M, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps]

  /**
    * Analyse of query.
    *
    * @param query the query to analyse
    * @param qWrites query to json string conversion
    * @param jWrites Json object to json string conversion
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @tparam Q query type
    * @return Json object
    */
  def analyse[Q](query: Q)(implicit qWrites: Writer[Q, JsonR], jWrites: Writer[JsonR, String], sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * Add template
    *
    * @param name name of the template
    * @param template the template
    * @param mWrites T to Json object conversion
    * @param jWrites Json object to string conversion
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @tparam T template type
    * @return Json object
    */
  def putTemplate[T](name: String, template: T)(implicit mWrites: Writer[T, JsonR], jWrites: Writer[JsonR, String], sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * get template
    *
    * @param name name of the template
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return Json representation of the template
    */
  def getTemplate(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    *
    * @param name
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def deleteTemplate(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    *
    * @param name
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def verifyTemplate(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[Boolean]

  /**
    *
    * @param indexes
    * @param stats
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def stats(indexes: Seq[String], stats: Seq[String] = Seq())(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    *
    * @param indexes
    * @param verbose
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def segments(indexes: Seq[String], verbose: Boolean = false)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    *
    * @param indexes
    * @param detailed
    * @param active_only
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def recovery(indexes: Seq[String], detailed: Boolean = false, active_only: Boolean = false)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    *
    * @param indexes
    * @param status
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def shardStores(indexes: Seq[String], status: Option[String] = None)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    *
    * @param indexes
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def clearCache(indexes: Seq[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    *
    * @param indexes
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def refresh(indexes: Seq[String])(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]]

  /**
    *
    * @param indexes
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def flush(indexes: Seq[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    *
    * @param indexes
    * @param max_num_segments
    * @param only_expunge_deletes
    * @param flush
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def forceMerge(indexes: Seq[String], max_num_segments: Option[Int] = None, only_expunge_deletes: Boolean = false, flush: Boolean = false)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    *
    * @param index
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def upgrade(index: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    *
    * @param index
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @return
    */
  def upgradeStatus(index: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    *
    * @param index
    * @param `type`
    * @param query
    * @param from
    * @param size
    * @param search_type
    * @param request_cache
    * @param terminate_after
    * @param timeout
    * @param qWrites
    * @param sReader
    * @param jsonReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @tparam Q
    * @return
    */
  def search[Q](index: Seq[String], `type`: Seq[String], query: Q, from: Option[Int] = None, size: Option[Int] = None, search_type: Option[SearchType] = None, request_cache: Boolean = false, terminate_after: Option[Int] = None, timeout: Option[Int] = None)(implicit qWrites: Writer[Q, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Future[SearchResponse[JsonR]]

  /**
    *
    * @param index
    * @param `type`
    * @param query
    * @param from
    * @param size
    * @param search_type
    * @param request_cache
    * @param terminate_after
    * @param timeout
    * @param qWrites
    * @param respReads
    * @param sReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @tparam Q
    * @return
    */
  def aggregation[Q](index: Seq[String], `type`: Seq[String], query: Q, from: Option[Int] = None, size: Option[Int] = None, search_type: Option[SearchType] = None, request_cache: Boolean = false, terminate_after: Option[Int] = None, timeout: Option[Int] = None)(implicit qWrites: Writer[Q, String], respReads: Reader[String, SearchResponse[JsonR]], sReader: Reader[String, JsonR], ec: ExecutionContext): Future[SearchResponse[JsonR]]

  /**
    *
    * @param index
    * @param `type`
    * @param query
    * @param qWrites
    * @param sReader
    * @param jsonReader json string to json object conversion
    * @param ec ExecutionContext for future execution
    * @tparam Q
    * @return
    */
  def scrollSearch[Q](index: Seq[String], `type`: Seq[String], query: Q, scroll: String = "1m", size: Option[Int] = None)(implicit qWrites: Writer[Q, String], jsonWriter: Writer[Scroll, JsonR], sWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Publisher[SearchResponse[JsonR]]

  /**
    *
    * @param index
    * @param `type`
    * @param publisher
    * @param batchSize
    * @param qWrites
    * @param docWriter
    * @param bulkOpWriter
    * @param sReader
    * @param bReader
    * @param ec
    * @tparam D
    * @return
    */
  def bulk[D](index: Option[String] = None, `type`: Option[String] = None, publisher: Publisher[Bulk[D]], batchSize: Int)(implicit qWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Publisher[BulkResponse[JsonR]]

}

/**
  * Api to access index level operation.
  *
  * @tparam JsonR a representation of json.
  */
trait Index[JsonR] {

  /**
    * Index operation
    *
    * @param data
    * @param id
    * @param version
    * @param versionType
    * @param create
    * @param routing
    * @param parent
    * @param refresh
    * @param timeout
    * @param consistency
    * @param detectNoop
    * @param writer
    * @param strWriter
    * @param sReader
    * @param jsonReader
    * @param ec
    * @tparam D
    * @return
    */
  def index[D](data: D, id: Option[String] = None, version: Option[Int] = None, versionType: Option[VersionType] = None, create: Boolean = false, routing: Option[String] = None, parent: Option[String] = None, refresh: Boolean = false, timeout: Option[String]= None, consistency: Option[Consistency] = None, detectNoop: Boolean = false)(implicit writer: Writer[D, JsonR], strWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]]

  /**
    *
    * @param data
    * @param id
    * @param version
    * @param versionType
    * @param routing
    * @param parent
    * @param refresh
    * @param timeout
    * @param consistency
    * @param detectNoop
    * @param writer
    * @param strWriter
    * @param sReader
    * @param jsonReader
    * @param ec
    * @tparam D
    * @return
    */
  def update[D](data: D, id: String, version: Option[Int] = None, versionType: Option[VersionType] = None, routing: Option[String] = None, parent: Option[String] = None, refresh: Boolean = false, timeout: Option[String]= None, consistency: Option[Consistency] = None, detectNoop: Boolean = false)(implicit writer: Writer[D, JsonR], strWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]] =
    index(data, Some(id), version = version, versionType = versionType, create = false, routing = routing, parent = parent, refresh = refresh, timeout = timeout, consistency = consistency, detectNoop = detectNoop)

  /**
    *
    * @param data
    * @param id
    * @param version
    * @param versionType
    * @param routing
    * @param parent
    * @param refresh
    * @param timeout
    * @param consistency
    * @param detectNoop
    * @param writer
    * @param strWriter
    * @param sReader
    * @param jsonReader
    * @param ec
    * @tparam D
    * @return
    */
  def create[D](data: D, id: Option[String], version: Option[Int] = None, versionType: Option[VersionType] = None, routing: Option[String] = None, parent: Option[String] = None, refresh: Boolean = false, timeout: Option[String]= None, consistency: Option[Consistency] = None, detectNoop: Boolean = false)(implicit writer: Writer[D, JsonR], strWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]] =
    index(data, id, version = version, versionType = versionType, create = true, routing = routing, parent = parent, refresh = refresh, timeout = timeout, consistency = consistency, detectNoop = false)

  /**
    *
    * @param id
    * @param routing
    * @param fields
    * @param _source
    * @param sReader
    * @param jsonReader
    * @param ec
    * @return
    */
  def get(id: String, routing: Option[String] = None, fields: Seq[String] = Seq(), _source: Boolean = true)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, GetResponse[JsonR]], ec: ExecutionContext): Future[GetResponse[JsonR]]

  /**
    *
    * @param sReader
    * @param jsonReader
    * @param ec
    * @return
    */
  def delete(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps]

  /**
    *
    * @param id
    * @param sReader
    * @param jsonReader
    * @param ec
    * @return
    */
  def delete(id: String)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]]

  /**
    *
    * @param id
    * @param respReader
    * @param jsonReader
    * @param ec
    * @return
    */
  def mget(id: String)(implicit respReader: Reader[String, GetResponse[JsonR]], jsonReader: Reader[String, JsonR], ec: ExecutionContext): Future[GetResponse[JsonR]]

  /**
    *
    * @param query
    * @param qWrites
    * @param sReads
    * @param jsonReader
    * @param ec
    * @tparam Q
    * @return
    */
  def search[Q](query: Q)(implicit qWrites: Writer[Q, String], sReads: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Future[SearchResponse[JsonR]]

  /**
    *
    * @param publisher
    * @param batchSize
    * @param qWrites
    * @param docWriter
    * @param bulkOpWriter
    * @param sReader
    * @param bReader
    * @param ec
    * @tparam D
    * @return
    */
  def bulk[D](publisher: Publisher[Bulk[D]], batchSize: Int)(implicit qWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Publisher[BulkResponse[JsonR]]

  /**
    *
    * @param query
    * @param scroll
    * @param size
    * @param qWrites
    * @param jsonWriter
    * @param sWriter
    * @param sReader
    * @param jsonReader
    * @param ec
    * @tparam Q
    * @return
    */
  def scrollSearch[Q](query: Q, scroll: String = "1m", size: Option[Int] = None)(implicit qWrites: Writer[Q, String], jsonWriter: Writer[Scroll, JsonR], sWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Publisher[SearchResponse[JsonR]]

}

sealed abstract class Consistency(val value: String)
case object ALL extends Consistency("all")
case object ONE extends Consistency("one")
case object QUORUM extends Consistency("quorum")

sealed abstract class VersionType(val value: String)
case object INTERNAL extends VersionType("internal")
case object EXTERNAL extends VersionType("external")
case object EXTERNAL_GTE extends VersionType("external_gte")
case object FORCE extends VersionType("force")

sealed abstract class SearchType(val value: String)
case object DFS_QUERY_THEN_FETCH extends SearchType("dfs_query_then_fetch")
case object QUERY_THEN_FETCH extends SearchType("query_then_fetch")

/**
  * Data conversion
  */

trait Reader[In, Out] {
  def read(source: In): Out
}
object Reader {
  def apply[In, Out](fn: In => Out) = new Reader[In, Out] {
    override def read(source: In): Out = fn(source)
  }
}
trait Writer[In, Out] {
  def write(source: In): Out
}

object Writer {
  def apply[In, Out](fn: In => Out) = new Writer[In, Out] {
    override def write(source: In): Out = fn(source)
  }
}


class EsException[Json](val json: Json, val httpCode: Int, message: String) extends RuntimeException(message)

/**
  * Elastic request data structures.
  */

case class BulkOpDetail(_index: Option[String], _type: Option[String], _id: Option[String])
case class BulkOpType(index: Option[BulkOpDetail] = None, delete: Option[BulkOpDetail] = None, create: Option[BulkOpDetail] = None, update: Option[BulkOpDetail]= None)
case class Bulk[D](operation: BulkOpType, source: Option[D])

case class Scroll(scroll: String, scroll_id: String)


/**
  * Elastic responses data structures.
  */

trait ESResponse

case class IndexOps(acknowledged: Boolean) extends ESResponse

case class Shards[Json](total: Int, failed: Int, successful: Int, failures: Seq[Json]) extends ESResponse

case class IndexResponse[Json](_shards: Shards[Json], _index: Option[String], _type : Option[String], _id : Option[String], _version : Option[Int], created: Option[Boolean], found: Option[Boolean]) extends ESResponse

case class GetResponse[Json](_index: String, _type : String, _id : String, _version : Int, found: Boolean, _source: Json) extends ESResponse {
  def as[Document](implicit reads: Reader[Json, Document]): Document = reads.read(_source)
}

case class Hit[Json](_index: String, _type: String, _id: String, _score: Float, _source: Json) extends ESResponse {
  def as[Document](implicit reads: Reader[Json, Document]): Document = reads.read(_source)
}

case class Hits[Json](total: Int, max_score: Option[Float], hits: Seq[Hit[Json]]) extends ESResponse

case class SearchResponse[Json](took: Int, _shards: Shards[Json], timed_out: Boolean, hits: Hits[Json], scroll_id: Option[String], aggregations: Option[Json] = None) extends ESResponse {
  def hitsAs[Document](implicit reads: Reader[Json, Document]) = hits.hits.map(h => h.as[Document](reads))
}

case class BulkResult[Json](_index: String, _type: String, _id: String, _version: Int, _shards: Shards[Json]) extends ESResponse

case class BulkItem[Json](index: Option[BulkResult[Json]], delete: Option[BulkResult[Json]], update: Option[BulkResult[Json]], create:Option[BulkResult[Json]]) extends ESResponse

case class BulkResponse[Json](took: Int, errors: Boolean, items: Seq[BulkItem[Json]]) extends ESResponse

