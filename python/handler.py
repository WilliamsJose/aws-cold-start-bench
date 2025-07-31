import time, json
is_cold = True

def hello(event, context):
    global is_cold
    start = time.perf_counter()
    body = {
        "message": "HELLO WORLD",
        "runtime": "python3.11",
        "coldStart": is_cold
    }
    is_cold = False
    body["handlerDurationMs"] = round((time.perf_counter() - start) * 1000)
    return {
        "statusCode": 200,
        "headers": {"Content-Type": "application/json"},
        "body": json.dumps(body)
    }
