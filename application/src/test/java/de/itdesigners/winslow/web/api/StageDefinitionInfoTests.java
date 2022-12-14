package de.itdesigners.winslow.web.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.itdesigners.winslow.BaseRepository;
import de.itdesigners.winslow.api.pipeline.StageDefinitionInfo;
import de.itdesigners.winslow.api.pipeline.StageWorkerDefinitionInfo;
import de.itdesigners.winslow.config.Requirements;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class StageDefinitionInfoTests {


    @Test
    public void testWorkerDeserialisation() throws JsonProcessingException {

        var json = """
                {
                  "@type": "Worker",
                  "id" : null,
                  "name" : "test",
                  "description" : null,
                  "image" : null,
                  "requiredResources" : null,
                  "userInput" : null,
                  "environment" : null,
                  "highlight" : null,
                  "discardable" : null,
                  "privileged" : null,
                  "logParsers" : null,
                  "ignoreFailuresWithinExecutionGroup" : null,
                  "nextStages" : null
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        var workerDefinitionInfo = mapper.readValue(json, StageDefinitionInfo.class);
        assertTrue(workerDefinitionInfo instanceof StageWorkerDefinitionInfo);

    }

    @Test
    public void testSerialisationWithDefaultValues() throws IOException {

        var stage = new StageWorkerDefinitionInfo(
                null,
                "test",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        String jsonStr = mapper.writeValueAsString(stage);


        System.out.println(jsonStr);
    }


}
