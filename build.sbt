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
Our app have only one important direct dependency:
* scorex-core
  * The actual blockchain record technology. its physical format, validation rules, etc.
  * The book-keeping system and/or other data payload format within the blocks
  * The ‘mining’ (or ‘minting’, or ‘forging’) system which creates new valid blocks
    to add to the chain
  * The consensus system to resolve conflicts and detect attempts at malicious activity
    (which is often tied to the previous item)
  * The cryptographic system that controls who is authorised to perform actions on the blockchain
  * The network layer, which allows nodes to communicate with each other and stay in sync
  * The client API…



And few transitive dependencies:
  * akka actors
  * akka-http
  * iodb
  * scrypto framework
  *
*/


