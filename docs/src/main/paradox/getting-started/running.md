# Running the Projection

@@@ note

This example requires a Cassandra database to run. 
If you do not have a Cassandra database then you can run one locally as a Docker container.
To run a Cassandra database locally you can use [`docker-compose`](https://docs.docker.com/compose/) to run the [`docker-compose.yaml`](https://raw.githubusercontent.com/apache/incubator-pekko-projection/main/docker-compose.yml) found in the Projections project root.
The `docker-compose.yml` file references the latest [Cassandra Docker Image](https://hub.docker.com/_/cassandra).

Change directory to the directory of the `docker-compose.yml` file and manage a Cassandra container with the following commands.

| Action                   | Docker Command |
|--------------------------|----------------|
| Run                      | `docker-compose --project-name getting-started up -d cassandra` |
| Stop                     | `docker-compose --project-name getting-started stop` |
| Delete container state   | `docker-compose --project-name getting-started rm -f` |
| CQL shell (when running) | `docker run -it --network getting-started_default --rm cassandra cqlsh cassandra` |

To use a different Cassandra database update the [Cassandra driver's contact-points configuration](https://pekko.apache.org/docs/pekko-persistence-cassandra/current/configuration.html#contact-points-configuration) found in `./examples/src/resources/guide-shopping-cart-app.conf`.

@@@

To run the Projection we must setup our Cassandra database to support the Cassandra Projection offset store as well as the new table we are _projecting_ into with the `ItemPopularityProjectionHandler`.

Create a Cassandra keyspace.

```
CREATE KEYSPACE IF NOT EXISTS pekko_projection WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':1 };
```

Create the Cassandra Projection offset store table.
The DDL can be found in the @ref:[Cassandra Projection, Schema section](../cassandra.md#schema).

Create the `ItemPopularityProjectionHandler` projection table with the DDL below

```
CREATE TABLE IF NOT EXISTS pekko_projection.item_popularity (
item_id text,
count counter,
PRIMARY KEY (item_id));
```

Source events are generated with the `EventGeneratorApp`.
This app is configured to use [Apache Pekko Persistence Cassandra](https://pekko.apache.org/docs/pekko-persistence-cassandra/current/index.html) and [Apache Pekko Cluster](https://pekko.apache.org/docs/pekko/current/typed/cluster.html) [Sharding](https://pekko.apache.org/docs/pekko/current/typed/cluster-sharding.html) to persist random `ShoppingCartApp.Events` to a journal.
It will checkout a shopping cart with random items and quantities every 1 second.
The app will automatically create all the Apache Pekko Persistence infrastructure tables in the `pekko` keyspace.
We won't go into any further detail about how this app functions because it falls outside the scope of Apache Pekko Projections.
To learn more about the writing events with [Apache Pekko Persistence see the Apache Pekko documentation](https://pekko.apache.org/docs/pekko/current/typed/index-persistence.html).

Add the Apache Pekko Cluster Sharding library to your project:

@@dependency [sbt,Maven,Gradle] {
group=org.apache.pekko
artifact=pekko-cluster-sharding-typed_$scala.binary.version$
version=$pekko.version$
}

Add the @ref:[EventGeneratorApp](event-generator-app.md) to your project.

Run `EventGeneratorApp`:

<!-- run from repo:
sbt "examples/test:runMain docs.guide.EventGeneratorApp"
sbt "examples/test:runMain jdocs.guide.EventGeneratorApp"
-->

sbt
:   @@@vars
```
sbt "runMain docs.guide.EventGeneratorApp"
```
@@@

Maven
:   @@@vars
```
mvn compile exec:java -Dexec.mainClass="jdocs.guide.EventGeneratorApp"
```
@@@

If you don't see any connection exceptions then you should eventually see log lines produced indicating that events are written to the journal.

Ex)

```shell
[2020-08-13 15:20:05,583] [INFO] [docs.guide.EventGeneratorApp$] [] [EventGenerator-pekko.actor.default-dispatcher-22] - id [cb52b] tag [shopping-cart] event: ItemQuantityAdjusted(cb52b,skis,1,1) MDC: {persistencePhase=persist-evt, pekkoAddress=pekko://EventGenerator@127.0.1.1:25520, pekkoSource=pekko://EventGenerator/system/sharding/shopping-cart-event/678/cb52b, sourceActorSystem=EventGenerator, persistenceId=cb52b}
```

Finally, we can run `ShoppingCartApp` in a new terminal:

<!-- run from repo:
sbt "examples/test:runMain docs.guide.ShoppingCartApp"
-->

sbt
:   @@@vars
```
sbt "runMain docs.guide.ShoppingCartApp"
```
@@@

Maven
:   @@@vars
```
mvn compile exec:java -Dexec.mainClass="jdocs.guide.ShoppingCartApp"
```
@@@

After a few seconds you should see the `ItemPopularityProjectionHandler` logging that displays the current checkouts for the day:

```shell
[2020-08-12 12:16:34,216] [INFO] [docs.guide.ItemPopularityProjectionHandler] [] [ShoppingCartApp-pekko.actor.default-dispatcher-10] - ItemPopularityProjectionHandler(shopping-cart) item popularity for 'bowling shoes': [58]
```

Use the CQL shell to observe the full information in the `item_popularity` table.

```
cqlsh> SELECT item_id, count FROM pekko_projection.item_popularity;

 item_id       | count
---------------+-------
 pekko t-shirt |    37
   cat t-shirt |    34
          skis |    33
 bowling shoes |    65

(4 rows)
```
