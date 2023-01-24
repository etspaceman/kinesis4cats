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

package kinesis4cats.localstack.aws.v1

import com.amazonaws.auth.{AWSCredentials, AWSCredentialsProvider}

/** [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/AWSCredentialsProvider.html AWSCredentialsProvider]]
  * implementation for mock purposes.
  *
  * @param creds
  *   [[kinesis4cats.localstack.aws.v1.AwsCreds AwsCreds]] in use
  */
final case class AwsCredsProvider(creds: AwsCreds)
    extends AWSCredentialsProvider {
  override def getCredentials(): AWSCredentials = creds
  override def refresh(): Unit = ()
}

/** [[https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/auth/AWSCredentials.html AWSCredentials]]
  * implementation for mock purposes.
  *
  * @param accessKey
  * @param secretKey
  */
final case class AwsCreds(accessKey: String, secretKey: String)
    extends AWSCredentials {
  override def getAWSAccessKeyId(): String = accessKey

  override def getAWSSecretKey(): String = secretKey

}

object AwsCreds {
  val mockAccessKey = "mock-access-key"
  val mockSecretKey = "mock-secret-key"
  val LocalCreds: AwsCreds =
    AwsCreds(mockAccessKey, mockSecretKey)
  val LocalCredsProvider = AwsCredsProvider(LocalCreds)
}
