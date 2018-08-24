/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import akka.actor.ActorSystem
import com.github.tomakehurst.wiremock.client.WireMock._
import config.SpecBase
import models.File
import org.scalacheck.{Gen, Shrink}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.http.Status
import uk.gov.hmrc.http.HeaderCarrier
import util.WireMockHelper

class FileUploadConnectorSpec extends SpecBase with WireMockHelper with GuiceOneAppPerSuite with ScalaFutures with PropertyChecks with IntegrationPatience {
  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  private val as = ActorSystem()

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.file-upload.port" -> server.port,
        "microservice.services.file-upload-frontend.port" -> server.port
      )
      .build()

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val connector: FileUploadConnector =
    app.injector.instanceOf[FileUploadConnector]

  private val statuses: Gen[Int] =
    Gen.chooseNum(
      200, 599,
      400, 499, 500
    )

  private val uuid: Gen[String] = Gen.uuid.map(_.toString)

  private val envelopeStatuses: Gen[String] = Gen.oneOf("OPEN", "CLOSED", "SEALED", "DELETED")

  private val fileStatuses: Gen[String] = Gen.oneOf("AVAILABLE", "QUARANTINED", "CLEANED", "INFECTED")

  private val file: Gen[File] = for {
    name <- uuid
    status <- fileStatuses
  } yield {
    File(name, status)
  }

  private val files: Gen[Seq[File]] = Gen.listOf(file)

  "createEnvelope" must {
    "return an envelope id" in {
      forAll(uuid) {
        envId =>
          server.stubFor(
            post(urlEqualTo("/file-upload/envelopes"))
              .willReturn(
                aResponse()
                  .withHeader("Location", s"file-upload/envelope/$envId")
                  .withStatus(Status.CREATED)
              )
          )

          whenReady(connector.createEnvelope) {
            result =>
              result mustBe envId
          }
      }
    }
  }
}
