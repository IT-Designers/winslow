package de.itdesigners.winslow.web.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import de.itdesigners.winslow.api.pipeline.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

public class StageDefinitionInfoTests {


    @Test
    public void testWorkerDeserialization() throws JsonProcessingException {

        var json = """
                {
                  "@type": "Worker",
                  "id": "bb734bb4-a0f9-42fa-9d9a-455edcd330e0",
                  "name": "test",
                  "description": "Some description",
                  "image": {
                    "name": "hello-world",
                    "args": [],
                    "shmMegabytes": null
                  },
                  "requiredResources": {
                    "cpus": 2,
                    "megabytesOfRam": 1024,
                    "gpu": {
                      "count": 1,
                      "vendor": "nvidia",
                      "support": []
                    },
                    "tags": []
                  },
                  "userInput": {
                    "confirmation": "NEVER",
                    "requiredEnvVariables": []
                  },
                  "environment": {},
                  "highlight": {
                    "resources": []
                  },
                  "discardable": false,
                  "privileged": false,
                  "logParsers": [],
                  "ignoreFailuresWithinExecutionGroup": false,
                  "nextStages": []
                }
                """;

        var mapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .enable(SerializationFeature.INDENT_OUTPUT);

        var workerDefinitionInfo = mapper.readValue(json, StageDefinitionInfo.class);
        assertTrue(workerDefinitionInfo instanceof StageWorkerDefinitionInfo);

    }

    @Test
    public void testSerialisationWithDefaultValues() throws IOException {

        var stage = new StageWorkerDefinitionInfo(
                UUID.randomUUID(),
                "test",
                "description",
                new ImageInfo("hello-world", new String[0], 0),
                new RequirementsInfo(
                        1,
                        1024,
                        new GpuRequirementsInfo(
                                1,
                                "nvidia",
                                new String[0]
                        ),
                        Collections.emptyList()
                ),
                new UserInputInfo(
                        UserInputInfo.Confirmation.NEVER,
                        Collections.emptyList()
                ),
                Collections.emptyMap(),
                new HighlightInfo(Collections.emptyList()),
                false,
                false,
                Collections.emptyList(),
                false,
                Collections.emptyList()
        );

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        String jsonStr = mapper.writeValueAsString(stage);


        System.out.println(jsonStr);
    }


}
