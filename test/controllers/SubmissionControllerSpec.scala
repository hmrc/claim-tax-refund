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
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class SubmissionControllerSpec extends SpecBase with MockitoSugar {
  val mockSubmissionService: SubmissionService = mock[SubmissionService]
  val submissionResponse = SubmissionResponse("12345", "12345-SubmissionCTR-20171023-iform.pdf")
  val mockSubmission = Submission("pdf", "metadata", "xml")
  implicit lazy val materializer: Materializer = app.materializer
  implicit val hc: HeaderCarrier = HeaderCarrier()

  def controller() = new SubmissionController(mockSubmissionService)

  "Submission controller" must {
    "return Ok with a envelopeId status" when {
      "valid payload is submitted" in {
        when(mockSubmissionService.submit(mockSubmission)) thenReturn Future.successful(SubmissionResponse("123", "asdf"))
        val result: Future[Result] = Helpers.call(controller().submit(), FakeRequest("POST", "/submit"))

        status(result) mustBe OK
        contentAsString(result) mustBe "123"
      }
    }

    "return 500" when {
      "invalid payload is submitted" in {
        when(mockSubmissionService.submit(mockSubmission)) thenReturn Future.failed(new Throwable)
        val result: Future[Result] = Helpers.call(controller().submit(), FakeRequest("POST", "/submit"))

        result mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
