import sbtcrossproject.CrossPlugin.autoImport.crossProject
import explicitdeps.ExplicitDepsPlugin.autoImport.moduleFilterRemoveValue
import BuildHelper._

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
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc"),
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
  .settings(testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")))
  .settings(buildInfoSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.monix" %%% "monix"       % "3.1.0",
      "dev.zio"  %%% "zio"         % "1.0.0-RC18-1",
      "dev.zio"  %%% "zio-test"    % "1.0.0-RC18-1",
      "dev.zio"  %% "zio-test-sbt" % "1.0.0-RC18-1" % "test"
    )
  )

lazy val interopMonixJVM = interopMonix.jvm

lazy val interopMonixJS = interopMonix.js
  .settings(
    libraryDependencies += "org.scala-js" %%% "scalajs-java-time" % "1.0.0" % Test
  )
