package com.konsel.genelab.bio;

import com.konsel.genelab.model.AlignmentResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AlignmentServiceTest {
    @Test
    void smith_basic() {
        AlignmentResult r = AlignmentService.smithWaterman("ACGTT", "CGT", 2, -1, -2);
        assertTrue(r.getScore() > 0);
        assertTrue(r.getAlignedA().length() == r.getAlignedB().length());
    }
}
