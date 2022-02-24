package de.itdesigners.winslow.web.api;

import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class CsvLineAggregatorTest {

    @Test
    public void testBasicTimeAggregation() {
        var aggregator = new CsvLineAggregator(
                List.of(CsvLineAggregator.Operator.TimeMs, CsvLineAggregator.Operator.Last),
                new CsvLineAggregator.Config().setAggregationSpanMillis(1_000)
        );

        assertTrue(aggregator.aggregate("1123,bernd1").isEmpty());
        assertTrue(aggregator.aggregate("1234,bernd2").isEmpty());
        assertTrue(aggregator.aggregate("1456,bernd3").isEmpty());
        assertTrue(aggregator.aggregate("1999,bernd4").isEmpty());
        assertTrue(aggregator.aggregate("1999,bernd4").isEmpty());
        assertTrue(aggregator.aggregate("2122,bernd5").isEmpty());

        assertEquals(
                Optional.of("1123,bernd5"),
                aggregator.aggregate("2123,bernd6")
        );

        assertEquals(
                Optional.of("2123,bernd6"),
                aggregator.result()
        );
    }

    @Test
    public void testBasicMinMaxAvg() {
        var aggregator = new CsvLineAggregator(
                List.of(
                        CsvLineAggregator.Operator.TimeMs,
                        CsvLineAggregator.Operator.Min,
                        CsvLineAggregator.Operator.Max,
                        CsvLineAggregator.Operator.Avg
                ),
                new CsvLineAggregator.Config().setAggregationSpanMillis(1_000)
        );

        assertTrue(aggregator.aggregate("1000,0,0,0").isEmpty());
        assertTrue(aggregator.aggregate("1100,1,1,1").isEmpty());
        assertTrue(aggregator.aggregate("1200,2,2,2").isEmpty());
        assertTrue(aggregator.aggregate("1300,3,3,3").isEmpty());
        assertTrue(aggregator.aggregate("1400,4,4,4").isEmpty());
        assertTrue(aggregator.aggregate("1500,5,5,5").isEmpty());
        assertTrue(aggregator.aggregate("1600,6,6,6").isEmpty());
        assertTrue(aggregator.aggregate("1700,7,7,7").isEmpty());
        assertTrue(aggregator.aggregate("1800,8,8,8").isEmpty());
        assertTrue(aggregator.aggregate("1900,9,9,9").isEmpty());

        assertEquals(
                Optional.of("1000,0.00,9.00,4.50"),
                aggregator.aggregate("2000,0,0,0")
        );

        assertEquals(
                Optional.empty(),
                aggregator.aggregate("2100,1,1,1")
        );

        assertEquals(
                Optional.of("2000,0.00,1.00,0.50"),
                aggregator.result()
        );
    }

    @Test
    public void testBasicEmptyLastDistinct() {
        var aggregator = new CsvLineAggregator(
                List.of(
                        CsvLineAggregator.Operator.TimeMs,
                        CsvLineAggregator.Operator.Empty,
                        CsvLineAggregator.Operator.Last,
                        CsvLineAggregator.Operator.Distinct
                ),
                new CsvLineAggregator.Config().setAggregationSpanMillis(1_000)
        );

        assertEquals(
                Optional.empty(),
                aggregator.aggregate("1000,abc,aa,aa")
        );

        assertEquals(
                Optional.empty(),
                aggregator.aggregate("1100,abc,bb,aa")
        );
        assertEquals(
                Optional.empty(),
                aggregator.aggregate("1200,abc,cc,aa")
        );
        assertEquals(
                Optional.of("1000,,cc,aa"),
                aggregator.aggregate("1300,abc,dd,bb")
        );
        assertEquals(
                Optional.empty(),
                aggregator.aggregate("1400,abc,ee,bb")
        );
        assertEquals(
                Optional.empty(),
                aggregator.aggregate("1500,abc,ff,bb")
        );
        assertEquals(
                Optional.of("1300,,ff,bb"),
                aggregator.aggregate("1600,abc,gg,cc")
        );
        assertEquals(
                Optional.of("1600,,gg,cc"),
                aggregator.aggregate("1700,abc,hh,dd")
        );
        assertEquals(
                Optional.of("1700,,hh,dd"),
                aggregator.aggregate("1800,abc,ii,ee")
        );
        assertEquals(
                Optional.empty(),
                aggregator.aggregate("1900,abc,jj,ee")
        );
        assertEquals(
                Optional.empty(),
                aggregator.aggregate("2000,abc,kk,ee")
        );
        assertEquals(
                Optional.of("1800,,kk,ee"),
                aggregator.aggregate("2800,abc,kk,ee")
        );


        assertEquals(
                Optional.of("2800,,kk,ee"),
                aggregator.result()
        );
    }

}
