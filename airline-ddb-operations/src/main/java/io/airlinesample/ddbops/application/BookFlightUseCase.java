package io.airlinesample.ddbops.application;

import io.airlinesample.ddbops.domain.Booking;

/**
 * Interface for the use case of booking a flight.
 * <p>
 * This interface defines the contract for the functionality required to book a flight.
 * It includes a single method to process a booking, ensuring that the flight booking logic
 * is executed properly and returns an appropriate result.
 */
public interface BookFlightUseCase {

    /**
     * Books a flight for a given booking.
     * <p>
     * This method processes the flight booking by validating the booking details,
     * updating the necessary flight and booking records, and returning the result of the operation.
     *
     * @param booking The booking information for the flight.
     * @return true if the booking was successful, false otherwise.
     */
    boolean bookFlight(Booking booking);
}
