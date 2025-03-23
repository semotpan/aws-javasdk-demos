package io.airlinesample.ddbops.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import java.util.List;

@DynamoDbBean
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Preferences {

    public static final String SEAT_PREFERENCE_FIELD_NAME = "SeatPreference";
    public static final String MEAL_PREFERENCE_FIELD_NAME = "MealPreference";
    public static final String TIMEZONE_FIELD_NAME = "Timezone";
    public static final String LANGUAGE_FIELD_NAME = "Language";
    public static final String ACCESSIBILITY_REQUIREMENTS_FIELD_NAME = "AccessibilityRequirements";

    @Getter(onMethod = @__({@DynamoDbAttribute(SEAT_PREFERENCE_FIELD_NAME)}))
    @JsonProperty(SEAT_PREFERENCE_FIELD_NAME)
    private String seatPreference;

    @Getter(onMethod = @__({@DynamoDbAttribute(MEAL_PREFERENCE_FIELD_NAME)}))
    @JsonProperty(MEAL_PREFERENCE_FIELD_NAME)
    private List<String> mealPreference;

    @Getter(onMethod = @__({@DynamoDbAttribute(TIMEZONE_FIELD_NAME)}))
    @JsonProperty(TIMEZONE_FIELD_NAME)
    private String timezone;

    @Getter(onMethod = @__({@DynamoDbAttribute(LANGUAGE_FIELD_NAME)}))
    @JsonProperty(LANGUAGE_FIELD_NAME)
    private String language;

    @Getter(onMethod = @__({@DynamoDbAttribute(ACCESSIBILITY_REQUIREMENTS_FIELD_NAME)}))
    @JsonProperty(ACCESSIBILITY_REQUIREMENTS_FIELD_NAME)
    private List<String> accessibilityRequirements;

    @Builder
    public Preferences(String seatPreference, List<String> mealPreference, String timezone, String language, List<String> accessibilityRequirements) {
        this.seatPreference = seatPreference;
        this.mealPreference = mealPreference;
        this.timezone = timezone;
        this.language = language;
        this.accessibilityRequirements = accessibilityRequirements;
    }
}
