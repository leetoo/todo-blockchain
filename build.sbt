name := "todo-blockchain"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.5"

lazy val root = (project in file("."))

libraryDependencies ++= Seq(
  "org.scorexfoundation" %% "scorex-core" % "master-6a100ea0-SNAPSHOT",
  "org.scorexfoundation" %% "iodb" % "0.3.1",
  "org.scalactic" %% "scalactic" % "3.0.1" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.+" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.17" % "test",
  "net.databinder.dispatch" %% "dispatch-core" % "+" % "test"
)

/*
Notes for developers
App have only one important direct dependency:
* scorex-core

And few transitive dependiencies:
  * akka
  * akka-http
  * scrypto framework
  * iodb
*/

