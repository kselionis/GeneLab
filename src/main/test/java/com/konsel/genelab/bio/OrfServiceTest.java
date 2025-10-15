package com.konsel.genelab.bio;

import com.konsel.genelab.model.Orf;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class OrfServiceTest {
    @Test
    void reverseComplement_basic() {
        assertEquals("ACGT", OrfService.reverseComplement("ACGT")); // palindromic stays same
        assertEquals("ACGT", OrfService.reverseComplement("ACGT"));
        assertEquals("TACG", OrfService.reverseComplement("CGTA"));
    }

    @Test
    void findOrfs_forward() {
        // ATG --- TAA gives one ORF of length 6 (2 codons)
        List<Orf> orfs = OrfService.findOrfs("AAATGAAATAA", 6);
        assertTrue(orfs.stream().anyMatch(o -> o.getLenNt() == 6 && o.getFrame() == +1));
    }

    @Test
    void findOrfs_minLenFilter() {
        List<Orf> orfs = OrfService.findOrfs("ATGAAATAA", 9); // 9 nt required, but len=9 passes
        assertTrue(orfs.stream().anyMatch(o -> o.getLenNt() == 9));
    }
}
