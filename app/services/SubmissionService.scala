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

package services

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import connectors.{FileUploadConnector, PDFConnector}
import models.{Envelope, Submission, SubmissionResponse}
import org.joda.time.LocalDate
import play.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class SubmissionService @Inject()(
                                   val fileUploadConnector: FileUploadConnector,
                                   val pdfConnector: PDFConnector
                                 )(implicit as: ActorSystem) {

  import scala.concurrent.ExecutionContext.Implicits.global

  protected def removeExtension(fileName: String): String = fileName.split("\\.").head
  protected def responseReference(envelopeId: String) = s"$envelopeId-SubmissionCTR-${LocalDate.now().toString("YYYYMMdd")}"
  protected def pdfFileName(envelopeId: String) = s"$envelopeId-SubmissionCTR-${LocalDate.now().toString("YYYYMMdd")}-iform.pdf"
  protected def xmlFileName(envelopeId: String) = s"$envelopeId-SubmissionCTR-${LocalDate.now().toString("YYYYMMdd")}-robotic.xml"
  protected def metadataFileName(envelopeId: String) = s"$envelopeId-SubmissionCTR-${LocalDate.now().toString("YYYYMMdd")}-metadata.xml"

  def submit(submission: Submission)(implicit hc: HeaderCarrier): Future[SubmissionResponse] = {

    val result = for {
      pdf: Array[Byte] <- pdfConnector.generatePDF(submission.pdf)
      envelopeId: String <- fileUploadConnector.createEnvelope
      envelope: Envelope <- fileUploadConnector.envelopeSummary(envelopeId)
    } yield {

      envelope.status match {
        case "OPEN" =>
          fileUploadConnector.uploadFile(
            byteArray = submission.metadata.getBytes,
            fileName = metadataFileName(envelopeId),
            contentType = "application/xml",
            envelopeId = envelopeId,
            fileId = removeExtension(metadataFileName(envelopeId))
          )

          fileUploadConnector.uploadFile(
            byteArray = submission.xml.getBytes,
            fileName = xmlFileName(envelopeId),
            contentType = "application/xml",
            envelopeId = envelopeId,
            fileId = removeExtension(xmlFileName(envelopeId))
          )

          fileUploadConnector.uploadFile(
            byteArray = pdf,
            fileName = pdfFileName(envelopeId),
            contentType = "application/pdf",
            envelopeId = envelopeId,
            fileId = removeExtension(pdfFileName(envelopeId))
          )

        case _ =>
          Future.failed(throw new RuntimeException)
      }

      SubmissionResponse(envelopeId, responseReference(envelopeId))
    }


    result.onFailure {
      case e =>
        Logger.error("[SubmissionService][submit] submit failed: ", e)
    }

    result
  }

  def fileUploadCallback(envelopeId: String)(implicit hc: HeaderCarrier): Future[String] = {
    fileUploadConnector.envelopeSummary(envelopeId, 1, 5).flatMap {
      envelope =>
        envelope.status match {
          case "OPEN" =>
            envelope.files match {
              case Some(files) if files.count(file => file.status == "AVAILABLE") == 3 =>
                fileUploadConnector.closeEnvelope(envelopeId)
              case _ =>
                Logger.info("[SubmissionService][fileUploadCallback] incomplete, waiting for files")
                Future.successful(envelopeId)
            }
          case _ =>
            Logger.error(s"[SubmissionService][fileUploadCallback] envelope: $envelopeId not open instead status: ${envelope.status}")
            Future.successful(envelopeId)
        }
    }
  }


}
