package de.itd.tracking.winslow.config;

import com.moandjiezana.toml.Toml;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserInputTests {

    @Test
    public void testConfirmationMappingNever() {
        var userInput = new Toml().read("[userInput]\n" +
                "confirmation = \"Never\""
        )
                .getTable("userInput")
                .to(UserInput.class);

        assertTrue(userInput.getValueFor().isEmpty());
        assertEquals(UserInput.Confirmation.Never, userInput.requiresConfirmation());
    }

    @Test
    public void testConfirmationMappingDefault() {
        var userInput = new Toml().read("[userInput]")
                .getTable("userInput")
                .to(UserInput.class);

        assertTrue(userInput.getValueFor().isEmpty());
        assertEquals(UserInput.Confirmation.Never, userInput.requiresConfirmation());
    }

    @Test
    public void testConfirmationMappingNonDefault() {
        var userInput = new Toml().read("[userInput]\n" +
                "confirmation = \"Always\""
        )
                .getTable("userInput")
                .to(UserInput.class);

        assertTrue(userInput.getValueFor().isEmpty());
        assertEquals(UserInput.Confirmation.Always, userInput.requiresConfirmation());
    }
}
