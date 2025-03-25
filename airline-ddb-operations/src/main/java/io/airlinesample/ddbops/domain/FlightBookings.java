package io.airlinesample.ddbops.domain;

import java.util.Optional;

/**
 * Interface for flight booking operations, providing methods for
 * querying and transactional booking flights in a consistent manner.
 */
public interface FlightBookings {

    /**
     * Finds a flight by its primary key.
     *
     * @param primaryKey the primary key of the flight
     * @return an {@code Optional} containing the flight if found, or empty otherwise
     */
    Optional<Flight> findFlight(FlightPrimaryKey primaryKey);

    /**
     * Finds a booking by the customer's email and booking ID.
     *
     * @param customerEmail Partition Key
     * @param bookingID     Sort Key
     * @return an {@code Optional} containing the booking if found, or empty otherwise
     */
    Optional<Booking> findBooking(String customerEmail, String bookingID);

    /**
     * Performs a transactional operation to book a flight.
     * This operation decrements available seats and, optionally, assigns a seat.
     *
     * @param booking the booking details
     * @param flight  the flight details
     * @return a {@code TransactSummary} summarizing the transaction outcome
     */
    TransactSummary transactBookFlight(Booking booking, Flight flight);

    /**
     * Interface for summarizing the result of a flight booking transaction.
     */
    interface TransactSummary {

        /**
         * Checks if the transaction was successful.
         */
        boolean success();

        /**
         * Indicates a precondition failure, such as insufficient seats or conflicts.
         */
        boolean preconditionFailed();

        /**
         * Indicates that the transaction was cancelled, either due to
         * condition expression failures or timeouts.
         */
        boolean transactionCancelled();

        /**
         * Indicates a generic failure, such as network issues or invalid input.
         */
        boolean genericFailure();

        /**
         * Provides a reason for the failure, if applicable.
         */
        String failureReason();
    }
}

