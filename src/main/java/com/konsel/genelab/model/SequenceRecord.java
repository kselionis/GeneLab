package com.konsel.genelab.model;

public class SequenceRecord {
    private final String id;
    private final String description;
    private final String sequence; // uppercase A/C/G/T/N (or RNA with U)

    public SequenceRecord(String id, String description, String sequence){
        this.id = id;
        this.description = description == null ? "" : description;
        this.sequence = sequence == null ? "" : sequence.toUpperCase().trim();
    }
    public String id(){ return id; }
    public String description(){ return description; }
    public String seq(){ return sequence; }
    @Override public String toString(){ return id; }
}
