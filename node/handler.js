'use strict';
const { performance } = require('node:perf_hooks');
const os = require('os');

// Module initialization timestamp
const MODULE_INIT_TIME = performance.now();
const MODULE_INIT_DATE = new Date().toISOString();

// Cold start detection with more robust logic
let containerStartTime = MODULE_INIT_TIME;
let invocationCount = 0;
let lastInvocationTime = 0;

// Cold start threshold: 5 minutes of inactivity
const COLD_START_THRESHOLD_MS = 5 * 60 * 1000;

function detectColdStart() {
  const now = performance.now();
  const timeSinceLastInvocation = now - lastInvocationTime;
  
  // First invocation or long period of inactivity
  const isCold = invocationCount === 0 || timeSinceLastInvocation > COLD_START_THRESHOLD_MS;
  
  if (isCold && invocationCount > 0) {
    // Container was reused after inactivity - reset metrics
    containerStartTime = now;
  }
  
  return isCold;
}

function getMemoryUsage() {
  const memUsage = process.memoryUsage();
  return {
    heapUsed: Math.round(memUsage.heapUsed / 1024 / 1024 * 100) / 100, // MB
    heapTotal: Math.round(memUsage.heapTotal / 1024 / 1024 * 100) / 100, // MB
    external: Math.round(memUsage.external / 1024 / 1024 * 100) / 100, // MB
    rss: Math.round(memUsage.rss / 1024 / 1024 * 100) / 100 // MB
  };
}

module.exports.hello = async (event, context) => {
  const handlerStartTime = performance.now();
  const requestId = context?.awsRequestId || 'local-test';
  const isCold = detectColdStart();
  
  invocationCount++;
  lastInvocationTime = handlerStartTime;
  
  // Simulate some work to measure handler execution time
  const workStartTime = performance.now();
  
  // Get system information
  const memoryUsage = getMemoryUsage();
  const cpuCount = os.cpus().length;
  
  const workEndTime = performance.now();
  const handlerEndTime = performance.now();
  
  // Calculate precise timings
  const timings = {
    moduleInitDurationMs: Math.round((handlerStartTime - MODULE_INIT_TIME) * 100) / 100,
    handlerDurationMs: Math.round((handlerEndTime - handlerStartTime) * 100) / 100,
    workDurationMs: Math.round((workEndTime - workStartTime) * 100) / 100,
    totalContainerAgeMs: Math.round((handlerStartTime - containerStartTime) * 100) / 100
  };
  
  // Standardized response format
  const response = {
    // Basic info
    message: 'HELLO WORLD',
    runtime: 'nodejs22.x',
    timestamp: new Date().toISOString(),
    requestId,
    
    // Cold start detection
    coldStart: isCold,
    invocationCount,
    
    // Timing metrics (all in milliseconds with 2 decimal precision)
    timings,
    
    // System metrics
    system: {
      memoryUsage,
      cpuCount,
      platform: os.platform(),
      arch: os.arch(),
      nodeVersion: process.version
    },
    
    // Container lifecycle
    container: {
      initTimestamp: MODULE_INIT_DATE,
      ageMs: timings.totalContainerAgeMs
    }
  };
  
  // Structured logging
  console.log(JSON.stringify({
    level: 'INFO',
    timestamp: response.timestamp,
    requestId,
    coldStart: isCold,
    invocationCount,
    timings,
    memoryUsage: memoryUsage.heapUsed,
    message: 'Lambda invocation completed'
  }));
  
  return {
    statusCode: 200,
    headers: {
      'Content-Type': 'application/json',
      'X-Cold-Start': isCold.toString(),
      'X-Invocation-Count': invocationCount.toString(),
      'X-Handler-Duration-Ms': timings.handlerDurationMs.toString(),
      'X-Runtime': 'nodejs22.x'
    },
    body: JSON.stringify(response, null, 2)
  };
};
