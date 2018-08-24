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

package connectors

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import com.kenshoo.play.metrics.Metrics
import config.MicroserviceAppConfig
import play.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.HttpResponseHelper
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class FileUploadConnector @Inject()(
                                     appConfig: MicroserviceAppConfig,
                                     val httpClient: HttpClient,
                                     val WSClient: WSClient,
                                     val metrics: Metrics
                                   )(implicit as: ActorSystem) extends HttpResponseHelper {

  private val callbackUrl: String = appConfig.fileUploadCallbackUrl
  private val fileUploadUrl: String = appConfig.fileUploadUrl
  private val fileUploadFrontEndUrl: String = appConfig.fileUploadFrontendUrl

  private val firstRetryMilliseconds : Int = appConfig.firstRetryMilliseconds
  private val maxAttemptNumber: Int = appConfig.maxAttemptNumber

  def routingRequest(envelopeId: String): JsValue = Json.obj(
    "envelopeId" -> envelopeId,
    "application" -> "CTR",
    "destination" -> "DMS"
  )

  def createEnvelopeBody: JsValue = Json.obj("callbackUrl" -> callbackUrl)

  def createEnvelope(implicit hc: HeaderCarrier): Future[String] = {

    val result: Future[String] = httpClient.POST[JsValue, HttpResponse](s"$fileUploadUrl/file-upload/envelopes", createEnvelopeBody).flatMap { response =>

      response.status match {
        case CREATED =>
          envelopeId(response).map(Future.successful).getOrElse {
            Future.failed(new RuntimeException("No envelope id returned by file upload service"))
          }
        case _ =>
          Future.failed(new RuntimeException(s"failed to create envelope with status [${response.status}]"))
      }
    }

    result.onFailure {
      case e =>
        Logger.error("[FileUploadConnector][createEnvelope] - call to create envelope failed", e)
    }

    result
  }

  private def envelopeId(response: HttpResponse): Option[String] = {
    response.header("Location").map(
      path =>
        path.split("/").reverse.head
    )
  }
}
