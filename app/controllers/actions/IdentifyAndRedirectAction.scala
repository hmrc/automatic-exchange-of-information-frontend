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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.Service
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifyAndRedirectAction {
  def apply(service: Service): ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]
}

class AuthenticatedIdentifyAndRedirectAction @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifyAndRedirectAction
    with AuthorisedFunctions {

  override def apply(service: Service): ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest] =
    new AuthenticatedIdentifierActionWithService(authConnector, config, parser, service)
}

class AuthenticatedIdentifierActionWithService @Inject() (
  override val authConnector: AuthConnector,
  val config: FrontendAppConfig,
  val parser: BodyParsers.Default,
  val service: Service
)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with AuthorisedFunctions
    with Logging {

  private val enrolmentKey: String     = config.enrolmentKey(service.toString)
  private val identifier: String       = config.identifier(service.toString)
  private val registrationURL: String  = config.registrationUrl(service.toString)
  private val fileUploadURL: String    = config.fileUploadUrl(service.toString)
  private val loginContinueUrl: String = config.loginContinueUrl(service.toString)

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(AuthProviders(GovernmentGateway) and ConfidenceLevel.L50).retrieve(Retrievals.allEnrolments) {
      enrolments =>
        isEnrolledToService(enrolments) match {
          case Some(true) => Future.successful(Redirect(fileUploadURL))
          case _          => Future.successful(Redirect(registrationURL))
        }
    } recover {
      case _: NoActiveSession =>
        Redirect(config.loginUrl, Map("continue" -> Seq(loginContinueUrl)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad)
    }
  }

  private def isEnrolledToService[A](enrolments: Enrolments) =
    for {
      enrolment <- enrolments.enrolments.filter(_.isActivated).find(_.key.equals(enrolmentKey))
      enrolled  <- enrolment.getIdentifier(identifier).map(_.value.nonEmpty)
    } yield enrolled
}
