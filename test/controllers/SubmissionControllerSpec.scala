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
import connectors.FileUploadConnector
import models._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.IntegrationPatience
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import services.SubmissionService

import scala.concurrent.Future

class SubmissionControllerSpec
  extends SpecBase
    with IntegrationPatience
    with BeforeAndAfterEach {

  private val mockFileUploadConnector = mock[FileUploadConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFileUploadConnector)
  }

  private val mockSubmissionService: SubmissionService = mock[SubmissionService]
  private val submissionResponse = SubmissionResponse("12345", "12345-SubmissionCTR-20171023-iform.pdf")
  private val mockSubmission = Submission("pdf", "metadata", "xml")

  private def envelope(envId: String, fileId: String, status: String): Envelope = Envelope(envId, Some(fileId), status, None)

  private val fileStatuses: Gen[String] = Gen.oneOf("AVAILABLE", "QUARANTINED", "CLEANED", "INFECTED", "ERROR")

  private val uuid: Gen[String] = Gen.uuid.map(_.toString)

  implicit lazy val materializer: Materializer = app.materializer

  private val fakeRequest = FakeRequest(
    method = "POST",
    uri = "",
    headers = FakeHeaders(Seq("Content-type" -> "application/json")),
    body = Json.toJson(mockSubmission)
  )

  private def fakeCallbackRequestAvailable(envId: String, fileId: String, status: String) = FakeRequest(
    method = "POST",
    uri = "",
    headers = FakeHeaders(Seq("Content-type" -> "application/json")),
    body = Json.toJson(CallbackRequest(envId, fileId, status))
  )

  private val validData = Json.parse(
    """
      			|{
      			|   "id": "12345",
      			|   "filename": "12345-SubmissionCTR-20171023-iform.pdf"
      			|}
      			|""".stripMargin)

  private val response = for {
    envelopeId <- uuid
    fileId <- uuid
    status <- fileStatuses
  } yield {
    CallbackRequest(envelopeId, fileId, status, None)
  }

  def controller() = new SubmissionController(mockSubmissionService)

  "Submit" must {
    "return Ok with a envelopeId status" when {
      "valid payload is submitted" in {
        when(mockSubmissionService.submit(eqTo(mockSubmission))(any())) thenReturn Future.successful(submissionResponse)
        val result: Future[Result] = Helpers.call(controller().submit(), fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe validData
      }
    }

    "return 500" when {
      "invalid payload is submitted" in {
        when(mockSubmissionService.submit(eqTo(mockSubmission))(any())) thenReturn Future.failed(new Exception)
        val result: Future[Result] = Helpers.call(controller().submit(), fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "Callback" must {
    "return a 200 response status" when {
      "available callback response" in {
        forAll(response) {
          res =>
            when(mockFileUploadConnector.createEnvelope(any())) thenReturn Future.successful(res.envelopeId)
            when(mockFileUploadConnector.envelopeSummary(eqTo(res.envelopeId), eqTo(1), eqTo(5))(any(), any())) thenReturn Future.successful(envelope(res.envelopeId, res.fileId, res.status))
            when(mockSubmissionService.fileUploadCallback(eqTo(res.envelopeId))(any())) thenReturn Future.successful(res.envelopeId)

            val callback: Future[Result] = Helpers.call(controller().callback(), fakeCallbackRequestAvailable(res.envelopeId, res.fileId, res.status))

            whenReady(callback) {
              result =>
                result.header.status mustBe OK
            }
        }
      }
    }


    "call submissionService.fileUploadCallback" when {
      "file status is AVAILABLE" in {
        forAll(response, minSuccessful(10), maxDiscardedFactor(20.0)) {
          res =>
            when(mockFileUploadConnector.createEnvelope(any())) thenReturn Future.successful(res.envelopeId)
            when(mockFileUploadConnector.envelopeSummary(eqTo(res.envelopeId), eqTo(1), eqTo(5))(any(), any())) thenReturn Future.successful(envelope(res.envelopeId, res.fileId, res.status))
            when(mockSubmissionService.fileUploadCallback(eqTo(res.envelopeId))(any())) thenReturn Future.successful(res.envelopeId)

            val callback: Future[Result] = Helpers.call(controller().callback(), fakeCallbackRequestAvailable(res.envelopeId, res.fileId, res.status))

            whenReady(callback) {
              _ =>
                whenever(res.status == "AVAILABLE") {
                  verify(mockSubmissionService, times(1)).fileUploadCallback(eqTo(res.envelopeId))(any())
                }
            }
        }
      }
    }

    "not call submissionService.fileUploadCallback" when {
      "file status is not AVAILABLE" in {
        forAll(response, minSuccessful(10), maxDiscardedFactor(20.0)) {
          res =>
            when(mockFileUploadConnector.createEnvelope(any())) thenReturn Future.successful(res.envelopeId)
            when(mockFileUploadConnector.envelopeSummary(eqTo(res.envelopeId), eqTo(1), eqTo(5))(any(), any())) thenReturn Future.successful(envelope(res.envelopeId, res.fileId, res.status))
            when(mockSubmissionService.fileUploadCallback(eqTo(res.envelopeId))(any())) thenReturn Future.successful(res.envelopeId)

            val callback: Future[Result] = Helpers.call(controller().callback(), fakeCallbackRequestAvailable(res.envelopeId, res.fileId, res.status))

            whenReady(callback) {
              _ =>
                whenever(res.status != "AVAILABLE") {
                  verify(mockSubmissionService, times(0)).fileUploadCallback(eqTo(res.envelopeId))(any())
                }
            }
        }
      }
    }
  }
}
