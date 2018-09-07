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

import akka.actor.ActorSystem
import connectors.FileUploadConnector
import org.mockito.Mockito.reset
import org.scalacheck.Shrink
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.prop.PropertyChecks
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.inject.Injector
import uk.gov.hmrc.http.HeaderCarrier

trait SpecBase extends PlaySpec
  with OneAppPerSuite
  with ScalaFutures
  with MockitoSugar
  with PropertyChecks
  with BeforeAndAfterEach {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  def injector: Injector = app.injector

  def appConfig : MicroserviceAppConfig = injector.instanceOf[MicroserviceAppConfig]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val as: ActorSystem = ActorSystem()

  val mockFileUploadConnector = mock[FileUploadConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFileUploadConnector)
  }

  override def afterEach() = {
    reset(mockFileUploadConnector)
    super.afterEach()
  }

}