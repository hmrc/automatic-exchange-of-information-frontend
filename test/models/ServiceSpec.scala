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

package models

import base.SpecBase
import org.scalatest.EitherValues
import play.api.mvc.PathBindable

class ServiceSpec extends SpecBase with EitherValues {

  "Service" - {
    Seq(MDR, DAC6) foreach {
      service =>
        s"must bind from url for service $service" in {
          val pathBindable = implicitly[PathBindable[Service]]

          val bind: Either[String, Service] = pathBindable.bind("service", service.toString)
          bind.value mustBe service
        }

        s"unbind to path value $service" in {
          val pathBindable = implicitly[PathBindable[Service]]

          val bindValue = pathBindable.unbind("service", service)
          bindValue mustBe service.toString
        }
    }

    "must fail to bind for invalid service" in {
      val pathBindable = implicitly[PathBindable[Service]]

      val bind: Either[String, Service] = pathBindable.bind("service", "randomName")
      bind.left.value mustBe "Unknown service"
    }
  }
}
