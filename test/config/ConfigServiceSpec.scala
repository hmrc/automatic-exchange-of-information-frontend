/*
 * Copyright 2026 HM Revenue & Customs
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

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.Configuration

class ConfigServiceSpec extends AnyWordSpec with Matchers {

  "Service" should {

    "build the correct baseUrl from host, port and protocol" in {
      val service = Service("localhost", "8080", "http")
      service.baseUrl shouldBe "http://localhost:8080"
    }

    "use baseUrl as its toString representation" in {
      val service = Service("example.com", "443", "https")
      service.toString shouldBe "https://example.com:443"
    }

    "be implicitly convertible to a String" in {
      val asString: String = Service("host", "80", "http")
      asString shouldBe "http://host:80"
    }
  }

  "Service.configLoader" should {

    "load a Service from a config block with the given prefix" in {
      val config = ConfigFactory.parseString(
        """
          |my-service {
          |  host     = "localhost"
          |  port     = "9000"
          |  protocol = "http"
          |}
          |""".stripMargin
      )

      val service = Service.configLoader.load(config, "my-service")

      service shouldBe Service("localhost", "9000", "http")
    }

    "be picked up automatically by Configuration.get[Service]" in {
      val config = ConfigFactory.parseString(
        """
          |another-service {
          |  host     = "api.example.com"
          |  port     = "443"
          |  protocol = "https"
          |}
          |""".stripMargin
      )

      val configuration = Configuration(config)
      val service       = configuration.get[Service]("another-service")

      service shouldBe Service("api.example.com", "443", "https")
    }

    "throw a config exception if a required key is missing" in {
      val config = ConfigFactory.parseString(
        """
          |broken-service {
          |  host = "localhost"
          |  port = "8080"
          |  // protocol is missing on purpose
          |}
          |""".stripMargin
      )

      an[com.typesafe.config.ConfigException] should be thrownBy {
        Service.configLoader.load(config, "broken-service")
      }
    }
  }
}
