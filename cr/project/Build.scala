import sbt._
import sbt.Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "cr"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "org.specs2"          %%  "specs2"        % "2.3.6"   % "test",
    "org.mockito"         %   "mockito-all"   % "1.9.5"   % "test",
    "com.rabbitmq"        %   "amqp-client"   % "3.1.1",
    "me.moocar"           %   "logback-gelf"  % "0.9.6p2",
    "postgresql"          %   "postgresql"    % "9.1-901.jdbc4",
    "com.codahale.metrics" % "metrics-healthchecks" % "3.0.1",
    "com.kenshoo"         %% "metrics-play"        % "0.1.4",
    "com.dwp.carers"        %% "carerscommon"         % "5.4"
  )

  val res = resolvers ++= Seq(
    "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "Carers repo" at "http://build.3cbeta.co.uk:8080/artifactory/repo/"
  )

  val main = play.Project(appName, appVersion, appDependencies,settings = play.Project.playScalaSettings).settings(
    res,
    testOptions in Test += Tests.Argument("sequential"),
    organization  := "uk.gov.service.carersallowance",
    scalaVersion  := "2.10.3",
    scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8","-feature"),
    javaOptions in Test += "-Doverride.rabbit.uri="+(System.getProperty("override.rabbit.uri") match { case s:String => s case null => ""}),
    sbt.Keys.fork in Test := false
  )

}
