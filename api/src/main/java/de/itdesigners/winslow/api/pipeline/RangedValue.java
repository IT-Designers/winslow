package de.itdesigners.winslow.api.pipeline;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RangeWithStepSize.class, name = "DiscreteSteps"),
        @JsonSubTypes.Type(value = RangedList.class, name = "List")
})
public interface RangedValue {

    String getValue(int step);

    int getStepCount();
}
