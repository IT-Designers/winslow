package de.itdesigners.winslow.api.pipeline;

public record ResourceInfo(
        int cpus,
        long megabytesOfRam,
        int gpus) {

}
