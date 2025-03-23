package io.airlinesample.ddbops.persistence;

import io.airlinesample.ddbops.domain.Flight;
import io.airlinesample.ddbops.domain.FlightPrimaryKey;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
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
                .routeByDay(row.get(Flight.ROUTE_BY_DAY_FIELD_NAME).s()) // FIXME safe conversion?
                .departureTime(row.get(Flight.DEPARTURE_TIME_FIELD_NAME).s())
                .flightNumber(row.get(Flight.FLIGHT_NUMBER_FIELD_NAME).s())
                .airplaneModel(row.get(Flight.AIRPLANE_MODEL_FIELD_NAME).s())
                .totalSeats(Integer.parseInt(row.get(Flight.TOTAL_SEATS_FIELD_NAME).n()))
                .availableSeats(Integer.parseInt(row.get(Flight.AVAILABLE_SEATS_FIELD_NAME).n()))
                .heldSeats(Integer.parseInt(row.get(Flight.HELD_SEATS_FIELD_NAME).n()))
                .version(Long.parseLong(row.get(Flight.VERSION_FIELD_NAME).n()))
                .claimedSeatMap(claimedSeatMap)
                .build();
    }
}
