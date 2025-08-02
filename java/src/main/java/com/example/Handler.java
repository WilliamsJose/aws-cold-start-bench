package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

// Java 21 Records for structured data
record TimingMetrics(
    double moduleInitDurationMs,
    double handlerDurationMs,
    double workDurationMs,
    double totalContainerAgeMs
) {}

record MemoryMetrics(
    double heapUsed,
    double heapMax,
    double nonHeapUsed
) {}

record SystemInfo(
    int cpuCount,
    String platform,
    String arch,
    String javaVersion
) {}

record ContainerInfo(
    String initTimestamp,
    double ageMs
) {}

public class Handler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    // Module initialization timestamp
    private static final long MODULE_INIT_TIME = System.nanoTime();
    private static final String MODULE_INIT_DATE = Instant.now().toString();
    
    // Cold start detection with more robust logic
    private static long containerStartTime = MODULE_INIT_TIME;
    private static int invocationCount = 0;
    private static long lastInvocationTime = 0;
    
    // Cold start threshold: 5 minutes of inactivity
    private static final long COLD_START_THRESHOLD_MS = 5 * 60 * 1000;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    private static boolean detectColdStart() {
        long now = System.nanoTime();
        long timeSinceLastInvocation = (now - lastInvocationTime) / 1_000_000; // Convert to ms
        
        // First invocation or long period of inactivity
        boolean isCold = invocationCount == 0 || timeSinceLastInvocation > COLD_START_THRESHOLD_MS;
        
        if (isCold && invocationCount > 0) {
            // Container was reused after inactivity - reset metrics
            containerStartTime = now;
        }
        
        return isCold;
    }
    
    private static MemoryMetrics getMemoryUsage() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        
        return new MemoryMetrics(
            Math.round(heapUsage.getUsed() / 1024.0 / 1024.0 * 100.0) / 100.0, // MB
            Math.round(heapUsage.getMax() / 1024.0 / 1024.0 * 100.0) / 100.0, // MB
            Math.round(nonHeapUsage.getUsed() / 1024.0 / 1024.0 * 100.0) / 100.0 // MB
        );
    }
    
    private static SystemInfo getSystemInfo() {
        return new SystemInfo(
            Runtime.getRuntime().availableProcessors(),
            System.getProperty("os.name"),
            System.getProperty("os.arch"),
            System.getProperty("java.version")
        );
    }

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        long handlerStartTime = System.nanoTime();
        String requestId = context != null ? context.getAwsRequestId() : "local-test";
        boolean isCold = detectColdStart();
        
        invocationCount++;
        lastInvocationTime = handlerStartTime;
        
        // Simulate some work to measure handler execution time
        long workStartTime = System.nanoTime();
        
        // Get system information
        MemoryMetrics memoryUsage = getMemoryUsage();
        SystemInfo systemInfo = getSystemInfo();
        
        long workEndTime = System.nanoTime();
        long handlerEndTime = System.nanoTime();
        
        // Calculate precise timings using Java 21 records
        TimingMetrics timings = new TimingMetrics(
            Math.round((handlerStartTime - MODULE_INIT_TIME) / 1_000_000.0 * 100.0) / 100.0,
            Math.round((handlerEndTime - handlerStartTime) / 1_000_000.0 * 100.0) / 100.0,
            Math.round((workEndTime - workStartTime) / 1_000_000.0 * 100.0) / 100.0,
            Math.round((handlerStartTime - containerStartTime) / 1_000_000.0 * 100.0) / 100.0
        );
        
        // Container lifecycle info using record
        ContainerInfo containerInfo = new ContainerInfo(
            MODULE_INIT_DATE,
            timings.totalContainerAgeMs()
        );
        
        // System metrics combining memory and system info
        Map<String, Object> system = Map.of(
            "memoryUsage", Map.of(
                "heapUsed", memoryUsage.heapUsed(),
                "heapMax", memoryUsage.heapMax(),
                "nonHeapUsed", memoryUsage.nonHeapUsed()
            ),
            "cpuCount", systemInfo.cpuCount(),
            "platform", systemInfo.platform(),
            "arch", systemInfo.arch(),
            "javaVersion", systemInfo.javaVersion()
        );
        
        // Standardized response format using Java 21 features
        var timestamp = Instant.now().toString();
        Map<String, Object> response = Map.of(
            "message", "HELLO WORLD",
            "runtime", "java21",
            "timestamp", timestamp,
            "requestId", requestId,
            "coldStart", isCold,
            "invocationCount", invocationCount,
            "timings", Map.of(
                "moduleInitDurationMs", timings.moduleInitDurationMs(),
                "handlerDurationMs", timings.handlerDurationMs(),
                "workDurationMs", timings.workDurationMs(),
                "totalContainerAgeMs", timings.totalContainerAgeMs()
            ),
            "system", system,
            "container", Map.of(
                "initTimestamp", containerInfo.initTimestamp(),
                "ageMs", containerInfo.ageMs()
            )
        );
        
        // Structured logging using immutable maps
        Map<String, Object> logEntry = Map.of(
            "level", "INFO",
            "timestamp", timestamp,
            "requestId", requestId,
            "coldStart", isCold,
            "invocationCount", invocationCount,
            "timings", Map.of(
                "moduleInitDurationMs", timings.moduleInitDurationMs(),
                "handlerDurationMs", timings.handlerDurationMs(),
                "workDurationMs", timings.workDurationMs(),
                "totalContainerAgeMs", timings.totalContainerAgeMs()
            ),
            "memoryUsage", memoryUsage.heapUsed(),
            "message", "Lambda invocation completed"
        );
        
        try {
            System.out.println(objectMapper.writeValueAsString(logEntry));
        } catch (Exception e) {
            System.err.println("Failed to log: " + e.getMessage());
        }
        
        // Response headers using immutable Map
        Map<String, String> headers = Map.of(
            "Content-Type", "application/json",
            "X-Cold-Start", String.valueOf(isCold),
            "X-Invocation-Count", String.valueOf(invocationCount),
            "X-Handler-Duration-Ms", String.valueOf(timings.handlerDurationMs()),
            "X-Runtime", "java21"
        );
        
        try {
            String responseBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(responseBody)
                    .build();
        } catch (Exception e) {
            System.err.println("Failed to serialize response: " + e.getMessage());
            return APIGatewayV2HTTPResponse.builder()
                    .withStatusCode(500)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody("{\"error\":\"Internal server error\"}")
                    .build();
        }
    }
}
