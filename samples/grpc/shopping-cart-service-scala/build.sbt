name := "shopping-cart-service"

organization := "org.apache.pekko.samples"
organizationHomepage := Some(url("https://pekko.apache.org"))
licenses := Seq(
  ("CC0", url("https://creativecommons.org/publicdomain/zero/1.0")))

scalaVersion := "2.13.10"

Compile / scalacOptions ++= Seq(
  "-target:11",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xlint")
Compile / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

Test / parallelExecution := false
Test / testOptions += Tests.Argument("-oDF")
Test / logBuffered := false

run / fork := true
// pass along config selection to forked jvm
run / javaOptions ++= sys.props
  .get("config.resource")
  .fold(Seq.empty[String])(res => Seq(s"-Dconfig.resource=$res"))
Global / cancelable := false // ctrl-c

val PekkoVersion = "2.7.0"
val PekkoHttpVersion = "10.4.0"
val PekkoManagementVersion = "1.2.0"
val PekkoPersistenceR2dbcVersion = "1.0.1"
val PekkoProjectionVersion =
  sys.props.getOrElse("pekko-projection.version", "1.3.0")

enablePlugins(PekkoGrpcPlugin)

enablePlugins(JavaAppPackaging, DockerPlugin)
dockerBaseImage := "docker.io/library/adoptopenjdk:11-jre-hotspot"
dockerUsername := sys.props.get("docker.username")
dockerRepository := sys.props.get("docker.registry")
ThisBuild / dynverSeparator := "-"

libraryDependencies ++= Seq(
  // 1. Basic dependencies for a clustered application
  "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
  "org.apache.pekko" %% "pekko-cluster-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-cluster-sharding-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % PekkoVersion % Test,
  "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion % Test,
  // Pekko Management powers Health Checks and Pekko Cluster Bootstrapping
  "org.apache.pekko" %% "pekko-management" % PekkoManagementVersion,
  "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
  "org.apache.pekko" %% "pekko-http-spray-json" % PekkoHttpVersion,
  "org.apache.pekko" %% "pekko-management-cluster-http" % PekkoManagementVersion,
  "org.apache.pekko" %% "pekko-management-cluster-bootstrap" % PekkoManagementVersion,
  "org.apache.pekko" %% "pekko-discovery" % PekkoVersion,
  // Common dependencies for logging and testing
  "org.apache.pekko" %% "pekko-slf4j" % PekkoVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "org.scalatest" %% "scalatest" % "3.1.2" % Test,
  // 2. Using gRPC and/or protobuf
  "org.apache.pekko" %% "pekko-http2-support" % PekkoHttpVersion,
  // 3. Using Pekko Persistence
  "org.apache.pekko" %% "pekko-persistence-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-serialization-jackson" % PekkoVersion,
  "org.apache.pekko" %% "pekko-persistence-r2dbc" % PekkoPersistenceR2dbcVersion,
  "org.apache.pekko" %% "pekko-persistence-testkit" % PekkoVersion % Test,
  // 4. Querying and publishing data from Pekko Persistence
  "org.apache.pekko" %% "pekko-persistence-query" % PekkoVersion,
  "org.apache.pekko" %% "pekko-projection-r2dbc" % PekkoPersistenceR2dbcVersion,
  "org.apache.pekko" %% "pekko-projection-grpc" % PekkoProjectionVersion,
  "org.apache.pekko" %% "pekko-projection-eventsourced" % PekkoProjectionVersion,
  "org.apache.pekko" %% "pekko-projection-testkit" % PekkoProjectionVersion % Test)
