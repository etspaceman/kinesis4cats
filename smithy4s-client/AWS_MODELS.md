# Vendored AWS Smithy models

`src/main/smithy/kinesis-2013-12-02.json` is the official AWS Kinesis Smithy
model (JSON AST, Smithy 2.0), vendored verbatim from the AWS API models repository:

- Repo: https://github.com/aws/api-models-aws
- Path: `models/kinesis/service/2013-12-02/kinesis-2013-12-02.json`
- Pinned commit: `44e5150bfa1d9029f2562bbd88dcb65c9df51f4f` (2026-02-03)

AWS publishes these models as raw files in git, not as Maven artifacts, so the
file is checked in directly and fed to `smithy4s` via `smithy4sInputDirs`. The
non-core trait definitions the model references are supplied at parse time by
dependencies scoped to the `Smithy4s` config in `build.sbt`:

- `aws.*` (api/auth/protocols) → `smithy-aws-traits`
- `smithy.rules` → `smithy-rules-engine`
- `smithy.waiters` → `smithy-waiters`
- `smithy.test` → `smithy-smoke-test-traits`
- `aws.test` (smoke-test `vendorParamsShape`) → `smithy-aws-smoke-test-model`

Code is generated only for the namespaces allowlisted in `metadata.smithy`, so
these trait libraries are parse-only and produce no generated sources.

To update, replace the file with a newer revision from the repo above and bump
the pinned commit recorded here. Do not hand-edit the model — project-specific
adjustments live in the `KinesisSpecTransformer` (in `smithy4s-client-transformers`).
