package io.airlinesample.ddbops;

import io.airlinesample.ddbops.application.NoLockingBookFlightService;
import io.airlinesample.ddbops.domain.Booking;
import io.airlinesample.ddbops.domain.FlightPrimaryKey;
import io.airlinesample.ddbops.persistence.SimpleClientBookFlightRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class BookFlightNoLockingScenario {

    public static void main(String[] args) {

        try (var dynamoClient = AwsClientProvider.dynamoDbClient()) {

            var simpleClientBookFlightRepository = new SimpleClientBookFlightRepository(dynamoClient);
            var noLockingBookFlightUseCase = new NoLockingBookFlightService(simpleClientBookFlightRepository);

            var bookings = new CopyOnWriteArrayList<Booking>();
            // Create booking tasks
            Runnable bookingTask = () -> {
                Booking newBooking = Booking.builder()
                        .customerEmail("sherlock.homes@email.com")
                        .bookingID(UUID.randomUUID().toString())
                        .flightNumber("BA123")
                        .source("LHR")
                        .destination("CDG")
                        .departureDateTime(1765792800L)  // 2025-12-15T10:00
//                        .seatNumber("4C")  // Conflict! Both users try to book seat
                        .fareClass("Economy")
                        .build();

                bookings.add(newBooking);

                var success = noLockingBookFlightUseCase.bookFlight(newBooking);
                System.out.println(Thread.currentThread().getName() + " - Booking success: " + success);
            };

            // Execute two concurrent booking tasks using CompletableFuture
            var futures = List.of(
                    CompletableFuture.runAsync(bookingTask),
                    CompletableFuture.runAsync(bookingTask)
            );

            // Wait for both tasks to complete before querying the updated flight
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        var primaryKey = FlightPrimaryKey.builder()
                                .sourceAirportCode("LHR")
                                .destinationAirportCode("CDG")
                                .departureDateTime(LocalDateTime.of(2025, 12, 15, 10, 0))
                                .build();
                        simpleClientBookFlightRepository.findFlight(primaryKey)
                                .ifPresent(System.out::println);

                        bookings.forEach(booking ->
                                simpleClientBookFlightRepository.findBooking(booking.getCustomerEmail(), booking.getBookingID())
                                        .ifPresent(System.out::println));
                    })
                    .join(); // Ensure the main thread waits for all async tasks to finish
        }
    }
}
