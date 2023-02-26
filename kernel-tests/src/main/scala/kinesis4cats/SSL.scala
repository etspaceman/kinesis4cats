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

package kinesis4cats

import java.security.cert.X509Certificate

import javax.net.ssl._

object SSL {
  lazy val context = {
    val ctx = SSLContext.getInstance("TLS")
    val tm = new X509ExtendedTrustManager {
      def checkClientTrusted(x: Array[X509Certificate], y: String): Unit = {}
      def checkServerTrusted(x: Array[X509Certificate], y: String): Unit = {}
      def getAcceptedIssuers(): Array[X509Certificate] = Array()

      override def checkClientTrusted(
          chain: Array[X509Certificate],
          authType: String,
          socket: java.net.Socket
      ): Unit = {}

      override def checkServerTrusted(
          chain: Array[X509Certificate],
          authType: String,
          socket: java.net.Socket
      ): Unit = {}

      override def checkClientTrusted(
          chain: Array[X509Certificate],
          authType: String,
          engine: SSLEngine
      ): Unit = {}

      override def checkServerTrusted(
          chain: Array[X509Certificate],
          authType: String,
          engine: SSLEngine
      ): Unit = {}
    }
    ctx.init(null, Array(tm), null)
    ctx
  }
}
