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

package config

import com.google.inject.{Inject, Singleton}
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig


@Singleton
class MicroserviceAppConfig @Inject()(override val runModeConfiguration: Configuration, environment: Environment) extends ServicesConfig {
  override protected def mode: Mode = environment.mode

  private def loadConfig(key: String): String = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  private def loadBoolean(key: String): Boolean = runModeConfiguration.getBoolean(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  val appName: String = runModeConfiguration.underlying.getString("appName")
  val fileUploadUrl: String = baseUrl("file-upload")
  val fileUploadFrontendUrl: String = baseUrl("file-upload-frontend")
  val fileUploadCallbackUrl: String = loadConfig("microservice.services.file-upload.callbackUrl")
  val pdfGeneratorUrl =  baseUrl("pdf-generator-service")

  val maxAttemptNumber: Int = 5
  val firstRetryMilliseconds: Int = 20

  object CTUTR {
    val businessArea : String = loadConfig(s"pdf.ctr.metadata.businessArea")
    val queue : String = loadConfig(s"pdf.ctr.metadata.queue")
    val formId : String = loadConfig(s"pdf.ctr.metadata.formId")
    val source : String = loadConfig(s"pdf.ctr.metadata.source")
    val target : String = loadConfig(s"pdf.ctr.metadata.target")
    val save : Boolean = loadBoolean(s"pdf.ctr.save")
  }

}
