package de.itd.tracking.winslow.config;

import java.util.Collections;
import java.util.List;

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

    public enum Confirmation {
        Never,
        Once,
        Always
    }
}
