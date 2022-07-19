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

package config

import com.google.inject.{Inject, Singleton}
import javax.inject.Named
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.util.Try

@Singleton
class MicroserviceAppConfig @Inject()(val servicesConfig: ServicesConfig,
                                      @Named("appName") val appName: String) {

  private def loadConfig(key: String): String = Try(servicesConfig.getString(key))
    .getOrElse(throw new Exception(s"Missing configuration key: $key"))

  val fileUploadUrl: String = servicesConfig.baseUrl("file-upload")
  lazy val dmsApiUrl: String = servicesConfig.baseUrl("dmsapi")
  val fileUploadFrontendUrl: String = servicesConfig.baseUrl("file-upload-frontend")
  val fileUploadCallbackUrl: String = loadConfig("microservice.services.file-upload.callbackUrl")
  val pdfGeneratorUrl: String = servicesConfig.baseUrl("pdf-generator-service")

  val maxAttemptNumber: Int = 5
  val firstRetryMilliseconds: Int = 20
}
