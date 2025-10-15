package com.konsel.genelab.util;

public final class FxUtils {
    private FxUtils() {}
    public static String trimN(String seq) {
        return seq == null ? "" : seq.trim().toUpperCase();
    }
}
