import sbtcrossproject.CrossPlugin.autoImport.crossProject
import explicitdeps.ExplicitDepsPlugin.autoImport.moduleFilterRemoveValue
import BuildHelper._

Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(
  List(
    developers := List(
      Developer("jdegoes", "John De Goes", "john@degoes.net", url("https://degoes.net")),
      Developer("mijicd", "Dejan Mijic", "dmijic@acm.org", url("https://github.com/mijicd")),
      Developer("quelgar", "Lachlan O'Dea", "lodea@mac.com", url("https://github.com/quelgar"))
    ),
    homepage := Some(url("https://github.com/zio/interop-monix/")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    organization := "dev.zio",
    organizationName := "John A. De Goes and the ZIO contributors",
    startYear := Some(2021)
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")
addCommandAlias("testJVM", ";interopMonixJVM/test")
addCommandAlias("testJS", ";interopMonixJS/test")

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .aggregate(interopMonixJVM, interopMonixJS)
  .settings(
    publish / skip := true,
    unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library")
  )

lazy val interopMonix = crossProject(JSPlatform, JVMPlatform)
  .in(file("interop-monix"))
  .enablePlugins(BuildInfoPlugin)
  .settings(stdSettings("zio-interop-monix"))
  .settings(dottySettings)
  .settings(crossProjectSettings)
  .settings(testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")))
  .settings(buildInfoSettings("zio.interop.monix"))
  .settings(
    libraryDependencies ++= Seq(
      "io.monix" %%% "monix"        % "3.4.1",
      "dev.zio"  %%% "zio"          % "1.0.15",
      "dev.zio"  %%% "zio-test"     % "1.0.15" % Test,
      "dev.zio"  %%% "zio-test-sbt" % "1.0.15" % Test
    )
  )

lazy val interopMonixJVM = interopMonix.jvm
lazy val interopMonixJS  = interopMonix.js.settings(jsSettings)
