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

import java.util.concurrent.Callable
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.Patterns.after
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import com.google.inject.{Inject, Singleton}
import config.MicroserviceAppConfig
import jdk.nashorn.internal.objects.Global
import models.Envelope
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.http.logging.ConnectionTracing.formatNs
import uk.gov.hmrc.http.logging.LoggingDetails
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, StringContextOps, HttpResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.HttpResponseHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

@Singleton
class FileUploadConnector @Inject()(
                                     appConfig: MicroserviceAppConfig,
                                     val auditConnector: AuditConnector,
                                     val httpClient: HttpClientV2,
                                     val wsClient: WSClient
                                   )(implicit ec: ExecutionContext, as: ActorSystem)
  extends HttpResponseHelper
    with HttpAuditing {

  override def appName: String = appConfig.appName
  private val connectionLogger = Logger(this.getClass)

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
    val url = s"$fileUploadUrl/file-upload/envelopes"
    val result: Future[String] =
      httpClient
        .post(url"$url")
        .withBody(Json.toJson(createEnvelopeBody))
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case CREATED =>
              val res: Option[Future[String]] = envelopeId(response).map(Future.successful)
              connectionLogger.info(s"[FileUploadConnector][createEnvelope] Envelope created $res")
              res.getOrElse {
                Future.failed(new RuntimeException("No envelope id returned by file upload service"))
              }
            case _ =>
              Future.failed(new RuntimeException(s"failed to create envelope with status [${response.status}]"))
          }
    }

    result.failed.foreach {
      case e =>
        connectionLogger.error("[FileUploadConnector][createEnvelope] - call to create envelope failed", e)
    }

    result
  }

  def uploadFile(byteArray: Array[Byte], fileName: String, contentType: String, envelopeId: String, fileId: String)
                (implicit hc: HeaderCarrier): Future[HttpResponse] =  {
    val url: String =
      s"$fileUploadFrontEndUrl/file-upload/upload/envelopes/$envelopeId/files/$fileId"

    val multipartFormData: Source[MultipartFormData.Part[Source[ByteString, NotUsed]], NotUsed] =
      Source(FilePart("attachment", fileName, Some(contentType), Source(ByteString(byteArray) :: Nil)) :: DataPart("", "") :: Nil)

    val result: Future[HttpResponse] =
      wsClient.url(url)
        .withHttpHeaders(hc.copy(otherHeaders = Seq("CSRF-token" -> "nocheck")).headers(HeaderNames.explicitlyIncludedHeaders): _*)
        .post(multipartFormData).flatMap { response =>

        response.status match {
          case OK =>
            connectionLogger.info(formatMessage(ld = hc, method = "POST", uri = url, startAge = hc.age, message = "ok"))
            Future.successful(HttpResponse(response.status, ""))
          case _ =>
            connectionLogger.info(formatMessage(ld = hc, method = "POST", uri = url, startAge = hc.age, message = s"${response.status}"))
            Future.failed(new RuntimeException(s"failed with status [${response.status}]"))
        }
      }

    result.failed.foreach {
      case e =>
        connectionLogger.error(formatMessage(ld = hc, method = "POST", uri = url, startAge = hc.age, message = s"${e.getMessage}"), e)
    }
    result
  }

  def closeEnvelope(envId: String)(implicit hc: HeaderCarrier): Future[String] = {

    val url = s"$fileUploadUrl/file-routing/requests"
    val result: Future[String] =
      httpClient
        .post(url"$url")
        .withBody(Json.toJson(routingRequest(envId)))
        .execute[HttpResponse]
        .flatMap { response =>
          response.status match {
            case CREATED =>
              envelopeId(response).map(Future.successful).getOrElse {
                Future.failed(new RuntimeException("No routing id returned"))
              }
            case BAD_REQUEST =>
              if (response.body.contains("Routing request already received for envelope")) {
                connectionLogger.warn(s"[FileUploadConnector][closeEnvelope] Routing request already received for envelope")
                Future.successful("Already Closed")
              } else {
                Future.failed(new RuntimeException("failed with status 400 bad request"))
              }
            case _ =>
              Future.failed(new RuntimeException(s"failed to close envelope with status [${response.status}]"))
          }
    }

    result.failed.foreach {
      case e =>
        connectionLogger.error("[FileUploadConnector][closeEnvelope] call to close envelope failed", e)
    }

    result
  }

  def parseEnvelope(body: String): Future[Envelope] = {
    val envelope: JsResult[Envelope] = Json.parse(body).validate[Envelope]
    envelope match {
      case s: JsSuccess[Envelope] => Future.successful(s.get)
      case _ => Future.failed(new RuntimeException("Failed to parse envelope"))
    }
  }

  def retry(envelopeId: String, cur: Int, attempt: Int, factor: Float = 2f)(implicit hc: HeaderCarrier): Future[Envelope] = {
    attempt match {
      case attempt: Int if attempt < maxAttemptNumber =>
        val nextTry: Int = Math.ceil(cur * factor).toInt
        val nextAttempt = attempt + 1

        after(nextTry.milliseconds, as.scheduler, ec, new Callable[Future[Int]]{
          override def call(): Future[Int] = Future.successful(1) }).flatMap { _ =>
          envelopeSummary(envelopeId, nextTry, nextAttempt)(hc)
        }
      case _ =>
        Future.failed(new RuntimeException(s"[FileUploadConnector][retry] envelope[$envelopeId] summary failed at attempt: $attempt"))
    }
  }

  def envelopeSummary(envelopeId: String, nextTry: Int = firstRetryMilliseconds, attempt: Int = 1)
                     (implicit hc: HeaderCarrier): Future[Envelope] = {
    val url = s"$fileUploadUrl/file-upload/envelopes/$envelopeId"
    httpClient
      .get(url"$url")
      .execute[HttpResponse]
      .flatMap {
        response =>
          response.status match {
            case OK =>
              parseEnvelope(response.body)
            case NOT_FOUND =>
              retry(envelopeId, nextTry, attempt)
            case _ =>
              Future.failed(new RuntimeException(s"[FileUploadConnector][envelopeSummary]Failed with status [${response.status}]"))
          }
    }
  }

  def envelopeId(response: HttpResponse): Option[String] = {
    response.header("Location").map(
      path =>
        path.split("/").reverse.head
    )
  }

  def formatMessage(ld: LoggingDetails, method: String, uri: String, startAge: Long, message: String): String = {
    val requestId    = ld.requestId.getOrElse("")
    val requestChain = ld.requestChain
    val durationNs   = ld.age - startAge
    s"$requestId:$method:$startAge:${formatNs(startAge)}:$durationNs:${formatNs(durationNs)}:${requestChain.value}:$uri:$message"
  }
}
