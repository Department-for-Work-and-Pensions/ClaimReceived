// Comment to get more information during initialization
logLevel := Level.Warn

resolvers += "Carers repo" at "http://build.3cbeta.co.uk:8080/artifactory/repo/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.3")

addSbtPlugin("de.johoop" % "jacoco4sbt" % "2.1.6")

libraryDependencies += "gov.dwp.carers" %% "carerscommon" % "7.5"
