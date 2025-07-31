require 'json'
$is_cold = true

def hello(event:, context:)
  start = Process.clock_gettime(Process::CLOCK_MONOTONIC)
  body = {
    message: "HELLO WORLD",
    runtime: "ruby3.2",
    coldStart: $is_cold
  }
  $is_cold = false
  body[:handlerDurationMs] = ((Process.clock_gettime(Process::CLOCK_MONOTONIC) - start) * 1000).round

  {
    statusCode: 200,
    headers: { "Content-Type" => "application/json" },
    body: JSON.generate(body)
  }
end
