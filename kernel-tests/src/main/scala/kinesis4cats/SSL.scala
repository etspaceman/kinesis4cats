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
