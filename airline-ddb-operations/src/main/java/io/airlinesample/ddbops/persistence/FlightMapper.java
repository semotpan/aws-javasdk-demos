package io.airlinesample.ddbops.persistence;

import io.airlinesample.ddbops.domain.Flight;
import io.airlinesample.ddbops.domain.FlightPrimaryKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

final class FlightMapper {

    static Map<String, AttributeValue> toDDBKeyMap(FlightPrimaryKey primaryKey) {
        return Map.of(
                Flight.ROUTE_BY_DAY_FIELD_NAME, AttributeValue.fromS(primaryKey.getPartitionKey()),
                Flight.DEPARTURE_TIME_FIELD_NAME, AttributeValue.fromS(primaryKey.getSortKey())
        );
    }

    // FIXME: Convert to instance??
    static Flight toModel(Map<String, AttributeValue> row) {
        Map<String, String> claimedSeatMap = null;
        if (row.containsKey(Flight.CLAIMED_SEAT_MAP_FIELD_NAME)) {
            claimedSeatMap = row.get(Flight.CLAIMED_SEAT_MAP_FIELD_NAME).m()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().s()));
        }

        return Flight.mapBuilder()
                .routeByDay(safeStringRead(row, Flight.ROUTE_BY_DAY_FIELD_NAME)) // FIXME safe conversion?
                .departureTime(safeStringRead(row, Flight.DEPARTURE_TIME_FIELD_NAME))
                .flightNumber(safeStringRead(row, Flight.FLIGHT_NUMBER_FIELD_NAME))
                .airplaneModel(safeStringRead(row, Flight.AIRPLANE_MODEL_FIELD_NAME))
                .totalSeats(safeIntegerRead(row, Flight.TOTAL_SEATS_FIELD_NAME))
                .availableSeats(safeIntegerRead(row, Flight.AVAILABLE_SEATS_FIELD_NAME))
                .heldSeats(safeIntegerRead(row, Flight.HELD_SEATS_FIELD_NAME))
                .version(safeLongRead(row, Flight.VERSION_FIELD_NAME))
                .claimedSeatMap(claimedSeatMap)
                .build();
    }

    private static String safeStringRead(Map<String, AttributeValue> row, String fieldName) {
        return Optional.ofNullable(row.get(fieldName))
                .map(AttributeValue::s)
                .orElse(null);
    }

    private static Long safeLongRead(Map<String, AttributeValue> row, String fieldName) {
        return Optional.ofNullable(row.get(fieldName))
                .map(AttributeValue::n)
                .map(Long::parseLong)
                .orElse(null);
    }

    private static Integer safeIntegerRead(Map<String, AttributeValue> row, String fieldName) {
        return Optional.ofNullable(row.get(fieldName))
                .map(AttributeValue::n)
                .map(Integer::parseInt)
                .orElse(null);
    }
}
