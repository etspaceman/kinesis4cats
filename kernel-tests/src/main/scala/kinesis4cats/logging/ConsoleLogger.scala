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

package kinesis4cats.logging

import java.time.Instant

import cats.effect.kernel.Async
import cats.effect.std.Console
import cats.syntax.all._
import org.typelevel.log4cats.SelfAwareStructuredLogger

private[kinesis4cats] final class ConsoleLogger[F[_]](
    logLevel: ConsoleLogger.LogLevel = ConsoleLogger.LogLevel.Error
)(implicit
    C: Console[F],
    F: Async[F]
) extends SelfAwareStructuredLogger[F] {

  private def printMessage(
      logLevel: String,
      ctx: Map[String, String] = Map.empty,
      e: Option[Throwable] = None
  )(
      message: => String
  ): F[Unit] = for {
    _ <- C.println(
      s"[$logLevel] ${Instant
          .now()} ${ctx.map { case (k, v) => s"$k=$v" }.mkString(", ")} $message"
    )
    _ <- e.fold(F.unit)(e => C.printStackTrace(e))
  } yield ()

  override def error(message: => String): F[Unit] =
    isErrorEnabled.ifM(printMessage("error")(message), F.unit)

  override def warn(message: => String): F[Unit] =
    isWarnEnabled.ifM(printMessage("warn")(message), F.unit)

  override def info(message: => String): F[Unit] =
    isInfoEnabled.ifM(printMessage("info")(message), F.unit)

  override def debug(message: => String): F[Unit] =
    isDebugEnabled.ifM(printMessage("debug")(message), F.unit)

  override def trace(message: => String): F[Unit] =
    isTraceEnabled.ifM(printMessage("trace")(message), F.unit)

  override def error(t: Throwable)(message: => String): F[Unit] =
    isErrorEnabled.ifM(printMessage("error", e = Some(t))(message), F.unit)

  override def warn(t: Throwable)(message: => String): F[Unit] =
    isWarnEnabled.ifM(printMessage("warn", e = Some(t))(message), F.unit)

  override def info(t: Throwable)(message: => String): F[Unit] =
    isInfoEnabled.ifM(printMessage("info", e = Some(t))(message), F.unit)

  override def debug(t: Throwable)(message: => String): F[Unit] =
    isDebugEnabled.ifM(printMessage("debug", e = Some(t))(message), F.unit)

  override def trace(t: Throwable)(message: => String): F[Unit] =
    isTraceEnabled.ifM(printMessage("trace", e = Some(t))(message), F.unit)

  override def isTraceEnabled: F[Boolean] =
    F.pure(logLevel == ConsoleLogger.LogLevel.Trace)

  override def isDebugEnabled: F[Boolean] = F.pure(
    logLevel == ConsoleLogger.LogLevel.Trace ||
      logLevel == ConsoleLogger.LogLevel.Debug
  )

  override def isInfoEnabled: F[Boolean] = F.pure(
    logLevel == ConsoleLogger.LogLevel.Trace ||
      logLevel == ConsoleLogger.LogLevel.Debug ||
      logLevel == ConsoleLogger.LogLevel.Info
  )

  override def isWarnEnabled: F[Boolean] = F.pure(
    logLevel == ConsoleLogger.LogLevel.Trace ||
      logLevel == ConsoleLogger.LogLevel.Debug ||
      logLevel == ConsoleLogger.LogLevel.Info ||
      logLevel == ConsoleLogger.LogLevel.Warn
  )
  override def isErrorEnabled: F[Boolean] = F.pure(true)

  override def trace(ctx: Map[String, String])(msg: => String): F[Unit] =
    isTraceEnabled.ifM(printMessage("trace", ctx)(msg), F.unit)

  override def trace(ctx: Map[String, String], t: Throwable)(
      msg: => String
  ): F[Unit] =
    isTraceEnabled.ifM(printMessage("trace", ctx, Option(t))(msg), F.unit)

  override def debug(ctx: Map[String, String])(msg: => String): F[Unit] =
    isDebugEnabled.ifM(printMessage("debug", ctx)(msg), F.unit)

  override def debug(ctx: Map[String, String], t: Throwable)(
      msg: => String
  ): F[Unit] =
    isDebugEnabled.ifM(printMessage("debug", ctx, Option(t))(msg), F.unit)

  override def info(ctx: Map[String, String])(msg: => String): F[Unit] =
    isInfoEnabled.ifM(printMessage("info", ctx)(msg), F.unit)

  override def info(ctx: Map[String, String], t: Throwable)(
      msg: => String
  ): F[Unit] =
    isInfoEnabled.ifM(printMessage("info", ctx, Option(t))(msg), F.unit)

  override def warn(ctx: Map[String, String])(msg: => String): F[Unit] =
    isWarnEnabled.ifM(printMessage("warn", ctx)(msg), F.unit)

  override def warn(ctx: Map[String, String], t: Throwable)(
      msg: => String
  ): F[Unit] =
    isWarnEnabled.ifM(printMessage("warn", ctx, Option(t))(msg), F.unit)

  override def error(ctx: Map[String, String])(msg: => String): F[Unit] =
    isErrorEnabled.ifM(printMessage("error", ctx)(msg), F.unit)

  override def error(ctx: Map[String, String], t: Throwable)(
      msg: => String
  ): F[Unit] =
    isErrorEnabled.ifM(printMessage("error", ctx, Option(t))(msg), F.unit)

}

object ConsoleLogger {
  sealed trait LogLevel extends Product with Serializable
  object LogLevel {
    case object Error extends LogLevel
    case object Warn extends LogLevel
    case object Info extends LogLevel
    case object Debug extends LogLevel
    case object Trace extends LogLevel
  }
}
