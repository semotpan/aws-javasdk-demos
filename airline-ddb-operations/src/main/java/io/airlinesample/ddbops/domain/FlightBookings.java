package io.airlinesample.ddbops.domain;

import java.util.Optional;

public interface FlightBookings {

    Optional<Flight> findFlight(FlightPrimaryKey primaryKey);

    Optional<Booking> findBooking(String customerEmail, String bookingID);

    TransactSummary transactBookFlight(Booking booking, Flight flight);

    interface TransactSummary {

        // in case of success
        boolean isSuccess();

        // In case of ConditionExpressions: it could be OptimisticLocking or other checks
        boolean isPreconditionFailed();

        // in case of ConditionExpressions, or in case of timeouts or other cancellations
        boolean isTransactionCancelled();

        // Handle DynamoDB-specific errors such as network issues or invalid input
        boolean isGenericFailure();

        // Reason message
        String failureReason();
    }
}
