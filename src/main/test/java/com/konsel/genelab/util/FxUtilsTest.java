package com.konsel.genelab.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FxUtilsTest {
    @Test void trimN_basic() {
        assertEquals("ACGTN", FxUtils.trimN(" acgtn "));
    }
}
