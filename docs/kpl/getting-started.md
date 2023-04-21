# Getting Started

This module intends to be an enriched wrapper for the [KPL](https://docs.aws.amazon.com/streams/latest/dev/developing-producers-with-kpl.html), offered by AWS.

## Installation

```scala
libraryDependencies += "io.github.etspaceman" %% "kinesis4cats-kpl" % "@VERSION@"
```

## Usage

```scala mdoc:compile-only
import java.nio.ByteBuffer

import cats.effect._
import com.amazonaws.services.kinesis.producer._

import kinesis4cats.kpl.KPLProducer

object MyApp extends IOApp {
    override def run(args: List[String]) = {
        KPLProducer.Builder.default[IO].build.use(kpl => 
            for {
                _ <- kpl.put(
                    new UserRecord(
                        "my-stream", 
                        "my-partition-key", 
                        ByteBuffer.wrap("some-data".getBytes())
                    )
                )
                _ <- kpl.put(
                    new UserRecord(
                        "my-stream", 
                        "my-partition-key2", 
                        ByteBuffer.wrap("some-data2".getBytes())
                    )
                )
            } yield ExitCode.Success
        )
    }
}
```
