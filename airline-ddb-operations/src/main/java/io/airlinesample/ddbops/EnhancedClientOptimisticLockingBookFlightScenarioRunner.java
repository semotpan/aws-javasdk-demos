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

        System.out.println("\n🚀 Starting Optimistic Locking Booking Scenario (using EnhancedClient) ...");

        try (var dynamoDbClient = AwsClientProvider.dynamoDbClient()) {

            var enhancedClient = AwsClientProvider.dynamoDbEnhancedClient(dynamoDbClient);
            var enhancedClientFlightBookingsRepository = new EnhancedClientFlightBookingsRepository(enhancedClient);
            var bookFlightUseCase = new OptimisticLockingFlightBookingService(enhancedClientFlightBookingsRepository);

            var bookings = new CopyOnWriteArrayList<Booking>();

            Runnable bookingTask = () -> {
                var bookingId = UUID.randomUUID().toString();
                System.out.println("\n🛫 Attempting to book a flight (Thread: " + Thread.currentThread().getName() + ")");

                var newBooking = Booking.builder()
                        .customerEmail("sherlock.homes@email.com")
                        .bookingID(bookingId)
                        .flightNumber("KL456")
                        .source("AMS")
                        .destination("FRA")
                        .departureDateTime(1747296000L)  // 2025-05-15T08:00
                        .seatNumber("2D")  // Conflict! Both users try to book seat 2D
                        .fareClass("Economy")
                        .build();

                bookings.add(newBooking);

                var success = bookFlightUseCase.bookFlight(newBooking);
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
                                .sourceAirportCode("AMS")
                                .destinationAirportCode("FRA")
                                .departureDateTime(LocalDateTime.of(2025, 5, 15, 8, 0))
                                .build();

                        System.out.println("\n✈️ Updated Flight Information:");
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

