package com.konsel.genelab.bio;

import java.util.*;

public class StatsService {
    public static double gcContent(String seq) {
        if (seq == null || seq.isEmpty())
            return 0.0;
        long gc = 0, atgc = 0;
        for (int i = 0; i < seq.length(); i++) {
            char c = Character.toUpperCase(seq.charAt(i));
            switch (c) {
                case 'G', 'C' -> {
                    gc++;
                    atgc++;
                }
                case 'A', 'T', 'U' -> atgc++;
                default -> {
                }
            }
        }
        return atgc == 0 ? 0.0 : (gc * 100.0) / atgc;
    }

    public static Map<String, Integer> kmerCounts(String seq, int k) {
        Map<String, Integer> map = new HashMap<>();
        if (seq == null || seq.length() < k)
            return map;
        String s = seq.toUpperCase();
        for (int i = 0; i <= s.length() - k; i++) {
            String kmer = s.substring(i, i + k);
            if (kmer.chars().allMatch(ch -> ch == 'A' || ch == 'C' || ch == 'G' || ch == 'T' || ch == 'U')) {
                map.merge(kmer, 1, Integer::sum);
            }
        }
        return map;
    }
}
