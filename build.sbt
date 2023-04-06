import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import play.sbt.routes.RoutesKeys


lazy val appDependencies: Seq[ModuleID] = compile ++ test()
lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty

val appName = "claim-tax-refund"
val bootstrapVersion = "7.15.0"

val compile = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-backend-play-28"    % bootstrapVersion
)

def test(scope: String = "test"): Seq[ModuleID] = Seq(
  "uk.gov.hmrc"             %% "bootstrap-test-play-28"   % bootstrapVersion,
  "org.scalatestplus"       %% "scalatestplus-mockito"    % "1.0.0-M2",
  "org.scalatestplus"       %% "scalatestplus-scalacheck" % "3.1.0.0-RC2"
).map(_ % scope)

def oneForkedJvmPerTest(tests: Seq[TestDefinition]): Seq[Group] =
  tests.map { test =>
    Group(test.name, Seq(test), SubProcess(ForkOptions().withRunJVMOptions(Vector(s"-Dtest.name=${test.name}"))))
  }

val silencerVersion  = "1.7.12"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin) ++ plugins: _*)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(scalaVersion := "2.13.8")
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(RoutesKeys.routesImport := Seq.empty)
  .settings(
    fork in Test := true,
    libraryDependencies ++= appDependencies,
    retrieveManaged := true
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
    Resolver.jcenterRepo
  ))
  .settings(
    majorVersion := 0
  )
  .settings(
    isPublicArtefact := true
  )
  .settings(
    // ***************
    // Use the silencer plugin to suppress warnings from unused imports in compiled twirl templates
    scalacOptions += "-P:silencer:pathFilters=routes",
    scalacOptions += "-P:silencer:lineContentFilters=^\\w",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
    // ***************
  )
  .settings(
    scalacOptions += "-feature"
  )