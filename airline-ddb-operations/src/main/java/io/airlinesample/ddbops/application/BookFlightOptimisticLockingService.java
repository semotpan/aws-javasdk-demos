package io.airlinesample.ddbops.application;

import io.airlinesample.ddbops.domain.Booking;
import io.airlinesample.ddbops.domain.FlightBookings;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class BookFlightOptimisticLockingService implements BookFlightUseCase {

    private final FlightBookings flightBookings;

    @Override
    public boolean bookFlight(Booking booking) {

        // Step 1: Fetch the flight information from DynamoDB based on booking details
        var possibleFlight = flightBookings.findFlight(booking.flightPrimaryKey());
        if (possibleFlight.isEmpty()) {
            System.err.println("Flight not available for booking.");
            return false;  // Flight does not exist or could not be retrieved
        }
        var flight = possibleFlight.get();

        // Step 2: Check if the flight has available seats
        if (!flight.anySeatAvailable()) {
            System.err.println("No available seats for the flight: " + flight.getFlightNumber());
            return false;
        }

        if (booking.hasSeatNumber()) {
            // Step 3: Check if the requested seat is available and claim it
            if (!flight.addSeatIfAvailable(booking.getSeatNumber(), booking.getBookingID())) {
                System.err.println("The requested seat is already claimed.");
                return false;
            }
        } else {
            flight.incrementHeldSeats();
        }

        // Step 4: Decrement the available seats on the flight as the seat is now booked
        flight.decrementAvailableSeats();

        // Step 5: Submit changes in a transaction
        // FIXME: in case of retry consider the result of this
        var transactSummary = flightBookings.transactBookFlight(booking, flight);
        log(transactSummary);
        return transactSummary.isSuccess();
    }

    private void log(FlightBookings.TransactSummary transactSummary) {
        if (transactSummary.isSuccess()) {
            System.out.println("Flight booked successfully.");
            return;
        }

        if (transactSummary.isPreconditionFailed()) {
            System.err.println("Optimistic locking failed: Another user modified the flight concurrently. Consider attemting again ...");
            return;
        }

        System.err.println(transactSummary.failureReason());
    }
}
