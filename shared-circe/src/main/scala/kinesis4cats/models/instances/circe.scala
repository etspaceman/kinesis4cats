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

package kinesis4cats.models
package instances

import io.circe.Decoder
import io.circe.Encoder

object circe {
  implicit val encryptionTypeCirceEncoder: Encoder[EncryptionType] =
    Encoder[String].contramap(_.value)

  implicit val encryptionTypeCirceDecoder: Decoder[EncryptionType] =
    Decoder[String].emap {
      case EncryptionType.None.value => Right(EncryptionType.None)
      case EncryptionType.Kms.value  => Right(EncryptionType.Kms)
      case x =>
        Left(
          s"Unexpected Encryption Type value '$x' received. Expected values are [${EncryptionType.None.value}, ${EncryptionType.Kms.value}]"
        )
    }
}
