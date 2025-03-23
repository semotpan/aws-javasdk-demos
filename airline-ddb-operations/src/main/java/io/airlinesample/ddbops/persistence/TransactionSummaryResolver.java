package io.airlinesample.ddbops.persistence;

import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

import static software.amazon.awssdk.services.dynamodb.model.BatchStatementErrorCodeEnum.CONDITIONAL_CHECK_FAILED;

final class TransactionSummaryResolver {

    private final DynamoTransactSummary dynamoTransactSummary;

    TransactionSummaryResolver() {
        this.dynamoTransactSummary = DynamoTransactSummary.builder()
                .success(true)
                .build();
    }

    TransactionSummaryResolver(TransactionCanceledException e) {
        // Check if the cancellation reason is due to optimistic locking (version mismatch)
        //!!! @DynamoDbVersionAttribute - applies default ExpressionCondition on Version field on EnhancedClient usage
        var isPreconditionFailed = e.cancellationReasons().stream()
                .map(CancellationReason::code)
                .anyMatch(code -> code.equals(CONDITIONAL_CHECK_FAILED.toString()));

        if (isPreconditionFailed) {
            this.dynamoTransactSummary = DynamoTransactSummary.builder()
                    .preconditionFailed(true)
                    .transactionCancelled(true)
                    .failureReason("Optimistic locking failed: Another user modified the flight concurrently.")
                    .build();
            return;
        }

        this.dynamoTransactSummary = DynamoTransactSummary.builder()
                .transactionCancelled(true)
                .failureReason("Transaction canceled: " + e.getMessage())
                .build();
    }

    TransactionSummaryResolver(DynamoDbException e) {
        this.dynamoTransactSummary = DynamoTransactSummary.builder()
                .genericFailure(true)
                .failureReason("Transaction failed: " + e.getMessage())
                .build();
    }

    DynamoTransactSummary dynamoTransactSummary() {
        return this.dynamoTransactSummary;
    }
}
