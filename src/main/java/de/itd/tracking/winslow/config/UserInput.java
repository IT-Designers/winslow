package de.itd.tracking.winslow.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UserInput {
    private final Confirmation confirmation;
    private final List<String> valueFor;

    public UserInput(Confirmation confirmation, List<String> valueFor) {
        this.confirmation = confirmation;
        this.valueFor = valueFor;
    }

    public Confirmation requiresConfirmation() {
        return confirmation != null ? confirmation : Confirmation.Never;
    }

    public List<String> getValueFor() {
        return valueFor != null ? Collections.unmodifiableList(valueFor) : Collections.emptyList();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{valueFor=" + this.valueFor + "}#" + this.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserInput userInput = (UserInput) o;
        return confirmation == userInput.confirmation && Objects.equals(valueFor, userInput.valueFor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(confirmation, valueFor);
    }

    public enum Confirmation {
        Never, Once, Always
    }
}
