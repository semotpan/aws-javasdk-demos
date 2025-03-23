package io.airlinesample.ddbops;

import io.airlinesample.ddbops.application.BookFlightOptimisticLockingService;
import io.airlinesample.ddbops.domain.Booking;
import io.airlinesample.ddbops.domain.FlightPrimaryKey;
import io.airlinesample.ddbops.persistence.EnhancedClientFlightBookingsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class BookFlightOptimisticLockingScenario {

    public static void main(String[] args) {

        try (var dynamoDbClient = AwsClientProvider.dynamoDbClient()) {

            var enhancedClient = AwsClientProvider.dynamoDbEnhancedClient(dynamoDbClient);
            var enhancedClientFlightBookingsRepository = new EnhancedClientFlightBookingsRepository(enhancedClient);
            var bookFlightUseCase = new BookFlightOptimisticLockingService(enhancedClientFlightBookingsRepository);


            var bookings = new CopyOnWriteArrayList<Booking>();
            // Create booking tasks
            Runnable bookingTask = () -> {
                var newBooking = Booking.builder()
                        .customerEmail("sherlock.homes@email.com")
                        .bookingID(UUID.randomUUID().toString())
                        .flightNumber("KL456")
                        .source("AMS")
                        .destination("FRA")
                        .departureDateTime(1747296000L)  // 2025-05-15T08:00
                        .seatNumber("2D")  // Conflict! Both users try to book seat 2D
                        .fareClass("Economy")
                        .build();

                bookings.add(newBooking);

                var success = bookFlightUseCase.bookFlight(newBooking);
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
                                .sourceAirportCode("AMS")
                                .destinationAirportCode("FRA")
                                .departureDateTime(LocalDateTime.of(2025, 5, 15, 8, 0))
                                .build();
                        System.out.println("Created records:");
                        enhancedClientFlightBookingsRepository.findFlight(primaryKey)
                                .ifPresent(System.out::println);

                        bookings.forEach(booking ->
                                enhancedClientFlightBookingsRepository.findBooking(booking.getCustomerEmail(), booking.getBookingID())
                                        .ifPresent(System.out::println));
                    })
                    .join(); // Ensures the main thread waits for all async tasks to finish
        }
    }
}
