package com.konsel.genelab.model;

public class Orf {
    private final int start; // 0-based inclusive on forward strand
    private final int end; // exclusive
    private final int frame; // +1..+3 or -1..-3

    public Orf(int start, int end, int frame) {
        this.start = start;
        this.end = end;
        this.frame = frame;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getFrame() {
        return frame;
    }

    public int getLenNt() {
        return end - start;
    }

    public int getLenAa() {
        return (end - start) / 3;
    }
}
