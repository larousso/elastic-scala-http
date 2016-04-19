package com.adelegue.elastic.client.codec

import com.adelegue.elastic.client.api._

/**
  * Created by adelegue on 12/04/2016.
  */
object PlayJson {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val shardsReads: Reads[Shards[JsValue]] = (
    (__ \ "total").read[Int] and
      (__ \ "failed").read[Int] and
      (__ \ "successful").read[Int] and
      (__ \ "failures").read[Seq[JsValue]].orElse(Reads.pure(Nil))
    ) (Shards.apply[JsValue] _)

  implicit val shardsWrites: Writes[Shards[JsValue]] = (
    (__ \ "total").write[Int] and
      (__ \ "failed").write[Int] and
      (__ \ "successful").write[Int] and
      (__ \ "failures").write[Seq[JsValue]]
    ) (unlift(Shards.unapply[JsValue]))

  private implicit val indexOpsFormat = Json.format[IndexOps]

  private implicit val hitFormat: Format[Hit[JsValue]] =
    ((__ \ "_index").format[String] ~
      (__ \ "_type").format[String] ~
      (__ \ "_id").format[String] ~
      (__ \ "_score").format[Float] ~
      (__ \ "_source").format[JsValue]
      ) (Hit.apply, unlift(Hit.unapply))

  private implicit val hitsFormat: Format[Hits[JsValue]] =
    ((__ \ "total").format[Int] ~
      (__ \ "max_score").formatNullable[Float] ~
      (__ \ "hits").format[Seq[Hit[JsValue]]]
      ) (Hits.apply, unlift(Hits.unapply))

  private implicit val searchResponseFormat: Format[SearchResponse[JsValue]] =
    ((__ \ "took").format[Int] ~
      (__ \ "_shards").format[Shards[JsValue]] ~
      (__ \ "timed_out").format[Boolean] ~
      (__ \ "hits").format[Hits[JsValue]] ~
      (__ \ "_scroll_id").formatNullable[String] ~
      (__ \ "aggregation").formatNullable[JsValue]
      ) (SearchResponse.apply, unlift(SearchResponse.unapply))

  private implicit val getResponseFormats: Format[GetResponse[JsValue]] =
    ((__ \ "_index").format[String] ~
      (__ \ "_type").format[String] ~
      (__ \ "_id").format[String] ~
      (__ \ "_version").format[Int] ~
      (__ \ "found").format[Boolean] ~
      (__ \ "_source").format[JsValue]
      ) (GetResponse.apply, unlift(GetResponse.unapply))

  private implicit val mGetFormat = Json.format[MGet]

  private implicit val mGetsFormat = Json.format[MGets]

  private implicit val mGetResponseFormat = Reads[MGetResponse[JsValue]] {
    case json: JsObject => JsSuccess(MGetResponse( (json \ "docs").as[Seq[GetResponse[JsValue]]]) )
    case _ => JsError("wrong type")
  }

  private implicit val indexResponseFormat: Format[IndexResponse[JsValue]] =
    ((__ \ "_shards").format[Shards[JsValue]] ~
      (__ \ "_index").formatNullable[String] ~
      (__ \ "_type").formatNullable[String] ~
      (__ \ "_id").formatNullable[String] ~
      (__ \ "_version").formatNullable[Int] ~
      (__ \ "created").formatNullable[Boolean] ~
      (__ \ "found").formatNullable[Boolean]
      ) (IndexResponse.apply, unlift(IndexResponse.unapply))

  private implicit val bulkResultFormat: Format[BulkResult[JsValue]] =
    ((__ \ "_index").format[String] ~
      (__ \ "_type").format[String] ~
      (__ \ "_id").format[String] ~
      (__ \ "_version").format[Int] ~
      (__ \ "_shards").format[Shards[JsValue]]
      ) (BulkResult.apply, unlift(BulkResult.unapply))

