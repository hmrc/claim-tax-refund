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

import akka.actor.ActorSystem
import config.SpecBase
import connectors.FileUploadConnector
import models.{Envelope, File, Submission, SubmissionResponse}
import org.joda.time.LocalDate
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => eqTo, _}
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
  private val envelope = Envelope("12345", None, "OPEN", None)
  private val availableFiles =  Seq(File("one", "AVAILABLE"), File("two", "AVAILABLE"), File("three", "AVAILABLE"))
  private val envelopeWithFiles = Envelope(envelopeId, None, "OPEN", Some(availableFiles))
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val as: ActorSystem = ActorSystem()

  object Service extends SubmissionService(mockFileUploadConnector)

  "Submit" must {
    "return a submission response" when {
      "given valid inputs" in {
        when(mockFileUploadConnector.createEnvelope) thenReturn Future.successful(envelopeId)
        when(mockFileUploadConnector.envelopeSummary(envelopeId)) thenReturn
          Future.successful(Envelope("", Some(""), "OPEN", Some(Seq(File("", "")))))

        val result: Future[SubmissionResponse] = Service.submit(mockSubmission)

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

  "fileUploadCallback" must {
    "return an envelopeId string" in {
      when(mockFileUploadConnector.createEnvelope) thenReturn Future.successful(envelopeId)
      when(mockFileUploadConnector.envelopeSummary(envelopeId, 1, 5)) thenReturn Future.successful(envelope)

      val result: Future[String] = Service.fileUploadCallback(envelopeId)

      whenReady(result) {
        result =>
          result mustBe envelopeId
      }
    }

    "run closeEnvelope once if count of file status AVAILABLE == 3" in {
      when(mockFileUploadConnector.createEnvelope) thenReturn Future.successful(envelopeId)
      when(mockFileUploadConnector.envelopeSummary(envelopeId, 1, 5)) thenReturn Future.successful(envelopeWithFiles)

      verify(mockFileUploadConnector, times(1)).closeEnvelope(eqTo(envelopeId))(any())
    }
  }

}
