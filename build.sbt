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
val bootstrapVersion = "9.1.0"
ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.12"

val compile: Seq[ModuleID] = Seq(
  ws,
  "uk.gov.hmrc" %% "bootstrap-backend-play-30"    % bootstrapVersion
)

def test(scope: String = "test"): Seq[ModuleID] = Seq(
  "uk.gov.hmrc"             %% "bootstrap-test-play-30"   % bootstrapVersion,
  "org.scalatestplus"       %% "scalatestplus-mockito"    % "1.0.0-M2",
  "org.scalatestplus"       %% "scalacheck-1-17"          % "3.2.16.0"
).map(_ % scope)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(play.sbt.PlayScala, SbtDistributablesPlugin) ++ plugins: _*)
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
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
    isPublicArtefact := true
  )
  .settings(
    scalacOptions += "-Wconf:cat=unused-imports&src=routes/.*:s"
  )
  .settings(
    scalacOptions += "-feature"
  )