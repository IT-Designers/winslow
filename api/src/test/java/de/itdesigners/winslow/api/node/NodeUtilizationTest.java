package de.itdesigners.winslow.api.node;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class NodeUtilizationTest {

    private static final NodeInfo INFO_SAMPLE_1 = new NodeInfo(
            "INFO_1",
            1337_4711L,
            42_1337L,
            new CpuInfo(
                    "SuperMegaArch-ProEleven",
                    List.of(0.23f, 0.42f, 0.13f, 0.3337f)
            ),
            new MemInfo(
                    4711,
                    1337,
                    42,
                    1337_4711,
                    123
            ),
            new NetInfo(0, 33),
            new DiskInfo(
                    100,
                    200,
                    300,
                    400
            ),
            List.of(
                    new GpuInfo(
                            "pci-0",
                            "pixel",
                            "PixelSchubser GTR 42",
                            0.991f,
                            0.2f,
                            1024,
                            2048
                    ),
                    new GpuInfo(
                            "pci-1",
                            "fire",
                            "FireInTheHouse 1337",
                            0.21f,
                            0.92f,
                            1024,
                            2048
                    )
            ),
            Collections.emptyList()
    );

    @Test
    public void testCsvAndBack() {
        assertEquals(
                NodeUtilization.from(INFO_SAMPLE_1).toCsvLine(),
                NodeUtilization.from(INFO_SAMPLE_1).toCsvLine()
        );
        assertEquals(
                NodeUtilization.from(INFO_SAMPLE_1).toCsvLine(),
                NodeUtilization.fromCsvLine(NodeUtilization.from(INFO_SAMPLE_1).toCsvLine()).toCsvLine()
        );
        assertEquals(
                NodeUtilization.from(INFO_SAMPLE_1),
                NodeUtilization.fromCsvLine(NodeUtilization.from(INFO_SAMPLE_1).toCsvLine())
        );
    }
}
