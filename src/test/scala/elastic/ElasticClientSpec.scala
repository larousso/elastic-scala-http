package elastic

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import elastic.api._
import elastic.implicits._
import elastic.codec.PlayJson._
import elastic.client.ElasticClient
import elastic.spec.ElasticEmbeddedServer
import org.reactivestreams.Publisher
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures, Waiters}
import org.scalatest.time.{Minutes, Span}
import org.scalatest._
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Created by adelegue on 12\04\2016.
  */
class ElasticClientSpec extends WordSpec with MustMatchers with OptionValues with ScalaFutures with Waiters with IntegrationPatience with BeforeAndAfterAll {

  import Helper._

  case class MonDocument(name: String)
  case class Parent(name: String)
  case class Child(name: String)

  implicit val docFormat = Json.format[MonDocument]
  implicit val parentFormat = Json.format[Parent]
  implicit val childFormat = Json.format[Child]

  val server = new ElasticEmbeddedServer
  implicit val actorSystem = ActorSystem()
  implicit val mat = ActorMaterializer()
  import actorSystem.dispatcher

  implicit val client: ElasticClient[JsValue] = ElasticClient.fromServer(s"http://localhost:10901")

  override protected def beforeAll(): Unit = {
    server.run()
  }

  override protected def afterAll(): Unit = {
    server.stop()
    actorSystem.terminate()
  }

