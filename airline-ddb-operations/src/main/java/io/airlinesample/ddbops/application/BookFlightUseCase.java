package io.airlinesample.ddbops.application;

import io.airlinesample.ddbops.domain.Booking;

public interface BookFlightUseCase {

    boolean bookFlight(Booking booking);

}
