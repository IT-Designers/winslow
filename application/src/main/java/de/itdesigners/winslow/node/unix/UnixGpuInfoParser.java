package de.itdesigners.winslow.node.unix;

import de.itdesigners.winslow.Env;
import de.itdesigners.winslow.api.node.GpuInfo;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnixGpuInfoParser {

    public static final Logger LOG              = Logger.getLogger(UnixGpuInfoParser.class.getSimpleName());
    public static final String NVIDIA_SEPARATOR = ",";

    public static List<GpuInfo> loadGpuInfo() throws IOException {
        // Stream.concat(nvidia, amd, ..).collect(..)
        return getNvidiaGpuInfoNoThrows().collect(Collectors.toList());
    }

    public static Stream<GpuInfo> getNvidiaGpuInfoNoThrows() {
        try {
            return getNvidiaGpuInfo();
        } catch (InterruptedException | IOException e) {
            LOG.log(Env.isDevEnv() ? Level.WARNING : Level.FINE, "Failed to collect GPU info of nvidia devices", e);
            return Stream.empty();
        }
    }

    public static Stream<GpuInfo> getNvidiaGpuInfo() throws IOException, InterruptedException {
        var processBuilder = new ProcessBuilder(
                "nvidia-smi",
                "--format=csv,noheader,nounits",
                "--query-gpu=index,name,utilization.gpu,utilization.memory"
                // some additional parmeters
                // "temperature.gpu,fan.speed,power.draw,power.limit,clocks.gr,clocks.max.gr,clocks.mem,clocks.max.mem,memory.used,memory.total"
        );
        var process  = processBuilder.start();
        var exitCode = process.waitFor();

        if (exitCode == 0) {
            var info = new ArrayList<GpuInfo>();
            try (var reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(),
                    StandardCharsets.UTF_8
            ))) {
                var line  = (String) null;
                while ((line = reader.readLine()) != null) {
                    var split = line.split(NVIDIA_SEPARATOR);
                    info.add(new GpuInfo(
                            "nvidia-" + split[0].trim(), // index
                            "nvidia",
                            split[1].trim(), // name
                            Float.parseFloat(split[2].trim()),
                            Float.parseFloat(split[3].trim())
                    ));
                }
            } catch (IndexOutOfBoundsException e) {
                throw new IOException("Invalid line returned by call", e);
            }
            return info.stream();
        } else {
            var buffer = new ByteArrayOutputStream();
            process.getErrorStream().transferTo(buffer);
            throw new IOException("nvidia-smi failed: " + new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        }
    }
}
