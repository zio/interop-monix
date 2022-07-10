import explicitdeps.ExplicitDepsPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtbuildinfo.BuildInfoKeys._
import sbtbuildinfo._
import sbtcrossproject.CrossPlugin.autoImport._

object BuildHelper {
  private val versions: Map[String, String] = {
    import org.snakeyaml.engine.v2.api.{ Load, LoadSettings }

    import java.util.{ List => JList, Map => JMap }
    import scala.jdk.CollectionConverters._

    val doc = new Load(LoadSettings.builder().build())
      .loadFromReader(scala.io.Source.fromFile(".github/workflows/ci.yml").bufferedReader())

    val yaml = doc.asInstanceOf[JMap[String, JMap[String, JMap[String, JMap[String, JMap[String, JList[String]]]]]]]

    val list = yaml.get("jobs").get("testJVM").get("strategy").get("matrix").get("scala").asScala

    list.map(v => (v.split('.').take(2).mkString("."), v)).toMap
  }

  val Scala212: String = versions("2.12")
  val Scala213: String = versions("2.13")
  val Scala3: String   = versions("3.1")

  private val stdOptions = Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked"
  ) ++ {
    if (sys.env.contains("CI"))
      Seq("-Xfatal-warnings")
    else
      Nil // to enable Scalafix locally
  }

  private val std2xOptions = Seq(
    "-language:higherKinds",
    "-language:existentials",
    "-explaintypes",
    "-Yrangepos",
    "-Xlint:_,-missing-interpolator,-type-parameter-shadow",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  )

  private def optimizerOptions(optimize: Boolean) =
    if (optimize)
      Seq(
        "-opt:l:inline",
        "-opt-inline-from:zio.internal.**"
      )
    else Nil

  def buildInfoSettings(packageName: String) =
    Seq(
      buildInfoKeys := Seq[BuildInfoKey](organization, moduleName, name, version, scalaVersion, sbtVersion, isSnapshot),
      buildInfoPackage := packageName
    )

  val dottySettings = Seq(
    scalacOptions ++= {
      if (scalaVersion.value == Scala3)
        Seq("-noindent")
      else
        Seq()
    },
    Compile / doc / sources := {
      val old = (Compile / doc / sources).value
      if (scalaVersion.value == Scala3)
        Nil
      else
        old
    },
    Test / parallelExecution := {
      val old = (Test / parallelExecution).value
      if (scalaVersion.value == Scala3)
        false
      else
        old
    }
  )

  def extraOptions(scalaVersion: String, optimize: Boolean) =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((3, _))  =>
        Seq(
          "-language:implicitConversions",
          "-Xignore-scala2-macros"
        )
      case Some((2, 13)) =>
        Seq(
          "-Ywarn-unused:params,-implicits"
        ) ++ std2xOptions ++ optimizerOptions(optimize)
      case Some((2, 12)) =>
        Seq(
          "-opt-warnings",
          "-Ywarn-extra-implicit",
          "-Ywarn-unused:_,imports",
          "-Ywarn-unused:imports",
          "-Ypartial-unification",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Ywarn-unused:params,-implicits",
          "-Xfuture",
          "-Xsource:2.13",
          "-Xmax-classfile-name",
          "242"
        ) ++ std2xOptions ++ optimizerOptions(optimize)
      case Some((2, 11)) =>
        Seq(
          "-Ypartial-unification",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Xexperimental",
          "-Ywarn-unused-import",
          "-Xfuture",
          "-Xsource:2.13",
          "-Xmax-classfile-name",
          "242"
        ) ++ std2xOptions
      case _             => Seq.empty
    }

  def platformSpecificSources(platform: String, conf: String, baseDirectory: File)(versions: String*) =
    for {
      platform <- List("shared", platform)
      version  <- "scala" :: versions.toList.map("scala-" + _)
      result    = baseDirectory.getParentFile / platform.toLowerCase / "src" / conf / version
      if result.exists
    } yield result

  def crossPlatformSources(scalaVer: String, platform: String, conf: String, baseDir: File) = {
    val versions = CrossVersion.partialVersion(scalaVer) match {
      case Some((2, 11)) =>
        List("2.11+", "2.11-2.12")
      case Some((2, 12)) =>
        List("2.11+", "2.12+", "2.11-2.12", "2.12-2.13")
      case Some((2, 13)) =>
        List("2.11+", "2.12+", "2.13+", "2.12-2.13")
      case Some((3, _))  =>
        List("2.11+", "2.12+", "2.13+")
      case _             =>
        List()
    }
    platformSpecificSources(platform, conf, baseDir)(versions: _*)
  }

  lazy val crossProjectSettings = Seq(
    Compile / unmanagedSourceDirectories ++= {
      crossPlatformSources(
        scalaVersion.value,
        crossProjectPlatform.value.identifier,
        "main",
        baseDirectory.value
      )
    },
    Test / unmanagedSourceDirectories ++= {
      crossPlatformSources(
        scalaVersion.value,
        crossProjectPlatform.value.identifier,
        "test",
        baseDirectory.value
      )
    }
  )

  def stdSettings(prjName: String) =
    Seq(
      name := s"$prjName",
      crossScalaVersions := Seq(Scala212, Scala213, Scala3),
      ThisBuild / scalaVersion := Scala213,
      scalacOptions ++= stdOptions ++ extraOptions(scalaVersion.value, optimize = !isSnapshot.value),
      Test / parallelExecution := true,
      incOptions ~= (_.withLogRecompileOnMacro(false)),
      autoAPIMappings := true,
      unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library")
    )

  def jsSettings =
    Seq(
      libraryDependencies += "io.github.cquiroz" %%% "scala-java-time"      % "2.4.0",
      libraryDependencies += "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.4.0"
    )
}