  "Client" should {

    "verify index" in {
      client.verifyIndex("test").futureValue mustEqual false
    }

    "creating empty index and deleting" in {
      cleanUp("test") {
        client.createIndex("test", Json.obj()).futureValue mustEqual IndexOps(true)
        client.verifyIndex("test").futureValue mustEqual true
        client.deleteIndex("test").futureValue mustEqual IndexOps(true)
        client.verifyIndex("test").futureValue mustEqual false
      }
    }

    "creating index with settings, reading it and deleting it" in {
      cleanUp("test") {
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
        client.createIndex("test", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)
        client.verifyIndex("test").futureValue mustEqual true
        val resp = client.getIndex("test").futureValue

        (resp \ "test" \ "settings" \ "index" \ "number_of_shards").as[String] mustEqual "1"
        (resp \ "test" \ "mappings" \ "type1" \ "properties" \ "field1" \ "type").as[String] mustEqual "keyword"
      }
    }

    "reading unknow index" in {
      cleanUp("test") {
        val thrown = client.getIndex("test").failing[EsException[JsValue]]

        thrown.httpCode must be(404)
        (thrown.json \ "error" \ "type").as[String] mustEqual "index_not_found_exception"
      }
    }

    "creating index and get mapping" in {
      cleanUp("test") {
        val jsonSettings =
          """
          {
            "settings" : { "number_of_shards" : 1 },
            "mappings" : {
              "type1" : {
                "properties" : {
                  "field1" : { "type" : "keyword" }
                }
              }
            }
          }
        """
        client.createIndex("test", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)
        client.getMapping("test", "type1").futureValue mustEqual Json.parse(
          """
        {
          "test" : {
            "mappings" : {
              "type1" : {
                "properties" : {
                  "field1" : { "type" : "keyword" }
                }
              }
            }
          }
        } """)
      }
    }

    "creating index and get mappings" in {
      cleanUp("test1", "test2") {
        val jsonSettings =
          """
          {
            "mappings" : {
              "type1" : {
                "properties" : {
                  "field1" : { "type" : "keyword" }
                }
              },
              "type2" : {
                "properties" : {
                  "field1" : { "type" : "keyword" }
                }
              }
            }
          }
        """
        client.createIndex("test1", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)
        client.createIndex("test2", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)
        client.getMappings(Seq("test1", "test2"), Seq("type1", "type2")).futureValue mustEqual Json.parse(
          """
      {
        "test2": {
          "mappings": {
            "type2": {
              "properties": {
                "field1": {
                  "type": "keyword"
                }
              }
            },
            "type1": {
              "properties": {
                "field1": {
                  "type": "keyword"
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
                  "type": "keyword"
                }
              }
            },
            "type1": {
              "properties": {
                "field1": {
                  "type": "keyword"
                }
              }
            }
          }
        }
      }
        """)
      }
    }


    "creating index and updating mapping" in {
      cleanUp("twitter") {
        val jsonSettings =
          """
          {
            "mappings": {
              "tweet": {
                "properties": {
                  "message": { "type" : "text" }
                }
              }
            }
          }
        """
        client.createIndex("twitter", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)
        val mappingUpdate =
          """
        {
          "properties": {
            "user_name": { "type" : "text" }
          }
        }
        """
        client.putMapping("twitter", "tweet", Json.parse(mappingUpdate), false).futureValue mustEqual IndexOps(true)
        client.getMapping("twitter", "tweet").futureValue mustEqual Json.parse(
          """
        {
          "twitter" : {
            "mappings" : {
              "tweet" : {
                "properties" : {
                  "message": { "type": "text" },
                  "user_name": { "type" : "text" }
                }
              }
            }
          }
        } """)
      }
    }

    "creating index and updating mappings with update_all_types" in {
      cleanUp("my_index") {
        val jsonSettings =
          """
          |{
          |  "mappings": {
          |    "type_one": {
          |      "properties": {
          |        "text": {
          |          "type" : "text",
          |          "analyzer": "standard"
          |        }
          |      }
          |    },
          |    "type_two": {
          |      "properties": {
          |        "text": {
          |          "type" : "text",
          |          "analyzer": "standard"
          |        }
          |      }
          |    }
          |  }
          |}
        """.
            stripMargin
        client.createIndex(
          "my_index", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)
      val
      mappingUpdate =
        """
          |{
          |  "properties": {
          |    "text": {
          |      "type" : "text",
          |      "analyzer": "standard",
          |      "search_analyzer": "whitespace"
          |    }
          |  }
          |}
        """.
          stripMargin
      client.putMapping("my_index" / "type_one", Json.parse(mappingUpdate)).failing[EsException[JsValue]]
      client.
        putMapping("my_index" / "type_one", Json.parse(mappingUpdate), update_all_types = true).futureValue mustEqual IndexOps(true)
        client.getMapping("my_index" / "type_one").futureValue mustEqual Json.parse(
          """
        {
          "my_index" : {
            "mappings" : {
              "type_one" : {
                "properties" : {
                  "text": {
                    "type" : "text",
                    "analyzer": "standard",
                    "search_analyzer": "whitespace"
                  }
                }
              }
            }
          }
        } """)
      }
    }

    "creating index and aliases and get mapping" in {
      cleanUp("test") {
        val jsonSettings =
          """
          {
            "mappings" : {
              "type1" : {
                "properties" : {
                  "field1" : { "type" : "keyword" }
                }
              }
            }
          }
        """
        client.createIndex("test", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)

        val aliases =
          """
          |{
          |    "actions" : [
          |        { "add" : { "index" : "test", "alias" : "alias" } }
          |    ]
          |}
        """.
            stripMargin
        client.createAliases(Json.parse(aliases)).futureValue mustEqual IndexOps(true)
        val alias = client.getIndex("alias").futureValue
        (alias \ "test" \ "aliases" \ "alias").as[JsObject] must be(Json.obj())
      }
    }


    "indexing document and reading it and deleting it" in {
      cleanUp("test") {
        val index: Index[JsValue] = client.index("test", Some("type"))

        val document = MonDocument("nom")
        val indexResponse = index.index(document, Some("id")).futureValue

        indexResponse._index must be(Some("test"))


        index.get("id").map(_.as[MonDocument]).futureValue mustEqual document

        val d = index.delete("id").futureValue
        d.found must be(Some(true))
        d._id must be(Some("id"))

        val e = index.get("id").map(_.as[MonDocument]).failing[EsException[JsValue]]
        e.httpCode mustEqual 404
      }
    }

    "indexing twice a document with create option" in {
      cleanUp("test") {
        val index: Index[JsValue] = client.index("test", Some("type"))

        val document = MonDocument("nom")
        val indexResponse = index.index(document, Some("id")).futureValue
        indexResponse._index must be(Some("test"))

        val e = index.create(document, Some("id")).failing[EsException[JsValue]]

        e.httpCode mustEqual 409
        (Json.parse(e.getMessage) \ "error" \ "type").as[String] mustEqual "version_conflict_engine_exception"
        //(Json.parse(e.getMessage) \ "error" \ "type").as[String] mustEqual "document_already_exists_exception"
      }
    }

    "indexing document with version and reading it" in {
      cleanUp("test") {
        val index: Index[JsValue] = client.index("test", Some("type"))

        val document = MonDocument("nom")
        index.index(document, Some("id")).futureValue._index must be(Some("test"))

        index.get("id").futureValue mustEqual GetResponse("test", "type", "id", 1, true, Json.toJson(document))

        index.index(document, Some("id"), version = Some(1)).futureValue._index must be(Some("test"))

        index.get("id").futureValue mustEqual GetResponse("test", "type", "id", 2, true, Json.toJson(document))

      }
    }

    "indexing document with parent and reading it" in {
      cleanUp("parentchild") {
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
        client.createIndex("parentchild", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)

        val parent: Index[JsValue] = client.index("parentchild", Some("parent"))
        val child: Index[JsValue] = client.index("parentchild", Some("child"))

        val parentObj = Parent("I'am the parent")
        val indexResponse = parent.index(parentObj, Some("id1")).futureValue
        indexResponse._index must be(Some("parentchild"))
        indexResponse._type must be(Some("parent"))

        parent.get("id1").futureValue mustEqual GetResponse("parentchild", "parent", "id1", 1, true, Json.toJson(parentObj))

        val childObj = Child("I'am the child")
        val indexResponse2 = child.index(childObj, Some("id2"), parent = Some("id1")).futureValue
        indexResponse2._index must be(Some("parentchild"))
        indexResponse2._type must be(Some("child"))

        child.get("id2", routing = Some("id1")).futureValue mustEqual GetResponse("parentchild", "child", "id2", 1, true, Json.toJson(childObj))
      }
    }

    "indexing document with refresh option and searching it" in {
      cleanUp("test") {
        val jsonSettings =
          """
          {
            "settings" : { "number_of_shards" : 1 },
            "mappings" : {
              "type" : {
                "properties" : {
                  "name" : { "type" : "keyword" }
                }
              }
            }
          }
        """
        client.createIndex("test", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)
        client.verifyIndex("test").futureValue mustEqual true

        val index: Index[JsValue] = client.index("test", Some("type"))

        val document = MonDocument("nom")
        index.index(document, Some("id")).futureValue._index must be(Some("test"))

        val r = index.search(Json.obj("query" -> Json.obj("term" -> Json.obj("name" -> "nom")))).futureValue
        r.hits.total mustEqual 0

        index.index(document, Some("id"), refresh = true).futureValue._index must be(Some("test"))

        val r2 = index.search(Json.obj("query" -> Json.obj("term" -> Json.obj("name" -> "nom")))).futureValue
        r2.hits.total mustEqual 1
        r2.hitsAs[MonDocument] mustEqual Seq(document)
      }
    }

    "mget " in {
      cleanUp("test") {
        val index: Index[JsValue] = client.index("test", Some("type"))

        val document1 = MonDocument("nom")
        index.index(document1, Some("1")).futureValue._index must be(Some("test"))

        val document2 = MonDocument("nom")
        index.index(document2, Some("2")).futureValue._index must be(Some("test"))


        val response = client.mget(request = MGets(MGet(Some("test"), Some("type"), "1"), MGet(Some("test"), Some("type"), "1"))).futureValue
        response.docs.size mustEqual 2

        response.docsAs[MonDocument] mustEqual Seq(document1, document2)
      }
    }

    "create, read, verify, delete template" in {
      cleanUp("test") {
        client.verifyTemplate("template_1").futureValue mustEqual false

        val templateDef = Json.parse(
          """
          |{
          |  "template": "te*",
          |  "settings": {
          |    "number_of_shards": 1
          |  },
          |  "mappings": {
          |    "type1": {
          |      "_source": {
          |        "enabled": false
          |      },
          |      "properties": {
          |        "host_name": {
          |          "type" : "text",
          |          "index": "not_analyzed"
          |        },
          |        "created_at": {
          |          "type": "date",
          |          "format": "EEE MMM dd HH:mm:ss Z YYYY"
          |        }
          |      }
          |    }
          |  }
          |}
        """.
            stripMargin)
      client
        .putTemplate(
          "template_1", templateDef).futureValue mustEqual IndexOps(true)
      client.
        verifyTemplate("template_1").futureValue mustEqual true

      val

      template1 = Json.parse(
        """
          |{
          |  "template_1": {
          |    "order": 0,
          |    "template": "te*",
          |    "settings": {
          |      "index": {
          |        "number_of_shards": "1"
          |      }
          |    },
          |    "mappings": {
          |      "type1": {
          |        "_source": {
          |          "enabled": false
          |        },
          |        "properties": {
          |          "created_at": {
          |            "format": "EEE MMM dd HH:mm:ss Z YYYY",
          |            "type": "date"
          |          },
          |          "host_name": {
          |            "index": "not_analyzed",
          |            "type" : "text"
          |          }
          |        }
          |      }
          |    },
          |    "aliases": {}
          |  }
          |}
        """.stripMargin)
        client.getTemplate("template_1").futureValue mustEqual template1
      }
    }

    "bulk operation " in {
      cleanUp("index") {
        val ids = (1 to 105).map(i => i.toString).toList
        val publisher: Publisher[Bulk[MonDocument]] = Source(ids)
          .map(i => Bulk[MonDocument](BulkOpType(index = Some(BulkOpDetail(Some("index"), Some("type"), Some(i)))), Some(MonDocument(s"Nom $i"))))
          .runWith(Sink.asPublisher(fanout = false))

        val respPublisher: Publisher[BulkResponse[JsValue]] = client.bulk[MonDocument](publisher = publisher, batchSize = 10)

        val res: Future[List[BulkResponse[JsValue]]] = Source.fromPublisher(respPublisher).runFold(List.empty[BulkResponse[JsValue]])((acc, elt) => acc :+ elt)
        val result: List[BulkResponse[JsValue]] = res.futureValue(Timeout(Span(1, Minutes)))
        val allItems = result.flatMap(r => r.items).flatMap(i => i.index.toList).map(i => i._id)
        allItems mustEqual ids

        client.refresh("index").futureValue._shards.failed mustEqual 0

        val search: SearchResponse[JsValue] = client.index("index", Some("type")).search(Json.obj("query" -> Json.obj("match_all" -> Json.obj()))).futureValue
        search.hits.total mustEqual 105
      }
    }


    "bulk operation as flow" in {
      cleanUp("index") {
        val ids = (1 to 105).map(i => i.toString).toList
        val res: Future[Seq[BulkResponse[JsValue]]] = Source(ids)
          .map(i => Bulk[MonDocument](BulkOpType(index = Some(BulkOpDetail(Some("index"), Some("type"), Some(i)))), Some(MonDocument(s"Nom $i"))))
          .via(client.bulkFlow[MonDocument](batchSize = 10))
          .runWith(Sink.seq)

        val result: List[BulkResponse[JsValue]] = res.futureValue(Timeout(Span(1, Minutes))).toList
        val allItems = result.flatMap(r => r.items).flatMap(i => i.index.toList).map(i => i._id)
        allItems mustEqual ids

        client.refresh("index").futureValue._shards.failed mustEqual 0

        val search: SearchResponse[JsValue] = client.index("index", Some("type")).search(Json.obj("query" -> Json.obj("match_all" -> Json.obj()))).futureValue
        search.hits.total mustEqual 105
      }
    }

    "scroll operation" in {
      cleanUp("scroll_index") {
        val jsonSettings = """ { "settings" : { "number_of_shards" : 1 } } """
        client.createIndex("scroll_index", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)

        val ids = (1 to 105).map(i => i.toString).toList
        Source(ids)
          .map(i => Bulk.index("scroll_index", "type", i, MonDocument(s"Nom $i")))
          .via(client.bulkFlow[MonDocument](batchSize = 10))
          .runWith(Sink.seq)
          .futureValue
          .flatMap(r => r.items)
          .flatMap(i => i.index.toList)
          .map(i => i._id) mustEqual ids

        client.refresh("scroll_index").futureValue._shards.failed mustEqual 0

        //Sroll search
        val matchAllQuery = Json.obj("query" -> Json.obj("match_all" -> Json.obj()))
        //client.search(index = Seq("scroll_index"), query = matchAllQuery).futureValue.hits.total mustEqual 105

        val scrollResult = client.scroll(index = Seq("scroll_index"), query = matchAllQuery, size = 10, scroll = "1s")
          .mapConcat[MonDocument](r => r.hitsAs[MonDocument].toList)
          .runWith(Sink.seq).futureValue

        scrollResult.size mustEqual 105

      }
    }

    "flush" in {
      cleanUp("index") {
        val jsonSettings = """ { "settings" : { "number_of_shards" : 1 } } """
        client.createIndex("index", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)

        client.flush(Seq("index")).futureValue mustEqual IndexResponse(_shards = Shards[JsValue](2, 0, 1, Seq()))
      }
    }

    "analyse" in {
      cleanUp("index") {
          client.analyse(query = Json.parse("""{ "analyzer" : "standard", "text" : "this is a test" }""")).futureValue mustEqual Json.parse(
            """
            |{
            |   "tokens": [
            |      {
            |         "token": "this",
            |         "start_offset": 0,
            |         "end_offset": 4,
            |         "type": "<ALPHANUM>",
            |         "position": 0
            |      },
            |      {
            |         "token": "is",
            |         "start_offset": 5,
            |         "end_offset": 7,
            |         "type": "<ALPHANUM>",
            |         "position": 1
            |      },
            |      {
            |         "token": "a",
            |         "start_offset": 8,
            |         "end_offset": 9,
            |         "type": "<ALPHANUM>",
            |         "position": 2
            |      },
            |      {
            |         "token": "test",
            |         "start_offset": 10,
            |         "end_offset": 14,
            |         "type": "<ALPHANUM>",
            |         "position": 3
            |      }
            |   ]
            |}
          """.
              stripMargin)
        }
    }
    "forcemerge" in {
      cleanUp("index") {
        val
        jsonSettings =
          """ { "settings" : { "number_of_shards" : 1 } } """
        client.createIndex("index", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)

        client.forceMerge(Seq.empty[String]).futureValue mustEqual IndexResponse(_shards = Shards[JsValue](2, 0, 1, Seq()))
      }
    }

    "shard stores" in {
      cleanUp("index") {
        val jsonSettings = """ { "settings" : { "number_of_shards" : 1 } } """
        client.createIndex("index", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)

        client.shardStores(Seq("index")).futureValue must not be Json.obj()
      }
    }

    "upgrade" in {
      cleanUp("index") {
        val jsonSettings = """ { "settings" : { "number_of_shards" : 1 } } """
        client.createIndex("index", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)

        client.upgrade("index").futureValue must not be Json.obj()
        client.upgradeStatus("index").futureValue must not be Json.obj()
      }
    }

    "recovery" in {
      cleanUp("index") {
        val jsonSettings = """ { "settings" : { "number_of_shards" : 1 } } """
        client.createIndex("index", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)

        client.recovery(Seq("index")).futureValue must not be Json.obj()
      }
    }

    "segments" in {
      cleanUp("index") {
        val jsonSettings = """ { "settings" : { "number_of_shards" : 1 } } """
        client.createIndex("index", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)

        client.segments(Seq("index")).futureValue must not be Json.obj()
      }
    }

    "clear cache" in {
      cleanUp("index") {
        val jsonSettings = """ { "settings" : { "number_of_shards" : 1 } } """
        client.createIndex("index", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)

        client.clearCache(Seq("index")).futureValue must not be Json.obj()
      }
    }

    "refresh" in {
      cleanUp("index") {
        val jsonSettings = """ { "settings" : { "number_of_shards" : 1 } } """
        client.createIndex("index", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)

        client.refresh("index").futureValue must not be Json.obj()
      }
    }

    "stats" in {
      cleanUp("index") {
        val jsonSettings = """ { "settings" : { "number_of_shards" : 1 } } """
        client.createIndex("index", Json.parse(jsonSettings)).futureValue mustEqual IndexOps(true)

        client.stats(Seq("index")).futureValue must not be Json.obj()
      }
    }
  }



  def cleanUp(indices: String*)(test: => Unit)(implicit client: Elastic[JsValue]) = {
    try {
      test
    } finally {
      Future.sequence(
        indices.map(i =>
          client.deleteIndex(i).map(_ => true).recover{ case _ => true }
        )
      ).futureValue
    }
  }

}



object Helper {
  implicit class Failing[A](val f: Future[A]) extends Assertions with Waiters {
    def failing[T <: Throwable](implicit m: Manifest[T], ec: ExecutionContext) = {
      val w = new Waiter
      f onComplete {
        case Failure(e) => w(throw e); w.dismiss()
        case Success(_) => w.dismiss()
      }
      intercept[T] {
        w.await
      }
    }
  }
}