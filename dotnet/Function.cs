using System;
using System.Collections.Generic;
using Amazon.Lambda.Core;
using Amazon.Lambda.APIGatewayEvents;

[assembly: LambdaSerializer(typeof(Amazon.Lambda.Serialization.SystemTextJson.DefaultLambdaJsonSerializer))]

namespace ColdstartDotnet
{
    public class Function
    {
        private static bool isCold = true;

        public APIGatewayProxyResponse FunctionHandler(APIGatewayProxyRequest request, ILambdaContext context)
        {
            var start = DateTime.UtcNow.Ticks;

            var body = new Dictionary<string, object>
            {
                ["message"] = "HELLO WORLD",
                ["runtime"] = "dotnet8",
                ["coldStart"] = isCold
            };

            isCold = false;

            var durationMs = (DateTime.UtcNow.Ticks - start) / TimeSpan.TicksPerMillisecond;
            body["handlerDurationMs"] = durationMs;

            return new APIGatewayProxyResponse
            {
                StatusCode = 200,
                Headers = new Dictionary<string, string>
                {
                    ["Content-Type"] = "application/json"
                },
                Body = System.Text.Json.JsonSerializer.Serialize(body)
            };
        }
    }
}
