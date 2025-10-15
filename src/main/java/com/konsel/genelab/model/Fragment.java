package com.konsel.genelab.model;

public class Fragment {
    private final int start; // 0-based inclusive
    private final int end; // exclusive
    private final int length;

    public Fragment(int start, int end) {
        this.start = start;
        this.end = end;
        this.length = end - start;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getLength() {
        return length;
    }
}