  private implicit val bulkItemFormat: Format[BulkItem[JsValue]] =
    ((__ \ "index").formatNullable[BulkResult[JsValue]] ~
      (__ \ "delete").formatNullable[BulkResult[JsValue]] ~
      (__ \ "update").formatNullable[BulkResult[JsValue]] ~
      (__ \ "create").formatNullable[BulkResult[JsValue]]
      ) (BulkItem.apply, unlift(BulkItem.unapply))

  private implicit val bulkResponseFormat: Format[BulkResponse[JsValue]] =
    ((__ \ "took").format[Int] ~
      (__ \ "errors").format[Boolean] ~
      (__ \ "items").format[Seq[BulkItem[JsValue]]]
      ) (BulkResponse.apply, unlift(BulkResponse.unapply))

  private implicit val bulkOpDetailFormat = Json.format[BulkOpDetail]

  private implicit val bulkBulkOpType = Json.format[BulkOpType]

  private implicit def bulkRequestFormat[D](implicit format: Format[D]): Format[Bulk[D]] =
    ((__ \ "operation").format[BulkOpType] ~
      (__ \ "source").formatNullable[D]
      ) (Bulk.apply, unlift(Bulk.unapply))

  private implicit val scrollFormat = Json.format[Scroll]

  implicit val strJsValueWrites = Writer[JsValue, String] { json =>
    Json.stringify(json)
  }

  implicit val strJsObjectWrites = Writer[JsObject, String] { json =>
    Json.stringify(json)
  }
  implicit val strJsValueReads = Reader[String, JsValue] { str =>
    Json.parse(str)
  }

  implicit val bulkOpTypeWrites = Writer[BulkOpType, JsValue] { b =>
    Json.toJson(b)
  }

  implicit val mgetsWrites = Writer[MGets, JsValue] { mget =>
    Json.toJson(mget)
  }

  implicit val scrollWriter = Writer[Scroll, JsValue] { scroll =>
    Json.toJson(scroll)
  }

  implicit val strJsObjectReads = Reader[String, JsObject] { str =>
    Json.parse(str).as[JsObject]
  }

  implicit val indexOpsReads = Reader[JsValue, IndexOps] { json =>
    json.as[IndexOps]
  }

  implicit val searchResponseReads = Reader[JsValue, SearchResponse[JsValue]] { json =>
    json.as[SearchResponse[JsValue]]
  }

  implicit val getResponseReads = Reader[JsValue, GetResponse[JsValue]] { json =>
    json.as[GetResponse[JsValue]]
  }

  implicit val indexResponseReads = Reader[JsValue, IndexResponse[JsValue]] { json =>
    json.as[IndexResponse[JsValue]]
  }

  implicit val bulkResultReads = Reader[JsValue, BulkResult[JsValue]] { json =>
    json.as[BulkResult[JsValue]]
  }

  implicit val bulkItemReads = Reader[JsValue, BulkItem[JsValue]] { json =>
    json.as[BulkItem[JsValue]]
  }

  implicit val bulkResponseReads = Reader[JsValue, BulkResponse[JsValue]] { json =>
    json.as[BulkResponse[JsValue]]
  }

  implicit val mGetResponseReads = Reader[JsValue, MGetResponse[JsValue]] { json =>
    json.as[MGetResponse[JsValue]]
  }

  implicit def genericStringReader[D](implicit reads: Reads[D]) = Reader[String, D] { json =>
    Json.parse(json).as[D]
  }

  implicit def genericStringWriter[D](implicit reads: Writes[D]) = Writer[D, String] { any =>
    Json.stringify(Json.toJson(any))
  }

  implicit def genericReader[D](implicit reads: Reads[D]) = Reader[JsValue, D] { json =>
    json.as[D]
  }

  implicit def genericWriter[D](implicit writes: Writes[D]) = Writer[D, JsValue] { doc =>
    Json.toJson(doc)
  }

}
