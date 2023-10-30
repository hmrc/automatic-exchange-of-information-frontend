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

package controllers.actions

import base.SpecBase
import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.{CBC, MDR, Service}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.mockito.MockitoSugar.mock
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec extends SpecBase with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    super.beforeEach()
  }

  class Harness(authAction: IdentifyAndRedirectAction, service: Service) {

    def onPageLoad(): Action[AnyContent] = authAction(service) {
      _ => Results.Ok
    }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  val mdrEnrolments: Enrolments = Enrolments(
    Set(
      Enrolment(
        key = "HMRC-MDR-ORG",
        identifiers = Seq(
          EnrolmentIdentifier(
            "MDRID",
            "123"
          )
        ),
        state = "Activated"
      )
    )
  )

  val mdrNotActiveEnrolments: Enrolments = Enrolments(
    Set(
      Enrolment(
        key = "HMRC-MDR-ORG",
        identifiers = Seq(
          EnrolmentIdentifier(
            "MDRID",
            "123"
          )
        ),
        state = "NotYetActivated"
      )
    )
  )

  val mdrNoSubscriptionIDEnrolments: Enrolments = Enrolments(
    Set(
      Enrolment(
        key = "HMRC-MDR-ORG",
        identifiers = Seq.empty,
        state = "Activated"
      )
    )
  )

  val cbcEnrolments: Enrolments = Enrolments(
    Set(
      Enrolment(
        key = "HMRC-CBC-ORG",
        identifiers = Seq(
          EnrolmentIdentifier(
            "cbcId",
            "123"
          )
        ),
        state = "Activated"
      )
    )
  )

  val cbcNotActiveEnrolments: Enrolments = Enrolments(
    Set(
      Enrolment(
        key = "HMRC-CBC-ORG",
        identifiers = Seq(
          EnrolmentIdentifier(
            "cbcId",
            "123"
          )
        ),
        state = "NotYetActivated"
      )
    )
  )

  val cbcNoSubscriptionIDEnrolments: Enrolments = Enrolments(
    Set(
      Enrolment(
        key = "HMRC-CBC-ORG",
        identifiers = Seq.empty,
        state = "Activated"
      )
    )
  )

  "Auth Action" - {

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifyAndRedirectAction(new FakeFailingAuthConnector(new MissingBearerToken), appConfig, bodyParsers)
          val controller = new Harness(authAction, serviceMDR)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifyAndRedirectAction(new FakeFailingAuthConnector(new BearerTokenExpired), appConfig, bodyParsers)
          val controller = new Harness(authAction, serviceMDR)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user doesn't have sufficient enrolments" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifyAndRedirectAction(new FakeFailingAuthConnector(new InsufficientEnrolments), appConfig, bodyParsers)
          val controller = new Harness(authAction, serviceMDR)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user doesn't have sufficient confidence level" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifyAndRedirectAction(new FakeFailingAuthConnector(new InsufficientConfidenceLevel), appConfig, bodyParsers)
          val controller = new Harness(authAction, serviceMDR)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifyAndRedirectAction(new FakeFailingAuthConnector(new UnsupportedAuthProvider), appConfig, bodyParsers)
          val controller = new Harness(authAction, serviceMDR)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad.url
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifyAndRedirectAction(new FakeFailingAuthConnector(new UnsupportedAffinityGroup), appConfig, bodyParsers)
          val controller = new Harness(authAction, serviceMDR)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig   = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new AuthenticatedIdentifyAndRedirectAction(new FakeFailingAuthConnector(new UnsupportedCredentialRole), appConfig, bodyParsers)
          val controller = new Harness(authAction, serviceMDR)
          val result     = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad.url)
        }
      }
    }

    "must redirect to fileUpload frontend when user has MDR enrolments" in {

      val application = applicationBuilder.build()

      running(application) {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(mdrEnrolments))

        val bodyParsers       = application.injector.instanceOf[BodyParsers.Default]
        val frontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

        val authAction = new AuthenticatedIdentifyAndRedirectAction(mockAuthConnector, frontendAppConfig, bodyParsers)

        val controller = new Harness(authAction, serviceMDR)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(frontendAppConfig.fileUploadUrl(MDR.toString))
      }
    }

    "must redirect to registration frontend when user has no MDR subscriptionId" in {

      val application = applicationBuilder.build()

      running(application) {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(mdrNoSubscriptionIDEnrolments))

        val bodyParsers       = application.injector.instanceOf[BodyParsers.Default]
        val frontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

        val authAction = new AuthenticatedIdentifyAndRedirectAction(mockAuthConnector, frontendAppConfig, bodyParsers)

        val controller = new Harness(authAction, serviceMDR)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(frontendAppConfig.registrationUrl(MDR.toString))
      }
    }

    "must redirect to registration frontend when user has no active MDR enrolment" in {

      val application = applicationBuilder.build()

      running(application) {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(mdrNotActiveEnrolments))

        val bodyParsers       = application.injector.instanceOf[BodyParsers.Default]
        val frontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

        val authAction = new AuthenticatedIdentifyAndRedirectAction(mockAuthConnector, frontendAppConfig, bodyParsers)

        val controller = new Harness(authAction, serviceMDR)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(frontendAppConfig.registrationUrl(MDR.toString))
      }
    }

    "must redirect to registration frontend when user is not enrolled to MDR" in {

      val application = applicationBuilder.build()

      running(application) {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(Enrolments(Set.empty)))

        val bodyParsers       = application.injector.instanceOf[BodyParsers.Default]
        val frontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

        val authAction = new AuthenticatedIdentifyAndRedirectAction(mockAuthConnector, frontendAppConfig, bodyParsers)

        val controller = new Harness(authAction, serviceMDR)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(frontendAppConfig.registrationUrl(MDR.toString))
      }
    }

    "must redirect to fileUpload frontend when user has CBC enrolments" in {

      val application = applicationBuilder.build()

      running(application) {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(cbcEnrolments))

        val bodyParsers       = application.injector.instanceOf[BodyParsers.Default]
        val frontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

        val authAction = new AuthenticatedIdentifyAndRedirectAction(mockAuthConnector, frontendAppConfig, bodyParsers)

        val controller = new Harness(authAction, serviceCBC)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(frontendAppConfig.fileUploadUrl(CBC.toString))
      }
    }

    "must redirect to registration frontend when user has no CBC subscriptionId" in {

      val application = applicationBuilder.build()

      running(application) {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(mdrNoSubscriptionIDEnrolments))

        val bodyParsers       = application.injector.instanceOf[BodyParsers.Default]
        val frontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

        val authAction = new AuthenticatedIdentifyAndRedirectAction(mockAuthConnector, frontendAppConfig, bodyParsers)

        val controller = new Harness(authAction, serviceCBC)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(frontendAppConfig.registrationUrl(CBC.toString))
      }
    }

    "must redirect to registration frontend when user has no active CBC enrolment" in {

      val application = applicationBuilder.build()

      running(application) {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(cbcNotActiveEnrolments))

        val bodyParsers       = application.injector.instanceOf[BodyParsers.Default]
        val frontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

        val authAction = new AuthenticatedIdentifyAndRedirectAction(mockAuthConnector, frontendAppConfig, bodyParsers)

        val controller = new Harness(authAction, serviceCBC)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(frontendAppConfig.registrationUrl(CBC.toString))
      }
    }

    "must redirect to registration frontend when user is not enrolled to CBC" in {

      val application = applicationBuilder.build()

      running(application) {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(Enrolments(Set.empty)))

        val bodyParsers       = application.injector.instanceOf[BodyParsers.Default]
        val frontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

        val authAction = new AuthenticatedIdentifyAndRedirectAction(mockAuthConnector, frontendAppConfig, bodyParsers)

        val controller = new Harness(authAction, serviceCBC)
        val result     = controller.onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(frontendAppConfig.registrationUrl(CBC.toString))
      }
    }
  }
}

class FakeFailingAuthConnector @Inject() (exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}
