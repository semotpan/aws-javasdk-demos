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

        System.out.println("\n🚀 Starting Best Practice Flight Booking Scenario (using DynamoClient with ConditionalExpression) ...");

        try (var dynamoClient = AwsClientProvider.dynamoDbClient()) {

            var conditionalExpressionBookFlightRepository = new ConditionalExpressionBookFlightRepository(dynamoClient);
            var bookFlightService = new NoLockingBookFlightService(conditionalExpressionBookFlightRepository);

            var bookings = new CopyOnWriteArrayList<Booking>();

            Runnable bookingTask = () -> {
                var bookingId = UUID.randomUUID().toString();
                System.out.println("\n🛫 Attempting to book a flight (Thread: " + Thread.currentThread().getName() + ")");

                var newBooking = Booking.builder()
                        .customerEmail("sherlock.homes@email.com")
                        .bookingID(bookingId)
                        .flightNumber("BA123")
                        .source("LHR")
                        .destination("CDG")
                        .departureDateTime(1765792800L)  // 2025-12-15T10:00
                        .seatNumber("2C")  // Conflict! Both users try to book the same seat
                        .fareClass("Economy")
                        .build();

                bookings.add(newBooking);

                var success = bookFlightService.bookFlight(newBooking);
                System.out.println(
                        (success ? "✅ " : "❌ ") + "Booking (ID: " + bookingId + ") result: " + success
                                + " [Thread: " + Thread.currentThread().getName() + "]"
                );
            };

            var futures = List.of(
                    CompletableFuture.runAsync(bookingTask),
                    CompletableFuture.runAsync(bookingTask)
            );

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        System.out.println("\n📊 Fetching updated flight and booking details...");

                        var primaryKey = FlightPrimaryKey.builder()
                                .sourceAirportCode("LHR")
                                .destinationAirportCode("CDG")
                                .departureDateTime(LocalDateTime.of(2025, 12, 15, 10, 0))
                                .build();

                        System.out.println("\n✈️ Updated Flight Information:");
                        var enhancedClientFlightBookingsRepository =
                                new EnhancedClientFlightBookingsRepository(AwsClientProvider.dynamoDbEnhancedClient(dynamoClient));
                        enhancedClientFlightBookingsRepository.findFlight(primaryKey)
                                .ifPresentOrElse(
                                        flight -> System.out.println("📌 " + flight),
                                        () -> System.out.println("⚠️ Flight details not found!")
                                );

                        System.out.println("\n📌 Attempted Bookings:");
                        bookings.forEach(booking ->
                                enhancedClientFlightBookingsRepository.findBooking(booking.getCustomerEmail(), booking.getBookingID())
                                        .ifPresentOrElse(
                                                storedBooking -> System.out.println("✅ " + storedBooking),
                                                () -> System.out.println("❌ Booking not found in DB: " + booking.getBookingID())
                                        )
                        );
                    })
                    .join();

            System.out.println("\n🏁 Booking scenario completed.");
        }
    }
}

