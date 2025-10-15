package com.konsel.genelab.model;

public class CutSite {
    private final String enzyme;
    private final String site; // π.χ. GAATTC
    private final int position; // 0-based start στο forward reference
    private final String strand; // "+" ή "-"

    public CutSite(String enzyme, String site, int position, String strand) {
        this.enzyme = enzyme;
        this.site = site;
        this.position = position;
        this.strand = strand;
    }

    public String getEnzyme() {
        return enzyme;
    }

    public String getSite() {
        return site;
    }

    public int getPosition() {
        return position;
    }

    public String getStrand() {
        return strand;
    }
}
