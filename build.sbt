ThisBuild / version := "1.0.0"
ThisBuild / scalaVersion := "3.2.0"

ThisBuild / scalafmtConfig := file("configs/.scalafmt.conf")

ThisBuild / semanticdbEnabled := true
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / scalafixConfig := Option(file("configs/.scalafix.conf"))

scalastyleFailOnError := false
scalastyleConfig := file("configs/scalastyle-config.xml")

ThisBuild / wartremoverWarnings ++= Warts.allBut(Wart.ImplicitParameter, Wart.Equals)

ThisBuild / fork := true

assembly / assemblyJarName := "assembly.jar"
ThisBuild / assemblyMergeStrategy := {
  case PathList("module-info.class")         => MergeStrategy.discard
  case x if x.endsWith("/module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}

lazy val configVersion = "1.4.2"
lazy val akkaVersion = "2.6.20" // 2.7.x changes license to BSL v1.1
lazy val akkaHttpVersion = "10.2.10" // 10.4.x changes license to BSL v1.1
lazy val akkaMqttVersion = "4.0.0" // 5.x changes license to BSL v1.1
lazy val mqttWrapperVersion = "2.0.0"
lazy val scalaTestVersion = "3.2.14"
lazy val scalaTestPlusScalaCheck = "3.2.14.0"
lazy val logbackVersion = "1.4.5"
lazy val postgresJdbcVersion = "42.5.1"
lazy val hikariCpVersion = "5.0.1"
lazy val scalikeJdbcVersion = "3.5.0"
lazy val jooqVersion = "3.17.6"
lazy val jsonPathVersion = "2.7.0"
lazy val scalaLoggingVersion = "3.9.5"
lazy val swaggerVersion = "2.10.0"
lazy val jakartaVersion = "3.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "location-processing",
    libraryDependencies ++= Seq(
      "jakarta.ws.rs" % "jakarta.ws.rs-api" % jakartaVersion,
      ("com.github.swagger-akka-http" %% "swagger-akka-http" % swaggerVersion)
        .cross(CrossVersion.for3Use2_13),
      ("io.github.assist-iot-sripas" %% "scala-mqtt-wrapper" % mqttWrapperVersion)
        .cross(CrossVersion.for3Use2_13),
      "com.typesafe" % "config" % configVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
      ("com.typesafe.akka" %% "akka-actor-typed" % akkaVersion).cross(CrossVersion.for3Use2_13),
      ("com.typesafe.akka" %% "akka-stream-typed" % akkaVersion)
        .cross(CrossVersion.for3Use2_13),
      ("com.typesafe.akka" %% "akka-http" % akkaHttpVersion).cross(CrossVersion.for3Use2_13),
      ("com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion)
        .cross(CrossVersion.for3Use2_13),
      ("com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion)
        .cross(CrossVersion.for3Use2_13),
      ("com.lightbend.akka" %% "akka-stream-alpakka-mqtt-streaming" % akkaMqttVersion)
        .cross(CrossVersion.for3Use2_13),
      "org.postgresql" % "postgresql" % postgresJdbcVersion,
      ("org.scalikejdbc" %% "scalikejdbc" % scalikeJdbcVersion).cross(CrossVersion.for3Use2_13),
      ("org.scalikejdbc" %% "scalikejdbc-streams" % scalikeJdbcVersion)
        .cross(CrossVersion.for3Use2_13),
      ("org.scalikejdbc" %% "scalikejdbc-config" % scalikeJdbcVersion)
        .cross(CrossVersion.for3Use2_13),
      "com.zaxxer" % "HikariCP" % hikariCpVersion,
      "org.jooq" % "jooq" % jooqVersion,
      "com.jayway.jsonpath" % "json-path" % jsonPathVersion,
      ("fr.davit" %% "akka-http-metrics-prometheus" % "1.7.1")
        .cross(CrossVersion.for3Use2_13),
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.scalatestplus" %% "scalacheck-1-16" % scalaTestPlusScalaCheck % Test,
      ("com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test)
        .cross(CrossVersion.for3Use2_13),
      ("com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test)
        .cross(CrossVersion.for3Use2_13),
      ("com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test)
        .cross(CrossVersion.for3Use2_13),
      ("org.scalikejdbc" %% "scalikejdbc-test" % scalikeJdbcVersion % Test)
        .cross(CrossVersion.for3Use2_13)
    ),
    scalacOptions ++= Seq(
      "-encoding",
      "utf8",
      "-explain",
      "-explain-types",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-Ycheck-all-patmat",
      "-Vprofile",
      "-Vprofile-details",
      "10",
      "-Vprofile-sorted-by:complexity"
      // "-Yexplicit-nulls"
    )
  )
