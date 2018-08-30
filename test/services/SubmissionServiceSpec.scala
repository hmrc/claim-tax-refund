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

package services

import config.SpecBase
import connectors.FileUploadConnector
import models.{Submission, SubmissionResponse}
import org.joda.time.LocalDate
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class SubmissionServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  private val mockFileUploadConnector = mock[FileUploadConnector]
  private val mockSubmission = Submission("pdf", "metadata", "xml")
  private val envelopeId = "123"
  private val fileName = s"123-SubmissionCTR-${LocalDate.now().toString("YYYYMMdd")}-pdf"
  implicit val hc: HeaderCarrier = HeaderCarrier()

  object Service extends SubmissionService(mockFileUploadConnector)

  "Submit" must {
    "return a submission response" when {
      "given valid inputs" in {
        when(mockFileUploadConnector.createEnvelope) thenReturn Future.successful(envelopeId)

        val result: Future[SubmissionResponse] = Service.submit(mockSubmission)

        verify(mockFileUploadConnector, times(1)).createEnvelope

        result.map {
          submissionResponse =>
            submissionResponse mustBe Future.successful(SubmissionResponse(envelopeId, fileName))
        }

      }
    }

    "return an error" when {
      "submit fails" in {
        when(mockFileUploadConnector.createEnvelope) thenReturn Future.failed(new RuntimeException)

        val result: Future[SubmissionResponse] = Service.submit(mockSubmission)

        whenReady(result.failed) {
          result =>
            result.getMessage mustBe "Submit failed"
            result mustBe a[RuntimeException]
        }
      }
    }
  }

}
