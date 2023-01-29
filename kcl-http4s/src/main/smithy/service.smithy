namespace kinesis4cats.kcl.http4s.generated

use alloy#simpleRestJson

@simpleRestJson
service KCLService {
  version: "1.0.0"
  errors: [ServiceNotReadyError]
  operations: [Initialized, Healthcheck]
}

@error("server")
@httpError(503)
structure ServiceNotReadyError {
  message: String
}

@readonly
@http(method: "GET", uri: "/initialized", code: 200)
operation Initialized {
  output: Response
}

@readonly
@http(method: "GET", uri: "/healthcheck", code: 200)
operation Healthcheck {
  output: Response
}

structure Response {
  @required
  message: String
}
