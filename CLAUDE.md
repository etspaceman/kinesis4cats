# CLAUDE.md — kinesis4cats

A Typelevel/cats-effect toolkit around AWS Kinesis — cats-friendly wrappers for the KCL (consumers), KPL (producers), AWS Java SDK v2 client, and a pure-Scala smithy4s client, plus Circe logging, Ciris config, Feral Lambda interfaces, and localstack test-kits. Cross-built for Scala 2.13 + 3 across JVM / JS / Native. Org `io.github.etspaceman`, base version `0.5`. Built with sbt-typelevel.

## Toolchain

- Java 17 via sdkman (`.sdkmanrc` -> `17.0.19-amzn`; CI uses temurin@17). Run `sdk env` in repo root before building.
- sbt 1.12.11 (`project/build.properties`); Scala 2.13.18 + 3.3.7 (`allScalaVersions` in `project/Kinesis4CatsPlugin.scala`).
- Typelevel ecosystem: cats, cats-effect, fs2, log4cats, munit-cats-effect, Circe, Ciris, http4s, smithy4s, Feral.
- Docker required for integration tests (localstack).

## Commands

Cross-build tasks operate over a root per platform: `rootJVM`, `rootJS`, `rootNative`. Use `++ <version>` to pick Scala. Plain Scala-3-only projects live under `rootJVMPlain3`.

Build / test (mirror CI — replace project/scala as needed):

```
sbt 'project rootJVM' '++ 3' test
```

Format + lint check (exactly what CI runs):

```
sbt 'project rootJVM' '++ 3' headerCheckAll scalafmtCheckAll 'project /' scalafmtSbtCheck && sbt 'project rootJVM' '++ 3' 'scalafixAll --check'
```

Auto-fix everything before committing (header + scalafix + scalafmt + workflow regen):

```
sbt pretty
```

Check-only equivalent (`prettyCheck` = `headerCheckAll;fixCheck;fmtCheck`):

```
sbt prettyCheck
```

Binary compat + docs (CI steps):

```
sbt 'project rootJVM' '++ 3' mimaReportBinaryIssues doc
```

JS link / Native link (CI gates these per-platform):

```
sbt 'project rootJS' '++ 3' Test/scalaJSLinkerResult
```
```
sbt 'project rootNative' '++ 3' Test/nativeLink
```

Generate docs site (mdoc + laika):

```
sbt docs/tlSite
```

Coverage:

```
sbt cov
```

## Integration tests (localstack)

Integration tests are munit-tagged `integration` and excluded by default. The `itTest` alias flips `ThisBuild/runIntegrationTests := true` around `test`. They require the docker-compose stack (localstack + the `kcl-http4s-test-server` image) running first.

Bring stack up (builds the test-server image, then `docker compose up -d` on `docker/docker-compose.yml`):

```
sbt 'project rootJVM' '++ 3' dockerComposeUp
```

Run integration tests, then tear down:

```
sbt 'project rootJVM' '++ 3' itTest && sbt 'project rootJVM' '++ 3' dockerComposeDown
```

Debug a stuck stack: `sbt dockerComposePs dockerComposeLogs`. Compose spins up localstack (kinesis/dynamodb/cloudwatch/sts on :4566), seeds a `test-kcl-service-stream`, and runs the http4s test server on :8080. `LOCALSTACK_AUTH_TOKEN` is optional locally.

## Architecture / module map

All modules are `crossProject` with `CrossType.Pure`. JVM is the primary target; Native pins to Scala 3 only. `moduleName` is prefixed `kinesis4cats-`.

Shared / core:
- `compat` — cross-Scala-version compatibility shims (JVM/JS/Native).
- `shared` — common utilities, log encoders, protobuf record aggregation (scalapb).
- `shared-circe` / `shared-ciris` — Circe and Ciris support for shared.
- `shared-localstack` — common localstack test-kit utilities.
- `shared-testkit` — integration-test scaffolding (NoPublish).
- `aws-v2-localstack` — localstack test-kit via AWS SDK v2 (JVM only).

KCL (consumers, JVM only):
- `kcl` — cats tooling for the Kinesis Client Library.
- `kcl-ciris` — Ciris config loading for KCL (uses sbt-buildinfo in tests).
- `kcl-http4s` — http4s/smithy4s interfaces for the KCL.
- `kcl-http4s-test-server` — Scala-3-only assembly/Docker test server for integration tests (NoPublish).
- `kcl-logging-circe` — JSON structured logging for KCL.
- `kcl-localstack` — KCL localstack test-kit.

KPL (producers, JVM only): `kpl`, `kpl-ciris`, `kpl-logging-circe`, `kpl-localstack` — parallel structure to KCL.

Java Kinesis client (AWS SDK v2, JVM only): `kinesis-client`, `kinesis-client-logging-circe`, `kinesis-client-localstack`.

smithy4s client (pure-Scala client; JVM/JS/Native): `smithy4s-client`, `smithy4s-client-logging-circe`, `smithy4s-client-localstack`, plus `smithy4s-client-transformers` (Scala-3-only build-time model transformer, NoPublish).

Other:
- `feral` — AWS Lambda interfaces via Typelevel Feral (JVM/JS).
- `docs` (in `site/`) + `unidocs` — microsite (laika/tlSite) and unified API docs (Scala 3 only).

## Code style

- Functional, Typelevel-first. scalafmt (`.scalafmt.conf`, dialect `scala213Source3`) and scalafix (`.scalafix.conf`) are CI-enforced.
- scalafix `DisableSyntax` bans: `var`, `null`, `throw`, `return`, while-loops, `asInstanceOf`/`isInstanceOf`, `finalize`, XML, val-patterns, final-val. Don't introduce these.
- Apache-2.0 license headers enforced (`headerCheckAll`); `sbt pretty` adds them.
- `tlJdkRelease := 8` for most modules (smithy4s/http4s-test-server use 11) — keep APIs JDK-8 compatible.

## Gotchas

- Cross-build matrix is large: 2.13+3 x JVM/JS/Native, with worktree dirs like `.jvm-plain-2.13`, `.native-3`, `.native-scala-3`. Native and several plain projects are Scala-3-only. `rootJVMPlain3` is deliberately kept out of the top-level aggregate so `+`-prefixed cross tasks don't drag plain Scala-3 projects under 2.13 — reach it via `rootJVMPlain3/Test/compile`.
- Integration tests need docker + localstack up first (`dockerComposeUp`); they're excluded from plain `test`.
- AWS SDK / KCL / KPL / kinesis-aggregation deps are pinned in `.scala-steward.conf` (`updates.ignore`) — Scala Steward won't bump them; update manually and deliberately.
- `.github/workflows/ci.yml` is generated by sbt-typelevel — never hand-edit. Change build settings and run `sbt githubWorkflowGenerate` (part of `sbt pretty`); CI verifies with `githubWorkflowCheck`.
- scalafmt-reformat commits are listed in `.git-blame-ignore-revs`; configure git blame to use it (`git config blame.ignoreRevsFile .git-blame-ignore-revs`).
- `Test / parallelExecution := false` and several modules `forkTests`; some tests rely on env vars/system props wired via buildinfo.
