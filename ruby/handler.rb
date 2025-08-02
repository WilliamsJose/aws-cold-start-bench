require 'json'
require 'etc'

# Module initialization timestamp
MODULE_INIT_TIME = Process.clock_gettime(Process::CLOCK_MONOTONIC)
MODULE_INIT_DATE = Time.now.utc.iso8601

# Cold start detection with more robust logic
$container_start_time = MODULE_INIT_TIME
$invocation_count = 0
$last_invocation_time = 0

# Cold start threshold: 5 minutes of inactivity
COLD_START_THRESHOLD_MS = 5 * 60 * 1000

def detect_cold_start
  now = Process.clock_gettime(Process::CLOCK_MONOTONIC)
  time_since_last_invocation = (now - $last_invocation_time) * 1000 # Convert to ms
  
  # First invocation or long period of inactivity
  is_cold = $invocation_count == 0 || time_since_last_invocation > COLD_START_THRESHOLD_MS
  
  if is_cold && $invocation_count > 0
    # Container was reused after inactivity - reset metrics
    $container_start_time = now
  end
  
  is_cold
end

def get_memory_usage
  # Get memory usage from /proc/self/status if available (Linux)
  if File.exist?('/proc/self/status')
    status = File.read('/proc/self/status')
    vm_rss = status[/VmRSS:\s+(\d+)\s+kB/, 1]
    vm_size = status[/VmSize:\s+(\d+)\s+kB/, 1]
    
    {
      rss: vm_rss ? (vm_rss.to_f / 1024).round(2) : 0, # MB
      vms: vm_size ? (vm_size.to_f / 1024).round(2) : 0 # MB
    }
  else
    # Fallback for non-Linux systems
    {
      rss: 0,
      vms: 0
    }
  end
end

def get_system_info
  {
    cpuCount: Etc.nprocessors,
    platform: RbConfig::CONFIG['host_os'],
    arch: RbConfig::CONFIG['host_cpu'],
    rubyVersion: RUBY_VERSION
  }
end

def hello(event:, context:)
  handler_start_time = Process.clock_gettime(Process::CLOCK_MONOTONIC)
  request_id = context&.aws_request_id || 'local-test'
  is_cold = detect_cold_start
  
  $invocation_count += 1
  $last_invocation_time = handler_start_time
  
  # Simulate some work to measure handler execution time
  work_start_time = Process.clock_gettime(Process::CLOCK_MONOTONIC)
  
  # Get system information
  memory_usage = get_memory_usage
  system_info = get_system_info
  
  work_end_time = Process.clock_gettime(Process::CLOCK_MONOTONIC)
  handler_end_time = Process.clock_gettime(Process::CLOCK_MONOTONIC)
  
  # Calculate precise timings (all in milliseconds with 2 decimal precision)
  timings = {
    moduleInitDurationMs: ((handler_start_time - MODULE_INIT_TIME) * 1000).round(2),
    handlerDurationMs: ((handler_end_time - handler_start_time) * 1000).round(2),
    workDurationMs: ((work_end_time - work_start_time) * 1000).round(2),
    totalContainerAgeMs: ((handler_start_time - $container_start_time) * 1000).round(2)
  }
  
  # Standardized response format
  response = {
    # Basic info
    message: "HELLO WORLD",
    runtime: "ruby3.2",
    timestamp: Time.now.utc.iso8601,
    requestId: request_id,
    
    # Cold start detection
    coldStart: is_cold,
    invocationCount: $invocation_count,
    
    # Timing metrics (all in milliseconds with 2 decimal precision)
    timings: timings,
    
    # System metrics
    system: {
      memoryUsage: memory_usage,
      **system_info
    },
    
    # Container lifecycle
    container: {
      initTimestamp: MODULE_INIT_DATE,
      ageMs: timings[:totalContainerAgeMs]
    }
  }
  
  # Structured logging
  log_entry = {
    level: "INFO",
    timestamp: response[:timestamp],
    requestId: request_id,
    coldStart: is_cold,
    invocationCount: $invocation_count,
    timings: timings,
    memoryUsage: memory_usage[:rss],
    message: "Lambda invocation completed"
  }
  puts JSON.generate(log_entry)
  
  {
    statusCode: 200,
    headers: {
      "Content-Type" => "application/json",
      "X-Cold-Start" => is_cold.to_s,
      "X-Invocation-Count" => $invocation_count.to_s,
      "X-Handler-Duration-Ms" => timings[:handlerDurationMs].to_s,
      "X-Runtime" => "ruby3.2"
    },
    body: JSON.pretty_generate(response)
  }
end
