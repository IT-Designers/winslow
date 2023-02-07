package de.itdesigners.winslow;

import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class PipelineRepositoryTest {

    @Test
    public void testGetSibling() {
        var pipelinePathBase = Path.of("/bernd/das/brot");
        var pipelinePath = pipelinePathBase.resolve("my-project-id.pipeline.yml");

        assertEquals(
                "my-project-id.pipeline.history.yml",
                pipelinePathBase.relativize(PipelineRepository.getPipelineSiblingFile(
                        pipelinePath,
                        PipelineRepository.SIBLING_EG_HISTORY
                )).toString()
        );
        assertEquals(
                "my-project-id.pipeline.enqueued.yml",
                pipelinePathBase.relativize(PipelineRepository.getPipelineSiblingFile(
                        pipelinePath,
                        PipelineRepository.SIBLING_EG_ENQUEUED
                )).toString()
        );
        assertEquals(
                "my-project-id.pipeline.active.yml",
                pipelinePathBase.relativize(PipelineRepository.getPipelineSiblingFile(
                        pipelinePath,
                        PipelineRepository.SIBLING_EG_ACTIVE
                )).toString()
        );
    }

}
