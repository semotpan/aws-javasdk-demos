package io.airlinesample.ddbops;

import io.airlinesample.ddbops.application.OptimisticLockingFlightBookingService;
import io.airlinesample.ddbops.domain.Booking;
import io.airlinesample.ddbops.domain.FlightPrimaryKey;
import io.airlinesample.ddbops.persistence.EnhancedClientFlightBookingsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class simulates a **less efficient** flight booking process using **Optimistic Locking** with the
 * DynamoDB Enhanced Client API. While the approach ensures data consistency, it introduces inefficiencies compared to
 * direct transaction management.
 * <p>
 * In this simulation:
 * <ul>
 *     <li>Two concurrent users attempt to book the same seat (2D) on the same flight, resulting in a conflict.</li>
 *     <li>Optimistic Locking is used to resolve conflicts, but it incurs the overhead of retrying transactions in case of conflicts.</li>
 *     <li>The **DynamoDB Enhanced Client** handles the Version constraint using the @DynamoDbVersionAttribute annotation, simplifying version management.</li>
 *     <li>The Enhanced Client provides a clean API but comes with limitations in flexibility compared to other approaches.</li>
 * </ul>
 * <p>
 * This scenario highlights the **inefficiency** of this approach, where:
 * <ul>
 *     <li>Optimistic Locking introduces retries and revalidation, leading to increased processing time compared to simpler transaction-based methods.</li>
 * </ul>
 */
public class EnhancedClientOptimisticLockingBookFlightScenarioRunner {

    public static void main(String[] args) {

        try (var dynamoDbClient = AwsClientProvider.dynamoDbClient()) {

            var enhancedClient = AwsClientProvider.dynamoDbEnhancedClient(dynamoDbClient);
            var enhancedClientFlightBookingsRepository = new EnhancedClientFlightBookingsRepository(enhancedClient);
            var bookFlightUseCase = new OptimisticLockingFlightBookingService(enhancedClientFlightBookingsRepository);

            var bookings = new CopyOnWriteArrayList<Booking>();
            // Create booking tasks
            Runnable bookingTask = () -> {
                // Create a new booking that simulates a conflict (same seat)
                // Amsterdam Schiphol (AMS) → Frankfurt Airport (FRA) Departure DateTime: 2025-05-15T08:00
                // Passenger: Sherlock Homes
                // Seat: 2D
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
                        System.out.println("Database stats:");
                        // Use the enhanced client to query flight details after booking attempts
                        enhancedClientFlightBookingsRepository.findFlight(primaryKey)
                                .ifPresent(System.out::println); // Print the updated flight details

                        // Query and print the details of all attempted bookings
                        bookings.forEach(booking ->
                                enhancedClientFlightBookingsRepository.findBooking(booking.getCustomerEmail(), booking.getBookingID())
                                        .ifPresent(System.out::println));
                    })
                    .join(); // Ensures the main thread waits for all async tasks to finish
        }
    }
}
