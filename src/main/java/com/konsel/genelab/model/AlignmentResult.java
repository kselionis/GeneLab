package com.konsel.genelab.model;

public class AlignmentResult {
    private final int score;
    private final String alignedA, alignedB, markers; // '|' σε matches

    public AlignmentResult(int score, String alignedA, String alignedB, String markers) {
        this.score = score;
        this.alignedA = alignedA;
        this.alignedB = alignedB;
        this.markers = markers;
    }

    public int getScore() {
        return score;
    }

    public String getAlignedA() {
        return alignedA;
    }

    public String getAlignedB() {
        return alignedB;
    }

    public String getMarkers() {
        return markers;
    }
}
