package com.konsel.genelab.bio;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class StatsServiceTest {
    @Test
    void gc_basic() {
        assertEquals(50.0, StatsService.gcContent("ACGT"), 1e-6);
    }

    @Test
    void kmers_basic() {
        Map<String, Integer> m = StatsService.kmerCounts("ACGTAC", 2);
        assertEquals(5, m.values().stream().mapToInt(i -> i).sum());
    }
}
