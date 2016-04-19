package com.adelegue.elastic.client.akka

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import org.reactivestreams.Publisher
import org.specs2.matcher.JsonMatchers
import org.specs2.mutable._
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


/**
  * Created by adelegue on 12/04/2016.
  */
class ElasticClientSpec extends Specification with JsonMatchers {

  sequential

  val server = new ElasticEmbeddedServer
  val testDuration = Duration(3, "second")


  case class MonDocument(name: String)

  case class Parent(name: String)

  case class Child(name: String)

  implicit val docFormat = Json.format[MonDocument]
  implicit val parentFormat = Json.format[Parent]
  implicit val childFormat = Json.format[Child]

  step {
    server.run()
  }

  "Client" should {

    import com.adelegue.elastic.client.api._
    import com.adelegue.elastic.client.codec.PlayJson._

    implicit val actorSystem = ActorSystem()
    implicit val mat = ActorMaterializer()
    //val client = ElasticClient[JsValue](port = 10901)
    def await[T](f: Future[T], duration: Duration = testDuration) = Await.result(f, duration)

    val client: ElasticClient[JsValue] = ElasticClientBuilder()
      .withHost("localhost")
      .withPort(10901)
      .withActorSystem(actorSystem)
      .withActorMaterializer(mat)
      .build()

    "verify index" in {
      await(client.verifyIndex("test")) mustEqual false
    }

    "creating empty index and deleting" in {
      await(client.createIndex("test", Json.obj())) mustEqual IndexOps(true)
      await(client.verifyIndex("test")) mustEqual true
      await(client.deleteIndex("test")) mustEqual IndexOps(true)
      await(client.verifyIndex("test")) mustEqual false
    }

    "creating index with settings, reading it and deleting it" in {
      val jsonSettings =
        """
          {
            "settings" : { "number_of_shards" : 1 },
            "mappings" : {
              "type1" : {
                "properties" : {
                  "field1" : { "type" : "string", "index" : "not_analyzed" }
                }
              }
            }
          }
        """
      await(client.createIndex("test", Json.parse(jsonSettings))) mustEqual IndexOps(true)
      await(client.verifyIndex("test")) mustEqual true
      await(client.getIndex("test").map(Json.stringify)) must
        /("test") / "settings" / "index" / ("number_of_shards" -> 1) and
        /("test") / "mappings" / "type1" / "properties" / "field1" / ("type" -> "string") and
        /("test") / "mappings" / "type1" / "properties" / "field1" / ("index" -> "not_analyzed")
      await(client.deleteIndex("test")) mustEqual IndexOps(true)
      await(client.verifyIndex("test")) mustEqual false
    }

    "reading unknow index" in {
      await(client.getIndex("test").map(Json.stringify)) must throwA[EsException[JsValue]].like {
        case e: EsException[JsValue] =>
          e.httpCode mustEqual 404
          e.getMessage must /("error") / ("type" -> "index_not_found_exception")
      }
    }

    "creating index and get mapping" in {
      val jsonSettings =
        """
          {
            "settings" : { "number_of_shards" : 1 },
            "mappings" : {
              "type1" : {
                "properties" : {
                  "field1" : { "type" : "string", "index" : "not_analyzed" }
                }
              }
            }
          }
        """
      await(client.createIndex("test", Json.parse(jsonSettings))) mustEqual IndexOps(true)
      await(client.getMapping("test", "type1")) mustEqual Json.parse(
        """
        {
          "test" : {
            "mappings" : {
              "type1" : {
                "properties" : {
                  "field1" : { "type" : "string", "index" : "not_analyzed" }
                }
              }
            }
          }
        } """)
      await(client.deleteIndex("test")) mustEqual IndexOps(true)
      await(client.verifyIndex("test")) mustEqual false
    }

    "creating index and get mappings" in {
      val jsonSettings =
        """
          {
            "mappings" : {
              "type1" : {
                "properties" : {
                  "field1" : { "type" : "string", "index" : "not_analyzed" }
                }
              },
              "type2" : {
                "properties" : {
                  "field1" : { "type" : "string", "index" : "not_analyzed" }
                }
              }
            }
          }
        """
      await(client.createIndex("test1", Json.parse(jsonSettings))) mustEqual IndexOps(true)
      await(client.createIndex("test2", Json.parse(jsonSettings))) mustEqual IndexOps(true)
      await(client.getMappings(Seq("test1", "test2"), Seq("type1", "type2"))) mustEqual Json.parse(
        """
      {
        "test2": {
          "mappings": {
            "type2": {
              "properties": {
                "field1": {
                  "type": "string",
                  "index": "not_analyzed"
                }
              }
            },
            "type1": {
              "properties": {
                "field1": {
                  "type": "string",
                  "index": "not_analyzed"
                }
              }
            }
          }
        },
        "test1": {
          "mappings": {
            "type2": {
              "properties": {
                "field1": {
                  "type": "string",
                  "index": "not_analyzed"
                }
              }
            },
            "type1": {
              "properties": {
                "field1": {
                  "type": "string",
                  "index": "not_analyzed"
                }
              }
            }
          }
        }
      }
        """)
      await(client.deleteIndex("test1")) mustEqual IndexOps(true)
      await(client.verifyIndex("test1")) mustEqual false
      await(client.deleteIndex("test2")) mustEqual IndexOps(true)
      await(client.verifyIndex("test2")) mustEqual false
    }

    "creating index and updating mapping" in {
      val jsonSettings =
        """
          {
            "mappings": {
              "tweet": {
                "properties": {
                  "message": { "type": "string" }
                }
              }
            }
          }
        """
      await(client.createIndex("twitter", Json.parse(jsonSettings))) mustEqual IndexOps(true)
      val mappingUpdate =
        """
        {
          "properties": {
            "user_name": { "type": "string" }
          }
        }
        """
      await(client.putMapping("twitter", "tweet", Json.parse(mappingUpdate))) mustEqual IndexOps(true)
      await(client.getMapping("twitter", "tweet")) mustEqual Json.parse(
        """
        {
          "twitter" : {
            "mappings" : {
              "tweet" : {
                "properties" : {
                  "message": { "type": "string" },
                  "user_name": { "type": "string" }
                }
              }
            }
          }
        } """)
      await(client.deleteIndex("twitter")) mustEqual IndexOps(true)
      await(client.verifyIndex("twitter")) mustEqual false
    }

    "creating index and updating mappings with update_all_types" in {
      val jsonSettings =
        """
          |{
          |  "mappings": {
          |    "type_one": {
          |      "properties": {
          |        "text": {
          |          "type": "string",
          |          "analyzer": "standard"
          |        }
          |      }
          |    },
          |    "type_two": {
          |      "properties": {
          |        "text": {
          |          "type": "string",
          |          "analyzer": "standard"
          |        }
          |      }
          |    }
          |  }
          |}
        """.stripMargin
      await(client.createIndex("my_index", Json.parse(jsonSettings))) mustEqual IndexOps(true)
      val mappingUpdate =
        """
          |{
          |  "properties": {
          |    "text": {
          |      "type": "string",
          |      "analyzer": "standard",
          |      "search_analyzer": "whitespace"
          |    }
          |  }
          |}
        """.stripMargin
      await(client.putMapping("my_index", "type_one", Json.parse(mappingUpdate))) must throwA[EsException[JsValue]]
      await(client.putMapping("my_index", "type_one", Json.parse(mappingUpdate), update_all_types = true)) mustEqual IndexOps(true)
      await(client.getMapping("my_index", "type_one")) mustEqual Json.parse(
        """
        {
          "my_index" : {
            "mappings" : {
              "type_one" : {
                "properties" : {
                  "text": {
                    "type": "string",
                    "analyzer": "standard",
                    "search_analyzer": "whitespace"
                  }
                }
              }
            }
          }
        } """)
      await(client.deleteIndex("my_index")) mustEqual IndexOps(true)
      await(client.verifyIndex("my_index")) mustEqual false
    }

    "creating index and aliases and get mapping" in {
      val jsonSettings =
        """
          {
            "mappings" : {
              "type1" : {
                "properties" : {
                  "field1" : { "type" : "string", "index" : "not_analyzed" }
                }
              }
            }
          }
        """
      await(client.createIndex("test", Json.parse(jsonSettings))) mustEqual IndexOps(true)

      val aliases =
        """
          |{
          |    "actions" : [
          |        { "add" : { "index" : "test", "alias" : "alias" } }
          |    ]
          |}
        """.stripMargin
      await(client.createAliases(Json.parse(aliases))) mustEqual IndexOps(true)
      await(client.getIndex("alias")) must beLike {
        case json: JsObject =>
          (json \ "test" \ "aliases" \ "alias").toOption must beSome(Json.obj())
      }

      await(client.deleteIndex("test")) mustEqual IndexOps(true)
      await(client.verifyIndex("test")) mustEqual false
    }


    "indexing document and reading it and deleting it" in {

      val index: Index[JsValue] = client.index("test", Some("type"))

      val document = MonDocument("nom")
      await(index.index(document, Some("id"))) must beLike[IndexResponse[JsValue]] {
        case indexResponse =>
          indexResponse._index must beSome("test")
      }

      await(index.get("id").map(_.as[MonDocument])) mustEqual document

      await(index.delete("id")) must beLike[IndexResponse[JsValue]] {
        case d =>
          d.found must beSome(true)
          d._id must beSome("id")
      }

      await(index.get("id").map(_.as[MonDocument])) must throwA[EsException[JsValue]].like {
        case e: EsException[JsValue] =>
          e.httpCode mustEqual 404
      }

      await(client.deleteIndex("test")) mustEqual IndexOps(true)
      await(client.verifyIndex("test")) mustEqual false
    }

    "indexing twice a document with create option" in {

      val index: Index[JsValue] = client.index("test", Some("type"))

      val document = MonDocument("nom")
      await(index.index(document, Some("id"))) must beLike[IndexResponse[JsValue]] {
        case indexResponse =>
          indexResponse._index must beSome("test")
      }

      await(index.create(document, Some("id"))) must throwA[EsException[JsValue]].like {
        case e: EsException[JsValue] =>
          e.httpCode mustEqual 409
          e.getMessage must /("error") / ("type" -> "document_already_exists_exception")
      }

      await(client.deleteIndex("test")) mustEqual IndexOps(true)
      await(client.verifyIndex("test")) mustEqual false
    }

    "indexing document with version and reading it" in {

      val index: Index[JsValue] = client.index("test", Some("type"))

      val document = MonDocument("nom")
      await(index.index(document, Some("id"))) must beLike[IndexResponse[JsValue]] {
        case indexResponse =>
          indexResponse._index must beSome("test")
      }
      await(index.get("id")) mustEqual GetResponse("test", "type", "id", 1, true, Json.toJson(document))

      await(index.index(document, Some("id"), version = Some(1))) must beLike {
        case indexResponse: IndexResponse[JsValue] =>
          indexResponse._index must beSome("test")
      }
      await(index.get("id")) mustEqual GetResponse("test", "type", "id", 2, true, Json.toJson(document))

      await(client.deleteIndex("test")) mustEqual IndexOps(true)
      await(client.verifyIndex("test")) mustEqual false
    }

    "indexing document with parent and reading it" in {
      val jsonSettings =
        """
          {
            "mappings" : {
              "parent" : {},
              "child": {
                "_parent": {"type": "parent"}
              }
            }
          }
        """
      await(client.createIndex("parentchild", Json.parse(jsonSettings))) mustEqual IndexOps(true)

      val parent: Index[JsValue] = client.index("parentchild", Some("parent"))
      val child: Index[JsValue] = client.index("parentchild", Some("child"))

      val parentObj = Parent("I'am the parent")
      await(parent.index(parentObj, Some("id1"))) must beLike[IndexResponse[JsValue]] {
        case indexResponse =>
          indexResponse._index must beSome("parentchild")
          indexResponse._type must beSome("parent")
      }
      await(parent.get("id1")) mustEqual GetResponse("parentchild", "parent", "id1", 1, true, Json.toJson(parentObj))

      val childObj = Child("I'am the child")
      await(child.index(childObj, Some("id2"), parent = Some("id1"))) must beLike[IndexResponse[JsValue]] {
        case indexResponse =>
          indexResponse._index must beSome("parentchild")
          indexResponse._type must beSome("child")
      }
      await(child.get("id2", routing = Some("id1"))) mustEqual GetResponse("parentchild", "child", "id2", 1, true, Json.toJson(childObj))

      await(client.deleteIndex("parentchild")) mustEqual IndexOps(true)
      await(client.verifyIndex("parentchild")) mustEqual false
    }

    "indexing document with refresh option and searching it" in {
      val jsonSettings =
        """
          {
            "settings" : { "number_of_shards" : 1 },
            "mappings" : {
              "type" : {
                "properties" : {
                  "name" : { "type" : "string", "index" : "not_analyzed" }
                }
              }
            }
          }
        """
      await(client.createIndex("test", Json.parse(jsonSettings))) mustEqual IndexOps(true)
      await(client.verifyIndex("test")) mustEqual true

      val index: Index[JsValue] = client.index("test", Some("type"))

      val document = MonDocument("nom")
      await(index.index(document, Some("id"))) must beLike[IndexResponse[JsValue]] {
        case indexResponse =>
          indexResponse._index must beSome("test")
      }

      await(index.search(Json.obj("query" -> Json.obj("term" -> Json.obj("name" -> "nom"))))) must beLike[SearchResponse[JsValue]] {
        case r => r.hits.total mustEqual 0
      }

      await(index.index(document, Some("id"), refresh = true)) must beLike[IndexResponse[JsValue]] {
        case indexResponse =>
          indexResponse._index must beSome("test")
      }

      await(index.search(Json.obj("query" -> Json.obj("term" -> Json.obj("name" -> "nom"))))) must beLike[SearchResponse[JsValue]] {
        case r =>
          r.hits.total mustEqual 1
          r.hitsAs[MonDocument] mustEqual Seq(document)
      }

      await(client.deleteIndex("test")) mustEqual IndexOps(true)
      await(client.verifyIndex("test")) mustEqual false
    }

    "bulk operation " in {

      val ids = (1 to 105).map(i => i.toString).toList
      val publisher: Publisher[Bulk[MonDocument]] = Source(ids)
        .map(i => Bulk[MonDocument](BulkOpType(index = Some(BulkOpDetail(Some("index"), Some("type"), Some(i)))), Some(MonDocument(s"Nom $i"))))
        .runWith(Sink.asPublisher(fanout = false))

      val respPublisher: Publisher[BulkResponse[JsValue]] = client.bulk[MonDocument](publisher = publisher, batchSize = 10)

      val res: Future[List[BulkResponse[JsValue]]] = Source.fromPublisher(respPublisher).runFold(List.empty[BulkResponse[JsValue]])((acc, elt) => acc :+ elt)
      val result: List[BulkResponse[JsValue]] = await(res, Duration(1, TimeUnit.MINUTES))
      val allItems = result.flatMap(r => r.items).flatMap(i => i.index.toList).map(i => i._id)
      allItems mustEqual ids

      await(client.refresh(Seq("index"))) must beLike[IndexResponse[JsValue]] {
        case e => e._shards.failed mustEqual 0
      }

      val search: SearchResponse[JsValue] = await(client.index("index", Some("type")).search(Json.obj("query" -> Json.obj("match_all" -> Json.obj()))))
      search.hits.total mustEqual 105

      await(client.deleteIndex("index")) mustEqual IndexOps(true)
      await(client.verifyIndex("index")) mustEqual false
    }

    "scroll operation" in {

      val jsonSettings = """ { "settings" : { "number_of_shards" : 1 } } """
      await(client.createIndex("index", Json.parse(jsonSettings))) mustEqual IndexOps(true)

      val ids = (1 to 105).map(i => i.toString).toList
      val publisher: Publisher[Bulk[MonDocument]] = Source(ids)
        .map(i => Bulk[MonDocument](BulkOpType(index = Some(BulkOpDetail(Some("index"), Some("type"), Some(i)))), Some(MonDocument(s"Nom $i"))))
        .runWith(Sink.asPublisher(fanout = false))

      val respPublisher: Publisher[BulkResponse[JsValue]] = client.bulk[MonDocument](publisher = publisher, batchSize = 10)

      await(Source.fromPublisher(respPublisher)
        .runFold(List.empty[BulkResponse[JsValue]])((acc, elt) => acc :+ elt))
        .flatMap(r => r.items)
        .flatMap(i => i.index.toList).map(i => i._id) mustEqual ids

      await(client.refresh(Seq("index"))) must beLike[IndexResponse[JsValue]] {
        case e => e._shards.failed mustEqual 0
      }

      //Sroll search
      val matchAllQuery = Json.obj("query" -> Json.obj("match_all" -> Json.obj()))
      await(Source.fromPublisher(client.scrollSearch(Seq("index"), Seq("type"), matchAllQuery, size = Some(10)))
        .mapConcat[MonDocument](r => r.hitsAs[MonDocument].toList)
        .runFold(List.empty[MonDocument])((acc, elt) => acc :+ elt), Duration(5, TimeUnit.MINUTES))
        .size mustEqual 105


      await(client.deleteIndex("index")) mustEqual IndexOps(true)
      await(client.verifyIndex("index")) mustEqual false
    }


  }

  step {
    server.stop()
  }

}
