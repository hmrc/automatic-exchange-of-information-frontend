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

import play.api.mvc.PathBindable

sealed trait Service

case object MDR extends WithName("mdr") with Service
case object DAC6 extends WithName("dac6") with Service
case object CBC extends WithName("cbc") with Service

object Service {

  implicit def pathBindable: PathBindable[Service] = new PathBindable[Service] {

    override def bind(key: String, value: String): Either[String, Service] =
      implicitly[PathBindable[String]].bind(key, value) match {
        case Right(MDR.toString)  => Right(MDR)
        case Right(DAC6.toString) => Right(DAC6)
        case Right(CBC.toString)  => Right(CBC)
        case _                    => Left("Unknown service")
      }

    override def unbind(key: String, value: Service): String =
      implicitly[PathBindable[String]].unbind(key, value.toString)
  }

}
