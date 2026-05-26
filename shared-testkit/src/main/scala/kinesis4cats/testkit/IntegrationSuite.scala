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

package kinesis4cats.testkit

import munit.{CatsEffectSuite, Tag}

/** Base class for integration tests. Subclasses (direct or transitive) are
  * tagged `Tag("integration")` on every `test(...)` call, so `sbt test`
  * excludes them by default and `sbt itTest` (or
  * `ThisBuild/runIntegrationTests := true`) includes them.
  */
trait IntegrationSuite extends CatsEffectSuite {
  override def munitTestTransforms: List[TestTransform] =
    super.munitTestTransforms :+ new TestTransform(
      "integration",
      _.tag(new Tag("integration"))
    )
}
