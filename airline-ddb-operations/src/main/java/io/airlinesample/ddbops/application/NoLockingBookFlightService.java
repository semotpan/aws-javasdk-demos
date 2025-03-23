package io.airlinesample.ddbops.application;

import io.airlinesample.ddbops.domain.Booking;
import io.airlinesample.ddbops.domain.FlightBookings;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class NoLockingBookFlightService implements BookFlightUseCase {

    private final FlightBookings simpleClientBookFlightBookings;

    @Override
    public boolean bookFlight(Booking booking) {
        var transactSummary = simpleClientBookFlightBookings.transactBookFlight(booking, null);
        log(transactSummary);
        return transactSummary.isSuccess();

    }

    private void log(FlightBookings.TransactSummary transactSummary) {
        if (transactSummary.isSuccess()) {
            System.out.println("Flight booked successfully.");
            return;
        }

        if (transactSummary.isPreconditionFailed()) {
            System.out.println("No seats available or specified seat already taken.");
            System.err.println("Optimistic locking failed: Another user modified the flight concurrently. Consider attemting again ...");
            return;
        }

        System.err.println(transactSummary.failureReason());
    }
}
