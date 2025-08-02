import time
import json
import os
import platform
import psutil
import sys
from datetime import datetime, timezone

# Module initialization timestamp
MODULE_INIT_TIME = time.perf_counter()
MODULE_INIT_DATE = datetime.now(timezone.utc).isoformat()

# Cold start detection with more robust logic
container_start_time = MODULE_INIT_TIME
invocation_count = 0
last_invocation_time = 0

# Cold start threshold: 5 minutes of inactivity
COLD_START_THRESHOLD_MS = 5 * 60 * 1000

def detect_cold_start():
    global container_start_time, invocation_count, last_invocation_time
    
    now = time.perf_counter()
    time_since_last_invocation = (now - last_invocation_time) * 1000  # Convert to ms
    
    # First invocation or long period of inactivity
    is_cold = invocation_count == 0 or time_since_last_invocation > COLD_START_THRESHOLD_MS
    
    if is_cold and invocation_count > 0:
        # Container was reused after inactivity - reset metrics
        container_start_time = now
    
    return is_cold

def get_memory_usage():
    """Get memory usage in MB with 2 decimal precision"""
    process = psutil.Process()
    memory_info = process.memory_info()
    
    return {
        "rss": round(memory_info.rss / 1024 / 1024, 2),  # MB
        "vms": round(memory_info.vms / 1024 / 1024, 2),  # MB
        "percent": round(process.memory_percent(), 2)
    }

def get_system_info():
    """Get system information"""
    return {
        "cpuCount": os.cpu_count(),
        "platform": platform.system(),
        "arch": platform.machine(),
        "pythonVersion": sys.version.split()[0]
    }

def hello(event, context):
    global invocation_count, last_invocation_time
    
    handler_start_time = time.perf_counter()
    request_id = getattr(context, 'aws_request_id', 'local-test')
    is_cold = detect_cold_start()
    
    invocation_count += 1
    last_invocation_time = handler_start_time
    
    # Simulate some work to measure handler execution time
    work_start_time = time.perf_counter()
    
    # Get system information
    memory_usage = get_memory_usage()
    system_info = get_system_info()
    
    work_end_time = time.perf_counter()
    handler_end_time = time.perf_counter()
    
    # Calculate precise timings (all in milliseconds with 2 decimal precision)
    timings = {
        "moduleInitDurationMs": round((handler_start_time - MODULE_INIT_TIME) * 1000, 2),
        "handlerDurationMs": round((handler_end_time - handler_start_time) * 1000, 2),
        "workDurationMs": round((work_end_time - work_start_time) * 1000, 2),
        "totalContainerAgeMs": round((handler_start_time - container_start_time) * 1000, 2)
    }
    
    # Standardized response format
    response = {
        # Basic info
        "message": "HELLO WORLD",
        "runtime": "python3.11",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "requestId": request_id,
        
        # Cold start detection
        "coldStart": is_cold,
        "invocationCount": invocation_count,
        
        # Timing metrics (all in milliseconds with 2 decimal precision)
        "timings": timings,
        
        # System metrics
        "system": {
            "memoryUsage": memory_usage,
            **system_info
        },
        
        # Container lifecycle
        "container": {
            "initTimestamp": MODULE_INIT_DATE,
            "ageMs": timings["totalContainerAgeMs"]
        }
    }
    
    # Structured logging
    log_entry = {
        "level": "INFO",
        "timestamp": response["timestamp"],
        "requestId": request_id,
        "coldStart": is_cold,
        "invocationCount": invocation_count,
        "timings": timings,
        "memoryUsage": memory_usage["rss"],
        "message": "Lambda invocation completed"
    }
    print(json.dumps(log_entry))
    
    return {
        "statusCode": 200,
        "headers": {
            "Content-Type": "application/json",
            "X-Cold-Start": str(is_cold).lower(),
            "X-Invocation-Count": str(invocation_count),
            "X-Handler-Duration-Ms": str(timings["handlerDurationMs"]),
            "X-Runtime": "python3.11"
        },
        "body": json.dumps(response, indent=2)
    }
