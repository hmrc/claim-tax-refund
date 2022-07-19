/*
 * Copyright 2022 HM Revenue & Customs
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

import com.google.inject.{Inject, Singleton}
import config.MicroserviceAppConfig
import play.api.http.Status
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.HttpException

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PDFConnector @Inject()(
                              appConfig: MicroserviceAppConfig,
                              wsClient: WSClient) {

  private val pdfGeneratorUrl = appConfig.pdfGeneratorUrl

  def generatePDF(html: String): Future[Array[Byte]] = {
    val result = wsClient.url(s"$pdfGeneratorUrl/pdf-generator-service/generate").post(Map("html" -> Seq(html)))
    result map {
      response =>
        response.status match {
          case Status.OK => response.bodyAsBytes.toArray
          case _ => throw new HttpException(s"PDF failed with status: ${response.status}", response.status)
        }
    }
  }

}
