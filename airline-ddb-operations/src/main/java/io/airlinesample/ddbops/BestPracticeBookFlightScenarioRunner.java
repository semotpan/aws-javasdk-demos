package io.airlinesample.ddbops;

import io.airlinesample.ddbops.application.NoLockingBookFlightService;
import io.airlinesample.ddbops.domain.Booking;
import io.airlinesample.ddbops.domain.FlightPrimaryKey;
import io.airlinesample.ddbops.persistence.ConditionalExpressionBookFlightRepository;
import io.airlinesample.ddbops.persistence.EnhancedClientFlightBookingsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Demonstrates a high-concurrency flight booking scenario using DynamoDB transactions, showcasing a **best-practice approach** for efficiently
 * handling booking conflicts with DynamoDB’s ACID guarantees.
 * <p>
 * This simulation leverages DynamoDB transactions to:
 * <ul>
 *     <li>Ensure atomic updates without explicit locking mechanisms.</li>
 *     <li>Maintain scalability and performance under high concurrency.</li>
 *     <li>Resolve conflicts naturally with DynamoDB's built-in ACID support.</li>
 * </ul>
 * <p>
 * Key features:
 * - Two concurrent users attempt to book the same seat, resulting in a transaction conflict. Only one booking succeeds, ensuring data integrity.
 * <p>
 * The implementation demonstrates:
 * - Conditional expressions for efficient conflict detection.
 * - Integration of `ConditionalExpressionBookFlightRepository` and `NoLockingBookFlightService` for atomic operations.
 * - A complete flow from service initialization to concurrent task execution and querying updated records.
 * <p>
 * This example guides developers on best practices for transactional operations in high-concurrency, distributed environments using DynamoDB.
 */
public class BestPracticeBookFlightScenarioRunner {

    public static void main(String[] args) {

        // Initialize DynamoDB client and flight booking service
        try (var dynamoClient = AwsClientProvider.dynamoDbClient()) {

            // Using the best practice repository with conditional expressions for transaction-based updates
            var conditionalExpressionBookFlightRepository = new ConditionalExpressionBookFlightRepository(dynamoClient);
            var bookFlightService = new NoLockingBookFlightService(conditionalExpressionBookFlightRepository);

            var bookings = new CopyOnWriteArrayList<Booking>();

            // Runnable task simulating flight booking requests
            Runnable bookingTask = () -> {
                // Create a new booking that simulates a conflict (same seat)
                // London Heathrow (LHR) → Paris Charles de Gaulle (CDG), Departure DateTime: 2025-12-15T10:00
                // Passenger: Sherlock Homes
                // Seat: 2C
                var newBooking = Booking.builder()
                        .customerEmail("sherlock.homes@email.com")
                        .bookingID(UUID.randomUUID().toString())
                        .flightNumber("BA123")
                        .source("LHR")
                        .destination("CDG")
                        .departureDateTime(1765792800L)  // 2025-12-15T10:00
                        .seatNumber("2C")  // Conflict! Both users try to book the same seat
                        .fareClass("Economy")
                        .build();

                bookings.add(newBooking);

                var success = bookFlightService.bookFlight(newBooking);
                System.out.println(Thread.currentThread().getName() + " - Booking success: " + success);
            };

            // Run two concurrent booking tasks using CompletableFuture
            var futures = List.of(
                    CompletableFuture.runAsync(bookingTask),
                    CompletableFuture.runAsync(bookingTask)
            );

            // Wait for both tasks to complete before querying the updated flight and booking details
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        // Define the flight primary key for querying flight details after booking attempts
                        var primaryKey = FlightPrimaryKey.builder()
                                .sourceAirportCode("LHR")
                                .destinationAirportCode("CDG")
                                .departureDateTime(LocalDateTime.of(2025, 12, 15, 10, 0))
                                .build();

                        System.out.println("Database stats:");
                        // Use the enhanced client to query flight details after booking attempts
                        var enhancedClientFlightBookingsRepository = new EnhancedClientFlightBookingsRepository(AwsClientProvider.dynamoDbEnhancedClient(dynamoClient));
                        enhancedClientFlightBookingsRepository.findFlight(primaryKey)
                                .ifPresent(System.out::println);  // Print the updated flight details

                        // Query and print the details of all attempted bookings
                        bookings.forEach(booking ->
                                enhancedClientFlightBookingsRepository.findBooking(booking.getCustomerEmail(), booking.getBookingID())
                                        .ifPresent(System.out::println));
                    })
                    .join();  // Ensure the main thread waits for all async tasks to finish
        }
    }
}
