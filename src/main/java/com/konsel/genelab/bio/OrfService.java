package com.konsel.genelab.bio;

import com.konsel.genelab.model.Orf;
import java.util.*;

public class OrfService {
    private static final Set<String> STOPS = Set.of("TAA", "TAG", "TGA");

    public static String reverseComplement(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = Character.toUpperCase(s.charAt(i));
            sb.append(switch (c) {
                case 'A' -> 'T';
                case 'T' -> 'A';
                case 'C' -> 'G';
                case 'G' -> 'C';
                case 'U' -> 'A';
                default -> 'N';
            });
        }
        return sb.toString();
    }

    public static List<Orf> findOrfs(String seq, int minLenNt) {
        String s = seq.toUpperCase();
        String rc = reverseComplement(s);
        List<Orf> out = new ArrayList<>();
        for (int frame = 0; frame < 3; frame++)
            scanFrame(s, frame, +1, minLenNt, out);
        for (int frame = 0; frame < 3; frame++)
            scanFrame(rc, frame, -1, minLenNt, out);
        return out;
    }

    private static void scanFrame(String s, int frame, int sign, int minLenNt, List<Orf> out) {
        int i = frame;
        while (i + 3 <= s.length()) {
            if (s.startsWith("ATG", i)) {
                int j = i + 3;
                while (j + 3 <= s.length()) {
                    String codon = s.substring(j, j + 3);
                    if (STOPS.contains(codon)) {
                        int len = j + 3 - i;
                        if (len >= minLenNt) {
                            int dispFrame = sign * (frame + 1); // +1..+3 or -1..-3
                            out.add(new Orf(i, j + 3, dispFrame));
                        }
                        i = j + 3; // move after stop
                        break;
                    }
                    j += 3;
                }
            }
            i += 3;
        }
    }
}
