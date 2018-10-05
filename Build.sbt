import play.core.PlayVersion
import play.routes.compiler.StaticRoutesGenerator
import play.sbt.PlayImport.ws
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.{SbtAutoBuildPlugin, _}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appDependencies: Seq[ModuleID] = compile ++ test()
lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty

val appName = "claim-tax-refund"

val compile = Seq(
  ws,
  "uk.gov.hmrc" %% "play-reactivemongo" % "5.2.0",
  "uk.gov.hmrc" %% "bootstrap-play-25" % "1.7.0"
)

def test(scope: String = "test"): Seq[ModuleID] = Seq(
  "uk.gov.hmrc" %% "hmrctest" % "3.0.0" % scope,
  "org.scalatest" %% "scalatest" % "2.2.6" % scope,
  "org.pegdown" % "pegdown" % "1.6.0" % scope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % scope,
  "org.mockito" % "mockito-all" % "1.10.19" % scope,
  "org.scalacheck" %% "scalacheck" % "1.13.4" % scope,
  "com.github.tomakehurst" % "wiremock" % "2.15.0" % scope
)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
  tests map {
    test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins: _*)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesGenerator := StaticRoutesGenerator
  )
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo
  ))
  .settings(majorVersion := 0)
