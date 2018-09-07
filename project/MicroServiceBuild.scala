import sbt._
import play.core.PlayVersion
import play.sbt.PlayImport._

object MicroServiceBuild extends Build with MicroService {

  val appName = "claim-tax-refund"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  private val playReactivemongoVersion = "5.2.0"
  private val bootstrapVersion = "1.7.0"
  private val scalaTestPlusPlayVersion = "2.0.1"
  private val mockitoAllVersion = "1.10.19"
  private val wireMockVersion = "2.15.0"
  private val scalacheckVersion = "1.13.4"

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-reactivemongo" % playReactivemongoVersion,
    "uk.gov.hmrc" %% "bootstrap-play-25" % bootstrapVersion
  )

  def test(scope: String = "test,it") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.0.0" % scope,
    "org.scalatest" %% "scalatest" % "2.2.6" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
    "org.mockito" % "mockito-all" % mockitoAllVersion % scope,
    "org.scalacheck" %% "scalacheck" % scalacheckVersion % scope,
    "com.github.tomakehurst" % "wiremock" % wireMockVersion % scope
  )
}
