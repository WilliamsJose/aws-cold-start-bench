using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text.Json;
using System.Text.Json.Serialization;
using Amazon.Lambda.Core;
using Amazon.Lambda.APIGatewayEvents;

[assembly: LambdaSerializer(typeof(Amazon.Lambda.Serialization.SystemTextJson.DefaultLambdaJsonSerializer))]

namespace ColdstartDotnet
{
    public class Function
    {
        // Module initialization timestamp
        private static readonly long ModuleInitTime = Stopwatch.GetTimestamp();
        private static readonly string ModuleInitDate = DateTime.UtcNow.ToString("O");
        
        // Cold start detection with more robust logic
        private static long containerStartTime = ModuleInitTime;
        private static int invocationCount = 0;
        private static long lastInvocationTime = 0;
        
        // Cold start threshold: 5 minutes of inactivity
        private static readonly long ColdStartThresholdMs = 5 * 60 * 1000;
        
        private static readonly JsonSerializerOptions JsonOptions = new JsonSerializerOptions
        {
            WriteIndented = true,
            PropertyNamingPolicy = JsonNamingPolicy.CamelCase
        };
        
        private static bool DetectColdStart()
        {
            var now = Stopwatch.GetTimestamp();
            var timeSinceLastInvocation = (now - lastInvocationTime) * 1000.0 / Stopwatch.Frequency; // Convert to ms
            
            // First invocation or long period of inactivity
            var isCold = invocationCount == 0 || timeSinceLastInvocation > ColdStartThresholdMs;
            
            if (isCold && invocationCount > 0)
            {
                // Container was reused after inactivity - reset metrics
                containerStartTime = now;
            }
            
            return isCold;
        }
        
        private static Dictionary<string, object> GetMemoryUsage()
        {
            var process = Process.GetCurrentProcess();
            var gcMemory = GC.GetTotalMemory(false);
            
            return new Dictionary<string, object>
            {
                ["workingSet"] = Math.Round(process.WorkingSet64 / 1024.0 / 1024.0, 2), // MB
                ["privateMemory"] = Math.Round(process.PrivateMemorySize64 / 1024.0 / 1024.0, 2), // MB
                ["gcMemory"] = Math.Round(gcMemory / 1024.0 / 1024.0, 2), // MB
                ["gen0Collections"] = GC.CollectionCount(0),
                ["gen1Collections"] = GC.CollectionCount(1),
                ["gen2Collections"] = GC.CollectionCount(2)
            };
        }
        
        private static Dictionary<string, object> GetSystemInfo()
        {
            return new Dictionary<string, object>
            {
                ["cpuCount"] = Environment.ProcessorCount,
                ["platform"] = Environment.OSVersion.Platform.ToString(),
                ["arch"] = Environment.Is64BitProcess ? "x64" : "x86",
                ["dotnetVersion"] = Environment.Version.ToString(),
                ["frameworkDescription"] = System.Runtime.InteropServices.RuntimeInformation.FrameworkDescription
            };
        }
        
        private static double GetElapsedMilliseconds(long startTimestamp, long endTimestamp)
        {
            return Math.Round((endTimestamp - startTimestamp) * 1000.0 / Stopwatch.Frequency, 2);
        }

        public APIGatewayProxyResponse FunctionHandler(APIGatewayProxyRequest request, ILambdaContext context)
        {
            var handlerStartTime = Stopwatch.GetTimestamp();
            var requestId = context?.AwsRequestId ?? "local-test";
            var isCold = DetectColdStart();
            
            invocationCount++;
            lastInvocationTime = handlerStartTime;
            
            // Simulate some work to measure handler execution time
            var workStartTime = Stopwatch.GetTimestamp();
            
            // Get system information
            var memoryUsage = GetMemoryUsage();
            var systemInfo = GetSystemInfo();
            
            var workEndTime = Stopwatch.GetTimestamp();
            var handlerEndTime = Stopwatch.GetTimestamp();
            
            // Calculate precise timings (all in milliseconds with 2 decimal precision)
            var timings = new Dictionary<string, object>
            {
                ["moduleInitDurationMs"] = GetElapsedMilliseconds(ModuleInitTime, handlerStartTime),
                ["handlerDurationMs"] = GetElapsedMilliseconds(handlerStartTime, handlerEndTime),
                ["workDurationMs"] = GetElapsedMilliseconds(workStartTime, workEndTime),
                ["totalContainerAgeMs"] = GetElapsedMilliseconds(containerStartTime, handlerStartTime)
            };
            
            // Container lifecycle info
            var containerInfo = new Dictionary<string, object>
            {
                ["initTimestamp"] = ModuleInitDate,
                ["ageMs"] = timings["totalContainerAgeMs"]
            };
            
            // System metrics
            var system = new Dictionary<string, object>
            {
                ["memoryUsage"] = memoryUsage
            };
            foreach (var kvp in systemInfo)
            {
                system[kvp.Key] = kvp.Value;
            }
            
            // Standardized response format
            var response = new Dictionary<string, object>
            {
                ["message"] = "HELLO WORLD",
                ["runtime"] = "dotnet8",
                ["timestamp"] = DateTime.UtcNow.ToString("O"),
                ["requestId"] = requestId,
                ["coldStart"] = isCold,
                ["invocationCount"] = invocationCount,
                ["timings"] = timings,
                ["system"] = system,
                ["container"] = containerInfo
            };
            
            // Structured logging
            var logEntry = new Dictionary<string, object>
            {
                ["level"] = "INFO",
                ["timestamp"] = response["timestamp"],
                ["requestId"] = requestId,
                ["coldStart"] = isCold,
                ["invocationCount"] = invocationCount,
                ["timings"] = timings,
                ["memoryUsage"] = memoryUsage["workingSet"],
                ["message"] = "Lambda invocation completed"
            };
            
            try
            {
                Console.WriteLine(JsonSerializer.Serialize(logEntry));
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Failed to log: {ex.Message}");
            }
            
            // Response headers
            var headers = new Dictionary<string, string>
            {
                ["Content-Type"] = "application/json",
                ["X-Cold-Start"] = isCold.ToString().ToLower(),
                ["X-Invocation-Count"] = invocationCount.ToString(),
                ["X-Handler-Duration-Ms"] = timings["handlerDurationMs"].ToString(),
                ["X-Runtime"] = "dotnet8"
            };
            
            try
            {
                var responseBody = JsonSerializer.Serialize(response, JsonOptions);
                return new APIGatewayProxyResponse
                {
                    StatusCode = 200,
                    Headers = headers,
                    Body = responseBody
                };
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"Failed to serialize response: {ex.Message}");
                return new APIGatewayProxyResponse
                {
                    StatusCode = 500,
                    Headers = new Dictionary<string, string> { ["Content-Type"] = "application/json" },
                    Body = "{\"error\":\"Internal server error\"}"
                };
            }
        }
    }
}
