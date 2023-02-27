/*
 * Copyright 2023-2023 etspaceman
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

package kinesis4cats.kcl.fs2.ciris

import scala.concurrent.duration._

import cats.effect.IO
import cats.syntax.all._

import kinesis4cats.kcl.fs2.KCLConsumerFS2
import kinesis4cats.kcl.fs2.instances.eq._
import kinesis4cats.kcl.fs2.instances.show._

class KCLCirisFS2Spec extends munit.CatsEffectSuite {
  test(
    "It should load the environment variables the same as system properties for CoordinatorConfig"
  ) {
    for {
      configEnv <- KCLCirisFS2.readFS2Config(Some("env")).load[IO]
      configProp <- KCLCirisFS2.readFS2Config(Some("prop")).load[IO]
      expected = KCLConsumerFS2.FS2Config(
        200,
        500,
        5.seconds,
        3,
        1.second
      )
    } yield {
      assert(
        configEnv === configProp,
        s"envi: ${configEnv.show}\nprop: ${configProp.show}"
      )
      assert(
        configEnv === expected,
        s"envi: ${configEnv.show}\nexpe: ${expected.show}"
      )
    }
  }
}
