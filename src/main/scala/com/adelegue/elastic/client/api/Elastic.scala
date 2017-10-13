package com.adelegue.elastic.client.api

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
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
    * @param name   of the index
    * @param `type` of the index
    * @return an Index référence.
    */
  def index(name: String, `type`: Option[String] = None): Index[JsonR]

  /**
    * Référence to the index
    *
    * @param typeDefinition   the definiton of the index
    * @return an Index référence.
    */
  def index(typeDefinition: TypeDefinition): Index[JsonR]

  /**
    * Verify if the index exists
    *
    * @param name    of the index
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @return Boolean
    */
  def verifyIndex(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[Boolean]

  /**
    * Return the mapping of the index and type
    *
    * @param index   name of the index
    * @param `type`  name of the type
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @return the mapping as json object
    */
  def getMapping(index: String, `type`: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  def getMapping(typeDefinition: TypeDefinition)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR] =
    getMapping(typeDefinition.name, typeDefinition.`type`)(sReader, ec)
  /**
    * Return the mappings definitions
    *
    * @param index   a Seq of index names
    * @param `type`  a Seq of type names
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @return the mappings as json object
    */
  def getMappings(index: Seq[String], `type`: Seq[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * Get the index
    *
    * @param name    of the index
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @return index as json object
    */
  def getIndex(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * Index creation
    *
    * @param name       of the index
    * @param settings   of the index
    * @param mWrites    settings to string conversion
    * @param sReader    json string to json object conversion
    * @param jsonReader json to IndexOps conversion
    * @param ec         ExecutionContext for future execution
    * @tparam S settings type
    * @return IndexOps
    */
  def createIndex[S](name: String, settings: Option[S])(implicit mWrites: Writer[S, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps]

  /**
    * Index creation
    *
    * @param name       of the index
    * @param settings   of the index
    * @param mWrites    settings to string conversion
    * @param sReader    json string to json object conversion
    * @param jsonReader json to IndexOps conversion
    * @param ec         ExecutionContext for future execution
    * @tparam S settings type
    * @return IndexOps
    */
  def createIndex[S](name: String, settings: S)(implicit mWrites: Writer[S, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] =
    createIndex(name, Some(settings))(mWrites, sReader, jsonReader, ec)

  /**
    * Index creation
    *
    * @param name       of the index
    * @param mWrites    settings to string conversion
    * @param sReader    json string to json object conversion
    * @param jsonReader json to IndexOps conversion
    * @param ec         ExecutionContext for future execution
    * @tparam S settings type
    * @return IndexOps
    */
  def createIndex[S](name: String)(implicit mWrites: Writer[S, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] =
    createIndex(name, None)(mWrites, sReader, jsonReader, ec)

  /**
    * Aliases creation
    *
    * @param settings   of the aliases
    * @param mWrites    settings to string conversion
    * @param sReader    json string to json object conversion
    * @param jsonReader json to IndexOps conversion
    * @param ec         ExecutionContext for future execution
    * @tparam S alias settings type
    * @return IndexOps
    */
  def createAliases[S](settings: S)(implicit mWrites: Writer[S, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps]

  /**
    * Delete index operation
    *
    * @param name       name of the index
    * @param `type`     the name of the type
    * @param sReader    json string to json object conversion
    * @param jsonReader json to IndexOps conversion
    * @param ec         ExecutionContext for future execution
    * @return IndexOps
    */
  def deleteIndex(name: String, `type`: Option[String] = None)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps]

  /**
    * Mapping modification
    *
    * @param name             name of the index
    * @param `type`           the name of the type
    * @param mapping          the mapping to update
    * @param update_all_types to update all type if conflicts
    * @param mWrites          mapping to json string conversion
    * @param sReader          json string to json object conversion
    * @param jsonReader       json object to IndexOps conversion
    * @param ec               ExecutionContext for future execution
    * @tparam M mapping type
    * @return IndexOps
    */
  def putMapping[M](name: String, `type`: String, mapping: M, update_all_types: Boolean)(implicit mWrites: Writer[M, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps]

  /**
    * Mapping modification
    *
    * @param name             name of the index
    * @param `type`           the name of the type
    * @param mapping          the mapping to update
    * @param mWrites          mapping to json string conversion
    * @param sReader          json string to json object conversion
    * @param jsonReader       json object to IndexOps conversion
    * @param ec               ExecutionContext for future execution
    * @tparam M mapping type
    * @return IndexOps
    */
  def putMapping[M](name: String, `type`: String, mapping: M)(implicit mWrites: Writer[M, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] =
    putMapping(name, `type`, mapping, false)(mWrites, sReader, jsonReader, ec)

  /**
    *
    * @param typeDefinition
    * @param mapping          the mapping to update
    * @param update_all_types to update all type if conflicts
    * @param mWrites          mapping to json string conversion
    * @param sReader          json string to json object conversion
    * @param jsonReader       json object to IndexOps conversion
    * @param ec               ExecutionContext for future execution
    * @tparam M mapping type
    * @return IndexOps
    */
  def putMapping[M](typeDefinition: TypeDefinition, mapping: M, update_all_types: Boolean = false)(implicit mWrites: Writer[M, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps] =
    putMapping[M](typeDefinition.name, typeDefinition.`type`, mapping, update_all_types)(mWrites, sReader, jsonReader, ec)
  /**
    * Analyse of query.
    *
    * @param query   the query to analyse
    * @param qWrites query to json string conversion
    * @param jWrites Json object to json string conversion
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @tparam Q query type
    * @return Json object
    */
  def analyse[Q](index: Option[String] = None, query: Q)(implicit qWrites: Writer[Q, JsonR], jWrites: Writer[JsonR, String], sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * Add template with name
    *
    * @param name       name of the template
    * @param template   template definition
    * @param mWrites    json to string conversion
    * @param jWrites    template to json object conversion
    * @param sReader    json object to string conversion
    * @param jsonReader json object to IndexOps conversion
    * @param ec         ExecutionContext for future execution
    * @tparam T template definition type
    * @return an IndexOps object
    */
  def putTemplate[T](name: String, template: T)(implicit mWrites: Writer[T, JsonR], jWrites: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps]

  /**
    * get template
    *
    * @param name    name of the template
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @return Json representation of the template
    */
  def getTemplate(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * Template deletion
    *
    * @param name       name of the template to delete
    * @param sReader    string to json object conversion
    * @param jsonReader json object to IndexOps conversion
    * @param ec         ExecutionContext for future execution
    * @return an IndexOps object
    */
  def deleteTemplate(name: String)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps]

  /**
    * Verify if the template exists
    *
    * @param name    name of the template
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @return true if exists
    */
  def verifyTemplate(name: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[Boolean]

  /**
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-stats.html
    *
    * @param indices the names of the indices
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @return
    */
  def stats(indices: Seq[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * read segments
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-segments.html
    *
    * @param indices the names of the indices
    * @param verbose verbose response if true
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @return a json representation of the segments
    */
  def segments(indices: Seq[String], verbose: Boolean = false)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-recovery.html
    *
    * @param indices     name of the indices
    * @param detailed    detailed option
    * @param active_only active_only option
    * @param sReader     json string to json object conversion
    * @param ec          ExecutionContext for future execution
    * @return a json representation of the response
    */
  def recovery(indices: Seq[String], detailed: Boolean = false, active_only: Boolean = false)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * Shard stores operation
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-shards-stores.html
    *
    * @param indices the indices
    * @param status  the status : green, yellow, red
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @return a json object of the response
    */
  def shardStores(indices: Seq[String], status: Option[String] = None)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-clearcache.html
    *
    * @param indices the names of the indices
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @return a json object of the response
    */
  def clearCache(indices: Seq[String])(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-refresh.html
    *
    * @param indices the names of the indices
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @return a json object of the response
    */
  def refresh(indices: String*)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]]

  /**
    * Flush operation
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-flush.html
    *
    * @param indices         a [[scala.Seq]] of indices names
    * @param wait_if_ongoing If set to true the flush operation will block until the flush can be executed if another flush operation is already executing.
    * @param force           Whether a flush should be forced even if it is not necessarily needed ie. if no changes will be committed to the index
    * @param sReader         json string to json object conversion
    * @param jsonReader      json object to [[com.adelegue.elastic.client.api.IndexResponse]] conversion
    * @param ec              ExecutionContext for future execution
    * @return a [[com.adelegue.elastic.client.api.IndexResponse]]
    */
  def flush(indices: Seq[String], wait_if_ongoing: Boolean = false, force: Boolean = false)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]]

  /**
    * Force merge operation
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-forcemerge.html
    *
    * @param indices              indices
    * @param max_num_segments     The number of segments to merge to
    * @param only_expunge_deletes Should the merge process only expunge segments with deletes in it.
    * @param flush                Should a flush be performed after the forced merge
    * @param sReader              json string to json object conversion
    * @param jsonReader           json object to [[com.adelegue.elastic.client.api.IndexResponse]] conversion
    * @param ec                   ExecutionContext for future execution
    * @return a [[com.adelegue.elastic.client.api.IndexResponse]] response
    */
  def forceMerge(indices: Seq[String] = Seq.empty[String], max_num_segments: Option[Int] = None, only_expunge_deletes: Boolean = false, flush: Boolean = true)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]]

  /**
    * Upgrade operation
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-upgrade.html
    *
    * @param index   the name if the index
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @return a json objet of the response
    */
  def upgrade(index: String, only_ancient_segments: Boolean = false)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * Upgrade operation status
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-upgrade.html
    *
    * @param index   the name if the index
    * @param sReader json string to json object conversion
    * @param ec      ExecutionContext for future execution
    * @return a json objet of the response
    */
  def upgradeStatus(index: String)(implicit sReader: Reader[String, JsonR], ec: ExecutionContext): Future[JsonR]

  /**
    * Multiple get operation
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html
    *
    * @param index      the name of the index
    * @param `type`     the name of the type
    * @param request    multiple get request : [[com.adelegue.elastic.client.api.MGets]]
    * @param sWriter    json to string conversion
    * @param jsonWriter [[com.adelegue.elastic.client.api.MGets]] to json object conversion
    * @param sReader    string to json object conversion
    * @param jsonReader json object to [[com.adelegue.elastic.client.api.MGetResponse]] conversion
    * @param ec         ExecutionContext for future execution
    * @return a [[com.adelegue.elastic.client.api.MGetResponse]] response
    */
  def mget(index: Option[String] = None, `type`: Option[String] = None, request: MGets)(implicit sWriter: Writer[JsonR, String], jsonWriter: Writer[MGets, JsonR], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, MGetResponse[JsonR]], ec: ExecutionContext): Future[MGetResponse[JsonR]]

  /**
    * Search operation
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-body.html
    *
    * @param index           name of the index
    * @param `type`          name of the type
    * @param query           the query to execute
    * @param from            The starting from index of the hits to return. Defaults to 0
    * @param size            The number of hits to return. Defaults to 10
    * @param search_type     The type of the search operation to perform
    * @param request_cache   Set to true or false to enable or disable the caching of search results for requests
    * @param terminate_after The maximum number of documents to collect for each shard, upon reaching which the query execution will terminate early
    * @param timeout         A search timeout, bounding the search request to be executed within the specified time value and bail with the hits accumulated up to that point when expired. Defaults to no timeout.
    * @param qWrites         Query to json object conversion
    * @param sReader         string to json conversion
    * @param jsonReader      json string to json object conversion
    * @param ec              ExecutionContext for future execution
    * @tparam Q query object type
    * @return a [[com.adelegue.elastic.client.api.SearchResponse]]
    */
  def search[Q](index: Seq[String] = Seq.empty[String], `type`: Seq[String] = Seq.empty[String], query: Q, from: Int = 0, size: Int = 20, search_type: SearchType = QUERY_THEN_FETCH, request_cache: Boolean = false, terminate_after: Option[Int] = None, timeout: Option[Int] = None)(implicit qWrites: Writer[Q, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Future[SearchResponse[JsonR]]

  /**
    *
    * Aggregation operation
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html
    *
    * @param index           name of the index
    * @param `type`          name of the type
    * @param query           the query to execute
    * @param from            The starting from index of the hits to return. Defaults to 0
    * @param size            The number of hits to return. Defaults to 10
    * @param search_type     The type of the search operation to perform
    * @param request_cache   Set to true or false to enable or disable the caching of search results for requests
    * @param terminate_after The maximum number of documents to collect for each shard, upon reaching which the query execution will terminate early
    * @param timeout         A search timeout, bounding the search request to be executed within the specified time value and bail with the hits accumulated up to that point when expired. Defaults to no timeout.
    * @param qWrites         Query to json object conversion
    * @param sReader         string to json conversion
    * @param jsonReader      json string to json object conversion
    * @param ec              ExecutionContext for future execution
    * @tparam Q query object type
    * @return a [[com.adelegue.elastic.client.api.SearchResponse]]
    */
  def aggregation[Q](index: Seq[String], `type`: Seq[String], query: Q, from: Option[Int] = None, size: Option[Int] = None, search_type: Option[SearchType] = None, request_cache: Boolean = false, terminate_after: Option[Int] = None, timeout: Option[Int] = None)(implicit qWrites: Writer[Q, String], jsonReader: Reader[String, SearchResponse[JsonR]], sReader: Reader[String, JsonR], ec: ExecutionContext): Future[SearchResponse[JsonR]]

  /**
    * The scroll search return a [[org.reactivestreams.Publisher]] (see reactives streams) of result.
    * All the result of the search are returned in an asynchronous stream.
    *
    * @param index      name of the index
    * @param `type`     name of the type
    * @param query      the query to execute
    * @param qWrites    Query to json object conversion
    * @param sReader    string to json conversion
    * @param jsonReader json string to json object conversion
    * @param ec         ExecutionContext for future execution
    * @tparam Q the query object type
    * @return a [[org.reactivestreams.Publisher]] (see reactive streams) of [[com.adelegue.elastic.client.api.SearchResponse]]
    */
  def scrollPublisher[Q](index: Seq[String] = Seq.empty, `type`: Seq[String] = Seq.empty, query: Q, scroll: String = "1m", size: Int = 20)(implicit qWrites: Writer[Q, String], jsonWriter: Writer[Scroll, JsonR], sWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Publisher[SearchResponse[JsonR]]

  /**
    * The scroll search return a [[org.reactivestreams.Publisher]] (see reactives streams) of result.
    * All the result of the search are returned in an asynchronous stream.
    *
    * @param index      name of the index
    * @param `type`     name of the type
    * @param query      the query to execute
    * @param qWrites    Query to json object conversion
    * @param sReader    string to json conversion
    * @param jsonReader json string to json object conversion
    * @param ec         ExecutionContext for future execution
    * @tparam Q the query object type
    * @return a [[org.reactivestreams.Publisher]] (see reactive streams) of [[com.adelegue.elastic.client.api.SearchResponse]]
    */
  def scroll[Q](index: Seq[String] = Seq.empty, `type`: Seq[String] = Seq.empty, query: Q, scroll: String = "1m", size: Int = 20)(implicit qWrites: Writer[Q, String], jsonWriter: Writer[Scroll, JsonR], sWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Source[SearchResponse[JsonR], NotUsed]

  /**
    * Bulk operation
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html
    *
    * @param index        name of the index
    * @param `type`       name of the type
    * @param publisher    a [[org.reactivestreams.Publisher]] (see reactive streams) of [[com.adelegue.elastic.client.api.Bulk]]
    * @param batchSize    the size of the packet of bulk operations to send to elastic
    * @param sWrites      json to string conversion
    * @param docWriter    document to json object conversion
    * @param bulkOpWriter [[com.adelegue.elastic.client.api.Bulk]] to json object conversion
    * @param sReader      string to json object conversion
    * @param bReader      json object to [[com.adelegue.elastic.client.api.BulkResponse]] conversion
    * @param ec           ExecutionContext for future execution
    * @tparam D the type of the document
    * @return a [[org.reactivestreams.Publisher]] (see reactive streams) of [[com.adelegue.elastic.client.api.BulkResponse]]
    */
  def bulk[D](index: Option[String] = None, `type`: Option[String] = None, publisher: Publisher[Bulk[D]], batchSize: Int)(implicit sWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Publisher[BulkResponse[JsonR]]


  /**
    * A akka stream flow to bulk datas.
    *
    * @param index        name of the index
    * @param `type`       name of the type
    * @param batchSize    the size of the packet of bulk operations to send to elastic
    * @param sWrites      json to string conversion
    * @param docWriter    document to json object conversion
    * @param bulkOpWriter [[com.adelegue.elastic.client.api.Bulk]] to json object conversion
    * @param sReader      string to json object conversion
    * @param bReader      json object to [[com.adelegue.elastic.client.api.BulkResponse]] conversion
    * @param ec           ExecutionContext for future execution
    * @tparam D the type of the document
    * @return a [[akka.stream.scaladsl.Flow]] that ingest [[com.adelegue.elastic.client.api.Bulk]] and out [[com.adelegue.elastic.client.api.BulkResponse]]
    */
  def bulkFlow[D](index: Option[String] = None, `type`: Option[String] = None, batchSize: Int)(implicit sWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Flow[Bulk[D], BulkResponse[JsonR], NotUsed]

  /**
    * A atomic bulk opération
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html
    *
    * @param index        name of the index
    * @param `type`       name of the type
    * @param bulk         a Seq of [[com.adelegue.elastic.client.api.Bulk]] to send in bulk
    * @param sWrites      json to string conversion
    * @param docWriter    document to json object conversion
    * @param bulkOpWriter [[com.adelegue.elastic.client.api.Bulk]] to json object conversion
    * @param sReader      string to json object conversion
    * @param bReader      json object to [[com.adelegue.elastic.client.api.BulkResponse]] conversion
    * @param ec           ExecutionContext for future execution
    * @tparam D the type of the document
    * @return a [[com.adelegue.elastic.client.api.BulkResponse]]
    */
  def oneBulk[D](index: Option[String] = None, `type`: Option[String] = None, bulk: Seq[Bulk[D]])(implicit sWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Future[BulkResponse[JsonR]]

}

/**
  * Api to access index level operation.
  *
  * @tparam JsonR a representation of json.
  */
trait Index[JsonR] {

  def / (`type`: String) : Index[JsonR]

  /**
    * Index operation
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html
    *
    * @param data        document to index
    * @param id          optional id of the document
    * @param version     current version of the document
    * @param versionType type of the version
    * @param create      if true an excpetion is thrown if the document allready exists
    * @param routing     routing key
    * @param parent      parent id if there is a parent child relation
    * @param refresh     if true, the index will be refresh
    * @param timeout     a timeout
    * @param consistency consistency level ALL, QUORUM, ONE
    * @param detectNoop  detect if the version should be increase
    * @param writer      document to json object conversion
    * @param strWriter   json object to string conversion
    * @param sReader     string to json object conversion
    * @param jsonReader  json object to [[com.adelegue.elastic.client.api.IndexResponse]] conversion
    * @param ec          ExecutionContext for future execution
    * @tparam D the type of the document
    * @return a [[com.adelegue.elastic.client.api.IndexResponse]]
    */
  def index[D](data: D, id: Option[String] = None, version: Option[Int] = None, versionType: Option[VersionType] = None, create: Boolean = false, routing: Option[String] = None, parent: Option[String] = None, refresh: Boolean = false, timeout: Option[String] = None, consistency: Option[Consistency] = None, detectNoop: Boolean = false)(implicit writer: Writer[D, JsonR], strWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]]

  /**
    * Update operation : index operation with create = false and id required.
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html
    *
    * @param data        document to index
    * @param id          optional id of the document
    * @param version     current version of the document
    * @param versionType type of the version
    * @param routing     routing key
    * @param parent      parent id if there is a parent child relation
    * @param refresh     if true, the index will be refresh
    * @param timeout     a timeout
    * @param consistency consistency level ALL, QUORUM, ONE
    * @param detectNoop  detect if the version should be increase
    * @param writer      document to json object conversion
    * @param strWriter   json object to string conversion
    * @param sReader     string to json object conversion
    * @param jsonReader  json object to [[com.adelegue.elastic.client.api.IndexResponse]] conversion
    * @param ec          ExecutionContext for future execution
    * @tparam D the type of the document
    * @return a [[com.adelegue.elastic.client.api.IndexResponse]]
    */
  def update[D](data: D, id: String, version: Option[Int] = None, versionType: Option[VersionType] = None, routing: Option[String] = None, parent: Option[String] = None, refresh: Boolean = false, timeout: Option[String] = None, consistency: Option[Consistency] = None, detectNoop: Boolean = false)(implicit writer: Writer[D, JsonR], strWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]] =
    index(data, Some(id), version = version, versionType = versionType, create = false, routing = routing, parent = parent, refresh = refresh, timeout = timeout, consistency = consistency, detectNoop = detectNoop)

  /**
    * Create operation : index operation with create = true.
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-index_.html
    *
    * @param data        document to index
    * @param id          optional id of the document
    * @param version     current version of the document
    * @param versionType type of the version
    * @param routing     routing key
    * @param parent      parent id if there is a parent child relation
    * @param refresh     if true, the index will be refresh
    * @param timeout     a timeout
    * @param consistency consistency level ALL, QUORUM, ONE
    * @param detectNoop  detect if the version should be increase
    * @param writer      document to json object conversion
    * @param strWriter   json object to string conversion
    * @param sReader     string to json object conversion
    * @param jsonReader  json object to [[com.adelegue.elastic.client.api.IndexResponse]] conversion
    * @param ec          ExecutionContext for future execution
    * @tparam D the type of the document
    * @return a [[com.adelegue.elastic.client.api.IndexResponse]]
    */
  def create[D](data: D, id: Option[String], version: Option[Int] = None, versionType: Option[VersionType] = None, routing: Option[String] = None, parent: Option[String] = None, refresh: Boolean = false, timeout: Option[String] = None, consistency: Option[Consistency] = None, detectNoop: Boolean = false)(implicit writer: Writer[D, JsonR], strWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]] =
    index(data, id, version = version, versionType = versionType, create = true, routing = routing, parent = parent, refresh = refresh, timeout = timeout, consistency = consistency, detectNoop = false)

  /**
    * Get a document by id
    *
    * @param id         id of the document
    * @param routing    the routing key
    * @param fields     a [[scala.Seq]] of fields to filter the source
    * @param _source    if false, the source is ignored
    * @param sReader    string to json object conversion
    * @param jsonReader json object to [[com.adelegue.elastic.client.api.GetResponse]] conversion
    * @param ec         ExecutionContext for future execution
    * @return a [[com.adelegue.elastic.client.api.GetResponse]]
    */
  def get(id: String, routing: Option[String] = None, fields: Seq[String] = Seq(), _source: Boolean = true)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, GetResponse[JsonR]], ec: ExecutionContext): Future[GetResponse[JsonR]]

  /**
    * Delete index operation
    *
    * @param sReader    string to json object conversion
    * @param jsonReader a json object to [[com.adelegue.elastic.client.api.IndexOps]] conversion
    * @param ec         ExecutionContext for future execution
    * @return
    */
  def delete(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexOps], ec: ExecutionContext): Future[IndexOps]

  /**
    * Delete a document by its id.
    *
    * @param id         of the document
    * @param sReader    string to json object conversion
    * @param jsonReader json object to [[com.adelegue.elastic.client.api.IndexResponse]] conversion
    * @param ec         ExecutionContext for future execution
    * @return a [[com.adelegue.elastic.client.api.IndexResponse]] object
    */
  def delete(id: String)(implicit sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, IndexResponse[JsonR]], ec: ExecutionContext): Future[IndexResponse[JsonR]]

  /**
    * Multiple get operation
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-multi-get.html
    *
    * @param request    multiple get request : [[com.adelegue.elastic.client.api.MGets]]
    * @param sWriter    json to string conversion
    * @param jsonWriter [[com.adelegue.elastic.client.api.MGets]] to json object conversion
    * @param sReader    string to json object conversion
    * @param jsonReader json object to [[com.adelegue.elastic.client.api.MGetResponse]] conversion
    * @param ec         ExecutionContext for future execution
    * @return a [[com.adelegue.elastic.client.api.MGetResponse]] response
    */
  def mget(request: MGets)(implicit sWriter: Writer[JsonR, String], jsonWriter: Writer[MGets, JsonR], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, MGetResponse[JsonR]], ec: ExecutionContext): Future[MGetResponse[JsonR]]

  /**
    * Search operation on this index.
    *
    * @param query      the search query object
    * @param qWrites    query to json object conversion
    * @param sReads     string to json object conversion
    * @param jsonReader json object to [[com.adelegue.elastic.client.api.SearchResponse]] conversion
    * @param ec         ExecutionContext for future execution
    * @tparam Q type of the query object
    * @return a [[com.adelegue.elastic.client.api.SearchResponse]]
    */
  def search[Q](query: Q)(implicit qWrites: Writer[Q, String], sReads: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Future[SearchResponse[JsonR]]

  /**
    * Bulk operation for the current index
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html
    *
    * @param publisher    a [[org.reactivestreams.Publisher]] (see reactive streams) of [[com.adelegue.elastic.client.api.Bulk]]
    * @param batchSize    the size of the packet of bulk operations to send to elastic
    * @param sWrites      json to string conversion
    * @param docWriter    document to json object conversion
    * @param bulkOpWriter [[com.adelegue.elastic.client.api.Bulk]] to json object conversion
    * @param sReader      string to json object conversion
    * @param bReader      json object to [[com.adelegue.elastic.client.api.BulkResponse]] conversion
    * @param ec           ExecutionContext for future execution
    * @tparam D the type of the document
    * @return a [[org.reactivestreams.Publisher]] (see reactive streams) of [[com.adelegue.elastic.client.api.BulkResponse]]
    */
  def bulk[D](publisher: Publisher[Bulk[D]], batchSize: Int)(implicit sWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Publisher[BulkResponse[JsonR]]



  /**
    * A akka stream flow to bulk datas.
    *
    * @param batchSize    the size of the packet of bulk operations to send to elastic
    * @param sWrites      json to string conversion
    * @param docWriter    document to json object conversion
    * @param bulkOpWriter [[com.adelegue.elastic.client.api.Bulk]] to json object conversion
    * @param sReader      string to json object conversion
    * @param bReader      json object to [[com.adelegue.elastic.client.api.BulkResponse]] conversion
    * @param ec           ExecutionContext for future execution
    * @tparam D the type of the document
    * @return a [[akka.stream.scaladsl.Flow]] that ingest [[com.adelegue.elastic.client.api.Bulk]] and out [[com.adelegue.elastic.client.api.BulkResponse]]
    */
  def bulkFlow[D](batchSize: Int)(implicit sWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Flow[Bulk[D], BulkResponse[JsonR], NotUsed]

  /**
    * A atomic bulk opération
    * see https://www.elastic.co/guide/en/elasticsearch/reference/current/docs-bulk.html
    *
    * @param bulk         a Seq of [[com.adelegue.elastic.client.api.Bulk]] to send in bulk
    * @param sWrites      json to string conversion
    * @param docWriter    document to json object conversion
    * @param bulkOpWriter [[com.adelegue.elastic.client.api.Bulk]] to json object conversion
    * @param sReader      string to json object conversion
    * @param bReader      json object to [[com.adelegue.elastic.client.api.BulkResponse]] conversion
    * @param ec           ExecutionContext for future execution
    * @tparam D the type of the document
    * @return a [[com.adelegue.elastic.client.api.BulkResponse]]
    */
  def oneBulk[D](bulk: Seq[Bulk[D]])(implicit sWrites: Writer[JsonR, String], docWriter: Writer[D, JsonR], bulkOpWriter: Writer[BulkOpType, JsonR], sReader: Reader[String, JsonR], bReader: Reader[JsonR, BulkResponse[JsonR]], ec: ExecutionContext): Future[BulkResponse[JsonR]]


  /**
    * The scroll search return a [[org.reactivestreams.Publisher]] (see reactives streams) of result.
    * All the result of the search are returned in an asynchronous stream.
    *
    * @param query      the query to execute
    * @param qWrites    Query to json object conversion
    * @param sReader    string to json conversion
    * @param jsonReader json string to json object conversion
    * @param ec         ExecutionContext for future execution
    * @tparam Q the query object type
    * @return a [[org.reactivestreams.Publisher]] (see reactive streams) of [[com.adelegue.elastic.client.api.SearchResponse]]
    */
  def scrollPublisher[Q](query: Q, scroll: String = "1m", size: Int = 20)(implicit qWrites: Writer[Q, String], jsonWriter: Writer[Scroll, JsonR], sWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Publisher[SearchResponse[JsonR]]


  /**
    * The scroll search return a [[org.reactivestreams.Publisher]] (see reactives streams) of result.
    * All the result of the search are returned in an asynchronous stream.
    *
    * @param query      the query to execute
    * @param qWrites    Query to json object conversion
    * @param sReader    string to json conversion
    * @param jsonReader json string to json object conversion
    * @param ec         ExecutionContext for future execution
    * @tparam Q the query object type
    * @return a [[org.reactivestreams.Publisher]] (see reactive streams) of [[com.adelegue.elastic.client.api.SearchResponse]]
    */
  def scroll[Q](query: Q, scroll: String = "1m", size: Int = 20)(implicit qWrites: Writer[Q, String], jsonWriter: Writer[Scroll, JsonR], sWriter: Writer[JsonR, String], sReader: Reader[String, JsonR], jsonReader: Reader[JsonR, SearchResponse[JsonR]], ec: ExecutionContext): Source[SearchResponse[JsonR], NotUsed]

}

case class TypeDefinition(name: String, `type`: String)

sealed trait Consistency {
  def value: String
}

case object ALL extends Consistency {
  val value = "all"
}

case object ONE extends Consistency {
  val value = "one"
}

case object QUORUM extends Consistency {
  val value = "quorum"
}

sealed trait VersionType {
  def value: String
}

case object INTERNAL extends VersionType {
  val value = "internal"
}

case object EXTERNAL extends VersionType {
  val value = "external"
}

case object EXTERNAL_GTE extends VersionType {
  val value = "external_gte"
}

case object FORCE extends VersionType {
  val value = "force"
}

sealed abstract class SearchType {
  def value: String
}

case object DFS_QUERY_THEN_FETCH extends SearchType {
  val value = "dfs_query_then_fetch"
}

case object QUERY_THEN_FETCH extends SearchType {
  val value = "query_then_fetch"
}

/**
  * Conversion from In to Out
  *
  * @tparam In
  * @tparam Out
  */
trait Reader[In, Out] {
  def read(source: In): Out
}

/**
  * Helper to create [[com.adelegue.elastic.client.api.Reader]] from function
  */
object Reader {
  def apply[In, Out](fn: In => Out) = new Reader[In, Out] {
    override def read(source: In): Out = fn(source)
  }
}

/**
  * Conversion from In to Out
  *
  * @tparam In
  * @tparam Out
  */
trait Writer[In, Out] {
  def write(source: In): Out
}

/**
  * Helper to create [[com.adelegue.elastic.client.api.Writer]] from function
  */
object Writer {
  def apply[In, Out](fn: In => Out) = new Writer[In, Out] {
    override def write(source: In): Out = fn(source)
  }
}

/**
  * Elastic errors representation
  *
  * @param json     the error as json object
  * @param httpCode http code return by elastic.
  * @param message  raw json as string
  * @tparam Json an json representation.
  */
class EsException[Json](val json: Json, val httpCode: Int, message: String) extends RuntimeException(message)

/**
  * trait for request
  */
trait ESRequest

/**
  * Bulk operation. Params are optional depending the bulk operation needed.
  *
  * @param _index the name of the index
  * @param _type  the name of the type
  * @param _id    the id of the document
  */
case class BulkOpDetail(_index: Option[String], _type: Option[String], _id: Option[String]) extends ESRequest

/**
  * Type of the bulk operation
  *
  * @param index  for index operation
  * @param delete for delete operation
  * @param create for create operation
  * @param update for update operation
  */
case class BulkOpType(index: Option[BulkOpDetail] = None, delete: Option[BulkOpDetail] = None, create: Option[BulkOpDetail] = None, update: Option[BulkOpDetail] = None) extends ESRequest


/**
  * A bulk directive.
  *
  * @param operation the operation
  * @param source    the document to index if needed
  * @tparam D the type of the document.
  */
case class Bulk[D](operation: BulkOpType, source: Option[D]) extends ESRequest

object Bulk {
  def index[D](index: String, `type`: String, id: String, source: D) = Bulk(BulkOpType(index = Some(BulkOpDetail(Some(index), Some(`type`), Some(id)))), Some(source))
  def index[D](index: String, `type`: String, source: D) = Bulk(BulkOpType(index = Some(BulkOpDetail(Some(index), Some(`type`), None))), Some(source))
  def index[D](id: String, source: D) = Bulk(BulkOpType(index = Some(BulkOpDetail(None, None, Some(id)))), Some(source))
  def index[D](source: D) = Bulk(BulkOpType(index = Some(BulkOpDetail(None, None, None))), Some(source))
  def update[D](index: String, `type`: String, id: String, source: D) = Bulk(BulkOpType(update = Some(BulkOpDetail(Some(index), Some(`type`), Some(id)))), Some(source))
  def update[D](index: String, `type`: String, source: D) = Bulk(BulkOpType(update = Some(BulkOpDetail(Some(index), Some(`type`), None))), Some(source))
  def update[D](id: String, source: D) = Bulk(BulkOpType(update = Some(BulkOpDetail(None, None, Some(id)))), Some(source))
  def update[D](source: D) = Bulk(BulkOpType(update = Some(BulkOpDetail(None, None, None))), Some(source))
  def create[D](index: String, `type`: String, id: String, source: D) = Bulk(BulkOpType(create = Some(BulkOpDetail(Some(index), Some(`type`), Some(id)))), Some(source))
  def create[D](index: String, `type`: String, source: D) = Bulk(BulkOpType(create = Some(BulkOpDetail(Some(index), Some(`type`), None))), Some(source))
  def create[D](id: String, source: D) = Bulk(BulkOpType(create = Some(BulkOpDetail(None, None, Some(id)))), Some(source))
  def create[D](source: D) = Bulk(BulkOpType(create = Some(BulkOpDetail(None, None, None))), Some(source))
  def delete(index: String, `type`: String, id: String) = Bulk(BulkOpType(delete = Some(BulkOpDetail(Some(index), Some(`type`), Some(id)))), None)
  def delete(id: String) = Bulk(BulkOpType(delete = Some(BulkOpDetail(None, None, Some(id)))), None)
}

/**
  * An scroll request.
  *
  * @param scroll    scroll context
  * @param scroll_id scroll id
  */
case class Scroll(scroll: String, scroll_id: String) extends ESRequest

/**
  * MGet request detail
  *
  * @param _index the name of the index
  * @param _type  the name of the type
  * @param _id    the id of the document
  */
case class MGet(_index: Option[String], _type: Option[String], _id: String) extends ESRequest

/**
  * MGet request.
  *
  * @param docs
  */
case class MGets(docs: MGet*) extends ESRequest

/**
  * trait for elastic responses
  */
trait ESResponse

/**
  * Response for operations on the index
  *
  * @param acknowledged true if ok
  */
case class IndexOps(acknowledged: Boolean) extends ESResponse

/**
  * A shards state.
  *
  * @param total      total of shards
  * @param failed     total of failed shards
  * @param successful total of successful shards
  * @param failures   a [[scala.Seq]] of errors as [[Json]] object representation
  * @tparam Json the type of the json representation.
  */
case class Shards[Json](total: Int, failed: Int, successful: Int, failures: Seq[Json]) extends ESResponse

/**
  * Document indexation response
  *
  * @param _shards  state of the shards
  * @param _index   the index name
  * @param _type    the type name
  * @param _id      the id of the document
  * @param _version the version of the document
  * @param created  true if created
  * @param found    true if found
  * @tparam Json the type of the json representation
  */
case class IndexResponse[Json](_shards: Shards[Json], _index: Option[String] = None, _type: Option[String] = None, _id: Option[String] = None, _version: Option[Int] = None, created: Option[Boolean] = None, found: Option[Boolean] = None) extends ESResponse

/**
  * Get response.
  *
  * @param _index   the index name
  * @param _type    the type name
  * @param _id      the id of the document
  * @param _version the version of the document
  * @param found    true if found
  * @param _source  the document as [[Json]]
  * @tparam Json the type of the json representation
  */
case class GetResponse[Json](_index: String, _type: String, _id: String, _version: Int, found: Boolean, _source: Json) extends ESResponse {
  def as[Document](implicit reads: Reader[Json, Document]): Document = reads.read(_source)
}

/**
  * MGet response
  *
  * @param docs a [[scala.Seq]] of get response
  * @tparam Json type of json representation
  */
case class MGetResponse[Json](docs: Seq[GetResponse[Json]]) extends ESResponse {
  def docsAs[D](implicit reader: Reader[Json, D]): Seq[D] = docs.map(_._source).map(reader.read)
}

/**
  *
  * @param _index
  * @param _type
  * @param _id
  * @param _score
  * @param _source
  * @tparam Json
  */
case class Hit[Json](_index: String, _type: String, _id: String, _score: Float, _source: Json) extends ESResponse {
  def as[Document](implicit reads: Reader[Json, Document]): Document = reads.read(_source)
}

/**
  *
  * @param total
  * @param max_score
  * @param hits
  * @tparam Json
  */
case class Hits[Json](total: Int, max_score: Option[Float], hits: Seq[Hit[Json]]) extends ESResponse

/**
  *
  * @param took
  * @param _shards
  * @param timed_out
  * @param hits
  * @param scroll_id
  * @param aggregations
  * @tparam Json
  */
case class SearchResponse[Json](took: Int, _shards: Shards[Json], timed_out: Boolean, hits: Hits[Json], scroll_id: Option[String], aggregations: Option[Json] = None) extends ESResponse {
  def hitsAs[Document](implicit reads: Reader[Json, Document]) = hits.hits.map(h => h.as[Document](reads))
}

/**
  *
  * @param _index
  * @param _type
  * @param _id
  * @param _version
  * @param _shards
  * @tparam Json
  */
case class BulkResult[Json](_index: String, _type: String, _id: String, _version: Option[Int], _shards: Option[Shards[Json]], status: Int, error: Option[Json]) extends ESResponse

/**
  *
  * @param index
  * @param delete
  * @param update
  * @param create
  * @tparam Json
  */
case class BulkItem[Json](index: Option[BulkResult[Json]], delete: Option[BulkResult[Json]], update: Option[BulkResult[Json]], create: Option[BulkResult[Json]]) extends ESResponse

/**
  *
  * @param took
  * @param errors
  * @param items
  * @tparam Json
  */
case class BulkResponse[Json](took: Int, errors: Boolean, items: Seq[BulkItem[Json]]) extends ESResponse
