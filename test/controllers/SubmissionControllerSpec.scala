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

package controllers

import akka.stream.Materializer
import config.SpecBase
import models.{Submission, SubmissionResponse}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import services.SubmissionService

import scala.concurrent.Future

class SubmissionControllerSpec extends SpecBase with MockitoSugar {
  private val mockSubmissionService: SubmissionService = mock[SubmissionService]
  private val submissionResponse = SubmissionResponse("12345", "12345-SubmissionCTR-20171023-iform.pdf")
  private val mockSubmission = Submission("pdf", "metadata", "xml")
  implicit lazy val materializer: Materializer = app.materializer

  private val fakeRequest = FakeRequest(
    method = "POST",
    uri = "",
    headers = FakeHeaders(Seq("Content-type" -> "application/json")),
    body = Json.toJson(mockSubmission)
  )

  private val validData = Json.parse(
    """
      |{
      |   "id": "12345",
      |   "filename": "12345-SubmissionCTR-20171023-iform.pdf"
      |}
      |""".stripMargin)

  def controller() = new SubmissionController(mockSubmissionService)

  "Submission controller" must {
    "return Ok with a envelopeId status" when {
      "valid payload is submitted" in {
        when(mockSubmissionService.submit(mockSubmission)) thenReturn Future.successful(submissionResponse)
        val result: Future[Result] = Helpers.call(controller().submit(), fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe validData
      }
    }

    "return 500" when {
      "invalid payload is submitted" in {
        when(mockSubmissionService.submit(mockSubmission)) thenReturn Future.failed(new Exception)
        val result: Future[Result] = Helpers.call(controller().submit(), fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
