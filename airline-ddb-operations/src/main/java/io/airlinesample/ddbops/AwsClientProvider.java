package io.airlinesample.ddbops;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

public class AwsClientProvider {

    private static final String ACCESS_KEY = "key";
    private static final String SECRET_KEY = "secret";
    private static final Region REGION = Region.EU_WEST_1;
    private static final URI ENDPOINT = URI.create("https://localhost.localstack.cloud:4566");

    public static DynamoDbClient dynamoDbClient() {
        AwsCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));

        return DynamoDbClient.builder()
                .region(REGION)
                .credentialsProvider(credentials)
                .endpointOverride(ENDPOINT)
                .build();
    }

    public static DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    public static DynamoDbAsyncClient dynamoDbAsyncClient() {
        AwsCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));

        return DynamoDbAsyncClient.builder()
                .region(REGION)
                .credentialsProvider(credentials)
                .endpointOverride(ENDPOINT)
                .build();
    }

    public static DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(DynamoDbAsyncClient dynamoDbAsyncClient) {
        return DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(dynamoDbAsyncClient)
                .build();
    }
}
