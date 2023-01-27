# kinesis4cats

[![Build](https://github.com/etspaceman/kinesis4cats/actions/workflows/ci.yml/badge.svg)](https://github.com/etspaceman/kinesis4cats/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/etspaceman/kinesis4cats/branch/main/graph/badge.svg?token=HSJWGQ8M2W)](https://codecov.io/gh/etspaceman/kinesis4cats)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

## Project Goals

kinesis4cats is intended to give Scala developers a rich ecosystem of tooling when developing [Kinesis](https://aws.amazon.com/kinesis/) applications, leveraging the [typelevel](https://typelevel.org/) ecosystem. 

## Logging

Logs are an important part of maintaining business applications. To accomplish this, most developers leverage some means of structured logging, which effectively boils down to having a context of keys and values that are parseable by a log ingestor (e.g. datadog, splunk, etc.).

This library takes structured logging seriously, and offers it as a first class citizen. This library adheres to the following log-level definitions:

- ERROR: Unhandled errors / outcomes
- WARN: Handled errors / outcomes
- INFO: Informational, low-frequency logs (e.g. startup/shutdown logs)
- DEBUG: Informational, high-frequency logs (e.g. request logging)
- TRACE: Data / class bodies

Each offering leverages [LogEncoder](https://github.com/etspaceman/kinesis4cats/shared/src/main/scala/kinesis4cats/logging/LogEncoder.scala) instances to structure the logs that you see. There are [Show](https://typelevel.org/cats/typeclasses/show.html) instances available to import out-of-the-box, which attempts to create a toString-like representation of the structures. That can be useful for most people, but if you are looking to introduce more searchability, using a JSON offering can be very beneficial. There are [Circe](https://circe.github.io/circe/) modules for these instances that users can leverage for this purpose.

## Offerings

- [Kinesis Client Library (KCL)](kcl/index.md)
- [Kinesis Producer Library (KPL)](kpl/index.md)
- [Kinesis Client Wrapper](client/index.md)
