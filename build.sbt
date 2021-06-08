import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.{SbtAutoBuildPlugin, _}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import play.sbt.routes.RoutesKeys


lazy val appDependencies: Seq[ModuleID] = compile ++ test()
lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty

val appName = "claim-tax-refund"

val compile = Seq(
  ws,
  "uk.gov.hmrc" %% "simple-reactivemongo" % "7.31.0-play-27",
  "uk.gov.hmrc" %% "bootstrap-backend-play-27"    % "4.2.0"
)

def test(scope: String = "test"): Seq[ModuleID] = Seq(
  "org.scalatest"           %% "scalatest"          % "3.0.8"             % scope,
  "org.pegdown"             % "pegdown"             % "1.6.0"             % scope,
  "com.typesafe.play"       %% "play-test"          % PlayVersion.current % scope,
  "org.scalatestplus.play"  %% "scalatestplus-play" % "4.0.3"             % scope,
  "org.mockito"             % "mockito-all"         % "1.10.19"           % scope,
  "org.scalacheck"          %% "scalacheck"         % "1.15.4"            % scope,
  "com.github.tomakehurst"  % "wiremock"            % "2.26.3"            % scope,
  "com.github.tomakehurst"  % "wiremock-jre8"       % "2.26.3"            % scope
)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
  tests.map { test =>
    Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin) ++ plugins: _*)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(scalaVersion := "2.12.12")
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(RoutesKeys.routesImport := Seq.empty)
  .settings(
    fork in Test := true,
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .settings(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*FrontendAuditConnector.*;.*Routes.*;.*GuiceInjector;.*DataCacheConnector;" +
      ".*ControllerConfiguration;.*LanguageSwitchController",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
  .settings(resolvers ++= Seq(
    Resolver.bintrayRepo("hmrc", "releases"),
    Resolver.jcenterRepo
  ))
  .settings(
    majorVersion := 0
  )
  .settings(
    // ***************
    // Use the silencer plugin to suppress warnings from unused imports in compiled twirl templates
    scalacOptions += "-P:silencer:pathFilters=routes",
    scalacOptions += "-P:silencer:lineContentFilters=^\\w",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.1" cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % "1.7.1" % Provided cross CrossVersion.full
    )
    // ***************
  )
  .settings(
    scalacOptions += "-feature"
  )