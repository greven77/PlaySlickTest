name := """ReactiveOverflow"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  cache,
  ws,
  evolutions,
  "org.scalatest" % "scalatest_2.11" % "3.0.1" % "test",
  "mysql" % "mysql-connector-java" % "6.0.3",
  "com.typesafe.play" %% "play-slick" % "2.0.2",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.2",
  "de.svenkubiak" % "jBCrypt" % "0.4.1",
  "com.softwaremill.macwire" %% "macros" % "2.2.2" % "provided",
  "com.softwaremill.macwire" %% "util" % "2.2.2",
  "com.softwaremill.macwire" %% "proxy" % "2.2.2",
  "com.jason-goodwin" % "authentikat-jwt_2.11" % "0.4.5",
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.2.0"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += DefaultMavenRepository

routesGenerator := InjectedRoutesGenerator
