/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import javax.inject.Singleton
import com.google.inject.{ImplementedBy, Inject}
import config.MicroserviceAppConfig
import models.{SubmissionArchiveRequest, SubmissionArchiveResponse}
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CasConnectorImpl @Inject()(appConfig: MicroserviceAppConfig, val http: HttpClient) extends CasConnector {

  private val logger = play.api.Logger(classOf[CasConnectorImpl])

  def archiveSubmission(submissionRef: String, data: SubmissionArchiveRequest)(implicit hc: HeaderCarrier, ec:ExecutionContext): Future[SubmissionArchiveResponse] = {
    logger.debug(s"Sending submission $submissionRef to CAS via DMS API")

    val url: String = s"${appConfig.dmsApiUrl}/digital-form/archive/$submissionRef"
    val result: Future[SubmissionArchiveResponse] = http.POST[SubmissionArchiveRequest, HttpResponse](url, data).flatMap {
      response =>
        response.status match {
          case OK =>
            response.json.validate[SubmissionArchiveResponse].map(Future.successful).getOrElse {
              Future.failed(new RuntimeException("[CasConnector][archiveSubmission] not a valid submission archive response"))
            }
          case _ =>
            Future.failed(new RuntimeException(s"[CasConnector][archiveSubmission] failed to archive submission with status [${response.status}]"))
        }
    }

    result.failed.foreach {
      case e =>
        logger.error("[CasConnector][archiveSubmission] call to archive submission failed", e)
    }

    result
  }
}

@ImplementedBy(classOf[CasConnectorImpl])
trait CasConnector {
  def archiveSubmission(submissionRef: String, data: SubmissionArchiveRequest)(implicit hc: HeaderCarrier, ec:ExecutionContext): Future[SubmissionArchiveResponse]
}
