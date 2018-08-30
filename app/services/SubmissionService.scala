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

import com.google.inject.{Inject, Singleton}
import connectors.FileUploadConnector
import models.{Submission, SubmissionResponse}
import org.joda.time.LocalDate
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class SubmissionService @Inject()(
                                 val fileUploadConnector: FileUploadConnector
                                 )(implicit val hc: HeaderCarrier){

  protected def fileName(envelopeId: String, fileType: String) = s"$envelopeId-SubmissionCTR-${LocalDate.now().toString("YYYYMMdd")}-$fileType"

  def submit(submission: Submission): Future[SubmissionResponse] = {

    val result = for {
      envelopeId: String <- fileUploadConnector.createEnvelope
    } yield {

      SubmissionResponse(envelopeId, fileName(envelopeId, "pdf"))
    }

    result.recoverWith{
      case e: Exception =>
        Future.failed(new RuntimeException("Submit failed", e))
    }
  }
}
