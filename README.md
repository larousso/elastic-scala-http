Elastic scala http client  
=========================

Elasticsearch scala http client base on akka http. Integration with play json library for json (de)serialisation. 

Work with elasticsearch 2.3.1

# Installation 

Dependencies : 

```scala
libraryDependencies += "com.adelegue" %% "elastic-scala-http" % "0.0.2"

resolvers += "larousso" at "https://raw.github.com/larousso/elastic-scala-http/master/repository/releases",
```

# Initialisation

Import :  

```scala
import elastic.api._  
import elastic.client._

// Play json support 
import elastic.codec.PlayJson._
```

Client creation :

```scala
implicit val actorSystem = ActorSystem()
val client = ElasticClient[JsValue](host = "localhost", port = 9200)
```

Or 

```scala
val client: ElasticClient[JsValue] = ElasticClientBuilder()
                .withHost("localhost")
                .withPort(9200)
                //Optional default one provided 
                .withActorSystem(actorSystem)               
                .build()
```

# Usage 

## Root level operations 

```scala

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
  val indexCreation: Future[IndexOps] = client.createIndex("test", Json.parse(jsonSettings))

  //True
  val index: Future[Boolean] = client.verifyIndex("test")

  //Json index
  val indexDefinition: Future[JsValue] = client.getIndex("test")

  //Json mapping
  val mapping: Future[JsValue] = client.getMapping("test", "type1")

  //Json mappings
  val mappings: Future[JsValue] = client.getMappings(Seq("test1", "test2"), Seq("type1", "type2"))

  val aliases = """
      {
          "actions" : [
              { "add" : { "index" : "test", "alias" : "alias" } }
          ]
      } """
  private val aliasesCreation: Future[IndexOps] = client.createAliases(Json.parse(aliases))

  val deletion: Future[IndexOps] = client.deleteIndex("test")

  //False
  val indexExists: Future[Boolean] = client.verifyIndex("test")

  //Index doesn't exists anymore
  client.getIndex("test").onFailure {
    case e: EsException[JsValue] =>
      val httpCode: Int = e.httpCode //404
  }
```

## Index level operations 

```scala

  case class MonDocument(name: String)

  implicit val docFormat = Json.format[MonDocument]

  //Index référence
  val indexTest: Index[JsValue] = client.index("test", Some("type"))

  // Id autocreation
  val create: Future[IndexResponse[JsValue]] = indexTest.index(MonDocument("name"))

  // With id
  val createWithId: Future[IndexResponse[JsValue]] = indexTest.index(MonDocument("name"), Some("id1"))
  //Update
  val update: Future[IndexResponse[JsValue]] = indexTest.index(MonDocument("name bis"), Some("id1"))

  // Create on existing doc
  indexTest.create(MonDocument("name bis"), Some("id1")).onFailure {
    case e: EsException[JsValue] =>
      e.httpCode // 409
      e.json // / ("error") / ("type" -> "document_already_exists_exception")
  }

  //Read
  val doc: Future[MonDocument] = indexTest.get("id1").map(_.as[MonDocument])

  val delete: Future[IndexResponse[JsValue]] = indexTest.delete("id1")


  //Indexing options 
  indexTest.index(MonDocument("test"),
    id = Some("id"),
    version = Some(1),
    versionType = Some(INTERNAL),
    create = false,
    routing = Some("routing"),
    parent = Some("parent id"),
    refresh = false,
    timeout = Some("1m"),
    consistency = Some(QUORUM),
    detectNoop = false
  )
  
  //Searching  
  val search: Future[SearchResponse[JsValue]] = indexTest.search(Json.obj("query" -> Json.obj("term" -> Json.obj("name" -> "nom"))))
  val docs: Future[Seq[MonDocument]] = search.map(_.hitsAs[MonDocument])
  
```

## Parent child 

```scala

  case class Parent(name: String)
  case class Child(name: String)
  implicit val parentFormat = Json.format[Parent]
  implicit val childFormat = Json.format[Child]

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
  client.createIndex("parentchild", Json.parse(jsonSettings))

  val parent: Index[JsValue] = client.index("parentchild", Some("parent"))
  val child: Index[JsValue] = client.index("parentchild", Some("child"))

  val parentObj = Parent("I'am the parent")
  private val parentIndexing: Future[IndexResponse[JsValue]] = parent.index(parentObj, Some("id1"))


  val childObj = Child("I'am the child")
  val childIndexing: Future[IndexResponse[JsValue]] = child.index(childObj, Some("id2"), parent = Some("id1"))

  //Routing option with parent id to get child
  val gettingChild: Future[GetResponse[JsValue]] = child.get("id2", routing = Some("id1"))

```

## Reactive streams 

Reactive streams support for bulk and scroll search 

Bulks : 

```scala 

  val publisher: Publisher[Bulk[MonDocument]] = Source((1 to 105).map(_.toString))
        .map(i => Bulk[MonDocument](BulkOpType(index = Some(BulkOpDetail(Some("index"), Some("type"), Some(i)))), Some(MonDocument(s"Nom $i"))) )
        .runWith(Sink.asPublisher(fanout = false))
  
  // Publisher of bulk operation 
  // batchSize is number elements send to es each request 
  val respPublisher: Publisher[BulkResponse[JsValue]] = client.bulk[MonDocument](publisher = publisher, batchSize = 10)

``` 
Or 
```scala 
    val ids = (1 to 105).map(i => i.toString).toList
    val res: Future[Seq[BulkResponse[JsValue]]] = Source(ids)
      .map(i => Bulk[MonDocument](BulkOpType(index = Some(BulkOpDetail(Some("index"), Some("type"), Some(i)))), Some(MonDocument(s"Nom $i"))))
      .via(client.bulkFlow[MonDocument](batchSize = 10))
      .runWith(Sink.seq)

```


Scroll search 

```scala 
  val matchAllQuery = Json.obj("query" -> Json.obj("match_all" -> Json.obj()))
  
  Source.fromPublisher(client.scrollPublisher(Seq("index"), Seq("type"), matchAllQuery, size = Some(10)))
      .mapConcat[MonDocument](r => r.hitsAs[MonDocument].toList)
      .runForeach(println)
  
  //Or 
  client.scroll(index = Seq("index"), query = matchAllQuery, size = Some(10)))
        .mapConcat[MonDocument](r => r.hitsAs[MonDocument].toList)
        .runForeach(println)      

```

#TODO
 
msearch  