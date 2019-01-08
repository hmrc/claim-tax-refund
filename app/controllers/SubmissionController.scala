/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers

import com.google.inject.{Inject, Singleton}
import connectors.CasConnector
import models.{CallbackRequest, Submission, SubmissionArchiveRequest}
import play.api.Logger
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc.{Action, Result}
import services.SubmissionService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


@Singleton
class SubmissionController @Inject()(
                                      submissionService: SubmissionService,
                                      casConnector: CasConnector
                                    ) extends BaseController {

  def submit(): Action[Submission] = Action.async(parse.json[Submission]) {
    implicit request =>
      val result = submissionService.submit(request.body).map {
        response =>
          Logger.info(s"[SubmissionController][submit] processed submission $response")
          Ok(Json.toJson(response))
      }

      result.onFailure {
        case e =>
          Logger.error(s"[SubmissionController][submit][exception returned when processing submission]", e)
      }

      result
  }

  def callback(): Action[CallbackRequest] = Action.async(parse.json[CallbackRequest]) {
    implicit request =>
      Logger.info(s"[SubmissionController][callback] processing callback ${request.body}")
      request.body.status match {
        case "AVAILABLE" =>
          submissionService.fileUploadCallback(request.body.envelopeId).map(_ => Ok)
        case _ =>
          Logger.warn(s"[SubmissionController][fileUploadCallback] callback for ${request.body.fileId} had status: ${request.body.status}")
          Future.successful(Ok)
      }
  }

  def archiveSubmission(): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      val data: JsResult[SubmissionArchiveRequest] = request.body.validate[SubmissionArchiveRequest]

      val response: Future[Result] = data.map {
        submission =>
          casConnector.archiveSubmission(submission.submissionRef, submission).map {
          response =>
            Logger.info(s"[SubmissionController][archiveSubmission] response received: $response")
            Ok(Json.toJson(response))
        }
      }.getOrElse(Future.failed(new RuntimeException))

      response.onFailure {
        case e =>
          Logger.error(s"[SubmissionController][archiveSubmission][exception returned when archiving submission: $data]", e)
      }

      response
  }

}
