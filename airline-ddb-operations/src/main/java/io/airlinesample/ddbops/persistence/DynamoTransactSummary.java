package io.airlinesample.ddbops.persistence;

import io.airlinesample.ddbops.domain.FlightBookings;
import lombok.Builder;

@Builder
record DynamoTransactSummary(boolean success,
                             boolean preconditionFailed,
                             boolean transactionCancelled,
                             boolean genericFailure,
                             String failureReason) implements FlightBookings.TransactSummary {
}
