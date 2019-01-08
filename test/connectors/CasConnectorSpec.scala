/*
 * Copyright 2019 HM Revenue & Customs
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
import models.{SubmissionArchiveRequest, SubmissionArchiveResponse}
import org.scalatest.concurrent.IntegrationPatience
import play.api.Application
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.HttpException
import util.WireMockHelper

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class CasConnectorSpec extends SpecBase with IntegrationPatience with WireMockHelper {

  override implicit lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(
        conf = "microservice.services.dmsapi.port" -> server.port
      )
      .build()

  private lazy val casConnector: CasConnector =
    app.injector.instanceOf[CasConnector]

  private val submissionArchiveRequest: SubmissionArchiveRequest = SubmissionArchiveRequest("", "", "", "")

  "ArchiveSubmission" must {
    "return a 200 and casKey when sending a valid SubmissionArchiveRequest" in {
      server.stubFor(
        post(urlEqualTo(s"/digital-form/archive/123"))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withBody("""{"casKey": "cas-1234"}""")
          )
      )

      val result = casConnector.archiveSubmission("123", submissionArchiveRequest)

      whenReady(result) {
        result =>
          result mustBe SubmissionArchiveResponse("cas-1234")
      }
    }

    "return an exception when unable to validate" in {
      server.stubFor(
        post(urlEqualTo(s"/digital-form/archive/123"))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withBody("""{"wrongKey": "badValue"}""")
          )
      )

      whenReady(casConnector.archiveSubmission("123", submissionArchiveRequest).failed) {
        exception =>
          exception.getMessage mustBe "[CasConnector][archiveSubmission] not a valid submission archive response"
      }
    }

    "return a http exception when bad request" in {
      server.stubFor(
        post(urlEqualTo(s"/digital-form/archive/123"))
          .willReturn(
            aResponse()
              .withStatus(Status.BAD_REQUEST)
          )
      )

      val result = casConnector.archiveSubmission("123", submissionArchiveRequest)

      the[HttpException] thrownBy Await.result(result, 5 seconds)
    }
  }

}
