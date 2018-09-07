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
import models.{Envelope, File, Submission, SubmissionResponse}
import org.mockito.Mockito._
import org.scalacheck.Gen

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionServiceSpec extends SpecBase {

  private val submissionService = new SubmissionService(mockFileUploadConnector)(as)
  private val fakeSubmission = Submission("pdf", "metadata", "xml")

  private val uuid: Gen[String] = Gen.uuid.map(_.toString)
  private val envelopeStatuses: Gen[String] = Gen.oneOf("OPEN", "CLOSED", "SEALED", "DELETED")
  private val fileStatuses: Gen[String] = Gen.oneOf("AVAILABLE", "QUARANTINED", "CLEANED", "INFECTED")

  private val threeAvailableFiles = Seq(File("Blah1", "AVAILABLE"), File("Blah2", "AVAILABLE"), File("Blah3", "AVAILABLE"))
  private val threeFiles = Seq(File("Blah1", "AVAILABLE"), File("Blah2", "QUARANTINED"), File("Blah3", "AVAILABLE"))
  private val twoFiles = Seq(File("Blah1", "AVAILABLE"), File("Blah2", "AVAILABLE"))

  private val envelopeWithThreeAvailableFiles = Envelope("env123", None, "OPEN", Some(threeAvailableFiles))
  private val envelopeWithThreeFiles = Envelope("env123", None, "OPEN", Some(threeFiles))
  private val envelopeWithTwoFiles = Envelope("env456", None, "OPEN", Some(twoFiles))

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

  "Submit" must {
    "return a submission response" when {
      "given valid inputs" in {

        forAll(uuid, envelope) {
          (envId, env) =>
            when(mockFileUploadConnector.createEnvelope) thenReturn Future.successful(envId)
            when(mockFileUploadConnector.envelopeSummary(envId)) thenReturn Future.successful(env)

            val result: Future[SubmissionResponse] = submissionService.submit(fakeSubmission)

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

        val result: Future[SubmissionResponse] = submissionService.submit(fakeSubmission)

        whenReady(result.failed) {
          result =>
            result.getMessage mustBe "Submit failed"
            result mustBe a[RuntimeException]
        }
      }
    }
  }


  "fileUploadCallback" must {
    "return an envelopeId string when Envelope status is OPEN" in {
      when(mockFileUploadConnector.envelopeSummary("env123", 1, 5)) thenReturn Future.successful(Envelope("env123", None, "OPEN", None))

      val result: Future[String] = submissionService.fileUploadCallback("env123")

      whenReady(result) {
        result =>
          result mustBe "env123"
      }
    }

    "return an envelopeId string when Envelope status is something other than OPEN" in {
      when(mockFileUploadConnector.envelopeSummary("env123", 1, 5)) thenReturn Future.successful(Envelope("env123", None, "INFECTED", None))

      val result: Future[String] = submissionService.fileUploadCallback("env123")

      whenReady(result) {
        result =>
          result mustBe "env123"
      }
    }

    "run closeEnvelope once if count of file status AVAILABLE == 3" in {
      when(mockFileUploadConnector.envelopeSummary("env123", 1, 5)) thenReturn Future.successful(envelopeWithThreeAvailableFiles)
      when(mockFileUploadConnector.closeEnvelope("env123")) thenReturn Future.successful("env123")

      val result = submissionService.fileUploadCallback("env123")

      whenReady(result) {
        _ =>
          verify(mockFileUploadConnector, times(1)).closeEnvelope("env123")
      }
    }

    "not run closeEnvelope once if count of file status AVAILABLE < 3" in {
      when(mockFileUploadConnector.envelopeSummary("env456", 1, 5)) thenReturn Future.successful(envelopeWithTwoFiles)
      val result = submissionService.fileUploadCallback("env456")

      whenReady(result) {
        _ =>
          verify(mockFileUploadConnector, times(0)).closeEnvelope("env456")
      }
    }

    "not run closeEnvelope once if count of file status AVAILABLE != 3" in {
      when(mockFileUploadConnector.envelopeSummary("env123", 1, 5)) thenReturn Future.successful(envelopeWithThreeFiles)
      val result = submissionService.fileUploadCallback("env123")

      whenReady(result) {
        _ =>
          verify(mockFileUploadConnector, times(0)).closeEnvelope("env123")
      }
    }
  }
}
