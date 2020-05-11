/*
 * Copyright 2020 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock._
import config.SpecBase
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HttpException
import util.WireMockHelper

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


class PDFConnectorSpec extends SpecBase with WireMockHelper {

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.pdf-generator-service.port" -> server.port
      )
      .build()

  private lazy val pdfConnector: PDFConnector =
    app.injector.instanceOf[PDFConnector]

  private val testHtml = "<html></html>"

  "PDFConnector" must {
    "return a 200 when valid html is submitted" in {
      server.stubFor(
        post(urlEqualTo("/pdf-generator-service/generate"))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withBody(testHtml.getBytes)
          )
      )
      val response = pdfConnector.generatePDF(testHtml)
      val result = Await.result(response, 5 seconds)
      result mustBe testHtml.getBytes
    }

    "return a 400 when no HTML is submitted" in {
      server.stubFor(
        post(urlEqualTo("/pdf-generator-service/generate"))
          .willReturn(
            aResponse()
              .withStatus(Status.BAD_REQUEST)
          )
      )
      val response = pdfConnector.generatePDF(testHtml)
      the[HttpException] thrownBy Await.result(response, 5 seconds)
    }
  }
}
