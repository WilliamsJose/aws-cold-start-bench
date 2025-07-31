package com.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;

public class Handler implements RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private static boolean isCold = true;

    @Override
    public APIGatewayV2HTTPResponse handleRequest(APIGatewayV2HTTPEvent event, Context context) {
        long start = System.nanoTime();

        String body = String.format(
            "{\"message\":\"HELLO WORLD\", \"runtime\":\"java17\", \"coldStart\":%b, \"handlerDurationMs\":%d}",
            isCold,
            (System.nanoTime() - start) / 1_000_000
        );

        isCold = false;

        return APIGatewayV2HTTPResponse.builder()
                .withStatusCode(200)
                .withHeaders(java.util.Map.of("Content-Type", "application/json"))
                .withBody(body)
                .build();
    }
}
