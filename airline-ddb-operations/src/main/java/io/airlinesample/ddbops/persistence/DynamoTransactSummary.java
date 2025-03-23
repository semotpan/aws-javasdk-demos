package io.airlinesample.ddbops.persistence;

import io.airlinesample.ddbops.domain.FlightBookings;
import lombok.Builder;

@Builder
record DynamoTransactSummary(boolean success,
                             boolean preconditionFailed,
                             boolean transactionCancelled,
                             boolean genericFailure,
                             String failureReason) implements FlightBookings.TransactSummary {

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public boolean isPreconditionFailed() {
        return preconditionFailed;
    }

    @Override
    public boolean isTransactionCancelled() {
        return transactionCancelled;
    }

    @Override
    public boolean isGenericFailure() {
        return genericFailure;
    }

    @Override
    public String failureReason() {
        return failureReason;
    }
}
