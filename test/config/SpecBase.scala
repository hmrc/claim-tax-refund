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

package config

import akka.actor.ActorSystem
import org.scalacheck.Shrink
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.Injector
import uk.gov.hmrc.http.HeaderCarrier

trait SpecBase extends PlaySpec
  with GuiceOneAppPerSuite
  with ScalaFutures
  with MockitoSugar
  with ScalaCheckPropertyChecks {

  implicit def dontShrink[A]: Shrink[A] = Shrink.shrinkAny

  def injector: Injector = app.injector

  def appConfig: MicroserviceAppConfig = injector.instanceOf[MicroserviceAppConfig]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  implicit val as: ActorSystem = ActorSystem()

}