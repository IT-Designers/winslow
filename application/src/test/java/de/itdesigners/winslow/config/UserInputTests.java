package de.itdesigners.winslow.config;


import de.itdesigners.winslow.BaseRepository;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserInputTests {

    @Test
    public void testConfirmationMappingNever() throws IOException {

        var userInputYaml = """ 
                confirmation: "Never" 
                 """;


        var userInput = BaseRepository.readFromString(UserInput.class, userInputYaml);

        assertTrue(userInput.getEnvironment().isEmpty());
        assertEquals(UserInput.Confirmation.Never, userInput.getConfirmation());
    }


    @Test
    public void testConfirmationMappingNonDefault() throws IOException {


        var userInputYaml = """ 
                confirmation: "Always" 
                 """;

        var userInput = BaseRepository.readFromString(UserInput.class, userInputYaml);

        assertTrue(userInput.getEnvironment().isEmpty());
        assertEquals(UserInput.Confirmation.Always, userInput.getConfirmation());
    }
}
