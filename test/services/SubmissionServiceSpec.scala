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
import connectors.{FileUploadConnector, PDFConnector}
import models.{Envelope, File, Submission, SubmissionResponse}
import org.joda.time.LocalDate
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class SubmissionServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val mockFileUploadConnector = mock[FileUploadConnector]
  private val mockPDFConnector = mock[PDFConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFileUploadConnector)
    reset(mockPDFConnector)
  }

  private val submissionService = new SubmissionService(mockFileUploadConnector, mockPDFConnector)(as)
  private val fakeSubmission = Submission("pdf", "metadata", "xml")

  protected def removeExtension(fileName: String): String = fileName.split("\\.").head
  protected def responseReference(envelopeId: String) = s"$envelopeId-SubmissionCTR-${LocalDate.now().toString("YYYYMMdd")}"
  protected def pdfFileName(envelopeId: String) = s"$envelopeId-SubmissionCTR-${LocalDate.now().toString("YYYYMMdd")}-pdf.pdf"
  protected def xmlFileName(envelopeId: String) = s"$envelopeId-SubmissionCTR-${LocalDate.now().toString("YYYYMMdd")}-robot.xml"
  protected def metadataFileName(envelopeId: String) = s"$envelopeId-SubmissionCTR-${LocalDate.now().toString("YYYYMMdd")}-metadata.xml"

  private val envelopeId = "env123"
  private val byteArray: Array[Byte] = "test".getBytes
  private val testHtml = "<html></html>"

  private val threeAvailableFiles = Seq(File("file1", "AVAILABLE"), File("file2", "AVAILABLE"), File("file3", "AVAILABLE"))
  private val threeFiles = Seq(File("file1", "AVAILABLE"), File("file2", "QUARANTINED"), File("file3", "AVAILABLE"))
  private val twoFiles = Seq(File("file1", "AVAILABLE"), File("file2", "AVAILABLE"))

  private val envelopeWithThreeAvailableFiles = Envelope(envelopeId, None, "OPEN", Some(threeAvailableFiles))
  private val envelopeWithThreeFiles = Envelope(envelopeId, None, "OPEN", Some(threeFiles))
  private val envelopeWithTwoFiles = Envelope(envelopeId, None, "OPEN", Some(twoFiles))
  private val closedEnvelopeWithThreeFiles = Envelope(envelopeId, None, "CLOSED", Some(threeFiles))

  "Submit" must {
    "return a submission response" when {
      "given valid inputs" in {
        val metadata = fakeSubmission.metadata.getBytes
        val xml = fakeSubmission.xml.getBytes
        val pdf = fakeSubmission.pdf.getBytes

        when(mockFileUploadConnector.createEnvelope(any())) thenReturn Future.successful(envelopeId)
        when(mockFileUploadConnector.envelopeSummary(any(), any(), any())(any(), any())) thenReturn Future.successful(envelopeWithThreeFiles)
        when(mockPDFConnector.generatePDF(any())) thenReturn Future.successful(pdf)

        when(mockFileUploadConnector.uploadFile(any(), eqTo(metadataFileName(envelopeId)), eqTo("application/xml"), any(), any())(any())) thenReturn Future.successful(HttpResponse(200))
        when(mockFileUploadConnector.uploadFile(any(), eqTo(xmlFileName(envelopeId)), eqTo("application/xml"), any(), any())(any())) thenReturn Future.successful(HttpResponse(200))
        when(mockFileUploadConnector.uploadFile(any(), any(), eqTo("application/pdf"), any(), any())(any())) thenReturn Future.successful(HttpResponse(200))

        val result: Future[SubmissionResponse] = submissionService.submit(fakeSubmission)

        whenReady(result) {
          result =>
            result mustBe SubmissionResponse(envelopeId, responseReference(envelopeId))
            verify(mockFileUploadConnector, times(1)).uploadFile(metadata, metadataFileName(envelopeId), "application/xml", envelopeId, removeExtension(metadataFileName(envelopeId)))
            verify(mockFileUploadConnector, times(1)).uploadFile(xml, xmlFileName(envelopeId), "application/xml", envelopeId, removeExtension(xmlFileName(envelopeId)))
            verify(mockFileUploadConnector, times(1)).uploadFile(pdf, pdfFileName(envelopeId), "application/pdf", envelopeId, removeExtension(pdfFileName(envelopeId)))
        }
      }
    }

    "return an error" when {
      "submit fails" in {
        when(mockFileUploadConnector.createEnvelope(any())) thenReturn Future.failed(new RuntimeException)
        when(mockFileUploadConnector.envelopeSummary(any(), any(), any())(any(), any())) thenReturn Future.successful(envelopeWithThreeFiles)
        when(mockPDFConnector.generatePDF(any())) thenReturn Future.successful(byteArray)

        val result: Future[SubmissionResponse] = submissionService.submit(fakeSubmission)

        whenReady(result.failed) {
          result =>
            result mustBe a[RuntimeException]
        }
      }

      "given a closed envelope" in {
        when(mockFileUploadConnector.createEnvelope(any())) thenReturn Future.successful(envelopeId)
        when(mockFileUploadConnector.envelopeSummary(any(), any(), any())(any(), any())) thenReturn Future.successful(closedEnvelopeWithThreeFiles)
        when(mockPDFConnector.generatePDF(any())) thenReturn Future.successful(byteArray)

        val result: Future[SubmissionResponse] = submissionService.submit(fakeSubmission)

        whenReady(result.failed) {
          result =>
            result mustBe a[RuntimeException]
        }
      }
    }
  }


  "fileUploadCallback" must {
    "return an envelopeId string when Envelope status is OPEN" in {
      when(mockFileUploadConnector.envelopeSummary(any(), any(), any())(any(), any())) thenReturn Future.successful(Envelope(envelopeId, None, "OPEN", None))

      val result: Future[String] = submissionService.fileUploadCallback(envelopeId)

      whenReady(result) {
        result =>
          result mustBe envelopeId
      }
    }

    "return an envelopeId string when Envelope status is something other than OPEN" in {
      when(mockFileUploadConnector.envelopeSummary(any(), any(), any())(any(), any())) thenReturn Future.successful(Envelope(envelopeId, None, "INFECTED", None))

      val result: Future[String] = submissionService.fileUploadCallback(envelopeId)

      whenReady(result) {
        result =>
          result mustBe envelopeId
      }
    }

    "run closeEnvelope once if count of file status AVAILABLE == 3" in {
      when(mockFileUploadConnector.envelopeSummary(any(), any(), any())(any(), any())) thenReturn Future.successful(envelopeWithThreeAvailableFiles)
      when(mockFileUploadConnector.closeEnvelope(any())(any())) thenReturn Future.successful(envelopeId)

      val result = submissionService.fileUploadCallback(envelopeId)

      whenReady(result) {
        _ =>
          verify(mockFileUploadConnector, times(1)).closeEnvelope(envelopeId)
      }
    }

    "not run closeEnvelope once if count of file status AVAILABLE < 3" in {
      when(mockFileUploadConnector.envelopeSummary(any(), any(), any())(any(), any())) thenReturn Future.successful(envelopeWithTwoFiles)

      val result = submissionService.fileUploadCallback(envelopeId)

      whenReady(result) {
        _ =>
          verify(mockFileUploadConnector, times(0)).closeEnvelope("env456")
      }
    }

    "not run closeEnvelope once if count of file status AVAILABLE != 3" in {
      when(mockFileUploadConnector.envelopeSummary(any(), any(), any())(any(), any())) thenReturn Future.successful(envelopeWithThreeFiles)

      val result = submissionService.fileUploadCallback(envelopeId)

      whenReady(result) {
        _ =>
          verify(mockFileUploadConnector, times(0)).closeEnvelope(envelopeId)
      }
    }
  }
}
