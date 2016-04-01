import de.johoop.jacoco4sbt.JacocoPlugin.jacoco
import sbt._
import sbt.Keys._
import play.sbt.Play.autoImport._
import utils.ConfigurationChangeHelper._


object ApplicationBuild extends Build {
  val appName         = "cr"
  val appVersion      = "2.5-SNAPSHOT"

  processConfFiles(Seq("conf/application-info.conf"), Seq("application.version" -> appVersion))

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    "com.typesafe.play"  %% "anorm"               % "2.4.0",
    ws,
    "org.mockito"         %   "mockito-all"   % "1.10.19"   % "test" withSources() withJavadoc(),
    "org.jacoco"          % "org.jacoco.core"     % "0.7.4.201502262128"  % "test",
    "org.jacoco"          % "org.jacoco.report"   % "0.7.4.201502262128"  % "test",
    "com.rabbitmq"        %   "amqp-client"   % "3.3.5",
    "me.moocar"           %   "logback-gelf"  % "0.12",
    "gov.dwp.carers"      %% "carerscommon"   % "7.6",
    "org.specs2" %% "specs2-core" % "3.3.1" % "test" withSources() withJavadoc(),
    "org.specs2" %% "specs2-mock" % "3.3.1" % "test" withSources() withJavadoc(),
    "org.specs2" %% "specs2-junit" % "3.3.1" % "test" withSources() withJavadoc(),
    "com.kenshoo" % "metrics-play_2.10" % "2.4.0_0.4.0"
  )

  var sO:Setting[_] = scalacOptions := Seq("-deprecation", "-unchecked", "-encoding", "utf8","-feature")

  var sV: Seq[Def.Setting[_]] = Seq(scalaVersion := "2.10.5")

  var sR:Seq[Setting[_]] = Seq(
    resolvers += "Carers repo" at "http://build.3cbeta.co.uk:8080/artifactory/repo/",
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases")

  var jO: Seq[Def.Setting[_]] = Seq(testOptions in Test += Tests.Argument("sequential", "true"),
    javaOptions in Test += "-Doverride.rabbit.uri="+(System.getProperty("override.rabbit.uri") match { case s:String => s case null => ""}))

  var f: Seq[Def.Setting[_]] = Seq(sbt.Keys.fork in Test := false)

  var vS: Seq[Def.Setting[_]] = Seq(libraryDependencies ++= appDependencies)

  var sAppN: Seq[Def.Setting[_]] = Seq(name := appName)
  var sAppV: Seq[Def.Setting[_]] = Seq(version := appVersion)
  var sOrg: Seq[Def.Setting[_]] = Seq(organization := "gov.dwp.carers")

  val isSnapshotBuild = appVersion.endsWith("-SNAPSHOT")
  var publ: Seq[Def.Setting[_]] = Seq(
    publishTo := Some("Artifactory Realm" at "http://build.3cbeta.co.uk:8080/artifactory/repo/"),
    publishTo <<= version {
      (v: String) =>
        if (isSnapshotBuild)
          Some("snapshots" at "http://build.3cbeta.co.uk:8080/artifactory/libs-snapshot-local")
        else
          Some("releases" at "http://build.3cbeta.co.uk:8080/artifactory/libs-release-local")
    })

  var appSettings: Seq[Def.Setting[_]] =  sV ++ sO ++ sR  ++ vS ++ jO ++sOrg ++ f ++ jacoco.settings ++ sAppN ++ sAppV ++ sOrg ++ publ

  val main = Project(appName, file(".")).enablePlugins(play.sbt.PlayScala).settings(appSettings: _*)

}
