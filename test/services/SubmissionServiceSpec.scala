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
import org.mockito.Mockito._
import org.scalacheck.{Gen, Shrink}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class SubmissionServiceSpec extends SpecBase with MockitoSugar with ScalaFutures with PropertyChecks {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny


  private val mockFileUploadConnector = mock[FileUploadConnector]
  private val mockSubmission = Submission("pdf", "metadata", "xml")
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val as: ActorSystem = ActorSystem()

  private val uuid: Gen[String] = Gen.uuid.map(_.toString)

  private val envelopeStatuses: Gen[String] = Gen.oneOf("OPEN", "CLOSED", "SEALED", "DELETED")

  private val fileStatuses: Gen[String] = Gen.oneOf("AVAILABLE", "QUARANTINED", "CLEANED", "INFECTED")
  private val file = for {
    name <- uuid
    status <- fileStatuses
  } yield {
    File(name, status)
  }
  private val files: Gen[Seq[File]] = Gen.listOf(file)

  private val envelope = for {
    envId <- uuid
    status <- envelopeStatuses
    files <- files
  } yield {
    Envelope(envId, None, status, Some(files))
  }


  object Service extends SubmissionService(mockFileUploadConnector)

  "Submit" must {
    "return a submission response" when {
      "given valid inputs" in {

        forAll(uuid, envelope) {

          (envId, env) =>

          when(mockFileUploadConnector.createEnvelope) thenReturn Future.successful(envId)
          when(mockFileUploadConnector.envelopeSummary(envId)) thenReturn Future.successful(env)

          val result: Future[SubmissionResponse] = Service.submit(mockSubmission)

          result.map {
            submissionResponse =>
              submissionResponse mustBe Future.successful(SubmissionResponse(envId, env.files.get.head.name))
          }

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

/*
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
      when(Service.fileUploadCallback(envelopeId)) thenReturn Future.successful(envelopeId)

      verify(mockFileUploadConnector, times(1)).closeEnvelope(eqTo(envelopeId))(eqTo(hc))
    }
  }
*/

}
