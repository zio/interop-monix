import sbtcrossproject.CrossPlugin.autoImport.crossProject
import explicitdeps.ExplicitDepsPlugin.autoImport.moduleFilterRemoveValue
import ScalazBuild._

inThisBuild(
  List(
    organization := "dev.zio",
    homepage := Some(url("https://zio.dev")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "jdegoes",
        "John De Goes",
        "john@degoes.net",
        url("http://degoes.net")
      )
    ),
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc"),
    releaseEarlyWith := SonatypePublisher,
    scmInfo := Some(
      ScmInfo(url("https://github.com/zio/interop-monix/"), "scm:git:git@github.com:zio/interop-monix.git")
    )
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
addCommandAlias("testJVM", ";interopMonixJVM/test")
addCommandAlias("testJS", ";interopMonixJS/test")

lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .aggregate(interopMonixJVM, interopMonixJS)
  .settings(
    skip in publish := true,
    unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library")
  )

lazy val interopMonix = crossProject(JSPlatform, JVMPlatform)
  .in(file("interop-monix"))
  .enablePlugins(BuildInfoPlugin)
  .settings(stdSettings("zio-interop-monix"))
  .settings(buildInfoSettings)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio"    %%% "zio"                  % "1.0.0-RC9-4",
      "io.monix"   %%% "monix"                % "3.0.0-RC2",
      "dev.zio"    %%% "zio"                  % "1.0.0-RC9-4" % Test classifier "tests",
      "org.specs2" %%% "specs2-core"          % "4.6.0" % Test,
      "org.specs2" %%% "specs2-scalacheck"    % "4.6.0" % Test,
      "org.specs2" %%% "specs2-matcher-extra" % "4.6.0" % Test
    )
  )

lazy val interopMonixJVM = interopMonix.jvm

lazy val interopMonixJS = interopMonix.js
  .settings(
    libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "0.2.5" % Test
  )
