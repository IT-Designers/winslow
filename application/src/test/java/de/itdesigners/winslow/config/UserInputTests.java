package de.itdesigners.winslow.config;

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

        assertTrue(userInput.getEnvironment().isEmpty());
        assertEquals(UserInput.Confirmation.Never, userInput.getConfirmation());
    }

    @Test
    public void testConfirmationMappingDefault() {
        var userInput = new Toml().read("[userInput]")
                .getTable("userInput")
                .to(UserInput.class);

        assertTrue(userInput.getEnvironment().isEmpty());
        assertEquals(UserInput.Confirmation.Never, userInput.getConfirmation());
    }

    @Test
    public void testConfirmationMappingNonDefault() {
        var userInput = new Toml().read("[userInput]\n" +
                "confirmation = \"Always\""
        )
                .getTable("userInput")
                .to(UserInput.class);

        assertTrue(userInput.getEnvironment().isEmpty());
        assertEquals(UserInput.Confirmation.Always, userInput.getConfirmation());
    }
}
