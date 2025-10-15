package com.konsel.genelab.bio;

import com.konsel.genelab.model.CutSite;
import com.konsel.genelab.model.Fragment;
import java.util.*;
import java.util.regex.*;

public class RestrictionService {

    /**
     * Ένζυμο με site (IUPAC) + θέση κοπής ως offset από την αρχή του site
     * (0-based).
     */
    public static class Enz {
        public final String name;
        public final String site; // IUPAC motif, π.χ. GAATTC
        public final int cutOffset; // π.χ. EcoRI G^AATTC => 1

        public Enz(String name, String site, int cutOffset) {
            this.name = name;
            this.site = site.toUpperCase();
            this.cutOffset = Math.max(0, Math.min(cutOffset, this.site.length())); // clamp
        }
    }

    // Built-ins με offsets (κλασικές θέσεις κοπής)
    private static final Map<String, Enz> BUILT_INS = Map.ofEntries(
            Map.entry("EcoRI", new Enz("EcoRI", "GAATTC", 1)), // G^AATTC
            Map.entry("HindIII", new Enz("HindIII", "AAGCTT", 1)), // A^AGCTT
            Map.entry("BamHI", new Enz("BamHI", "GGATCC", 1)), // G^GATCC
            Map.entry("PstI", new Enz("PstI", "CTGCAG", 5)), // CTGCA^G
            Map.entry("NotI", new Enz("NotI", "GCGGCCGC", 2)), // GC^GGCCGC
            Map.entry("XhoI", new Enz("XhoI", "CTCGAG", 1)) // C^TCGAG
    );

    // Custom enzymes
    private static final Map<String, Enz> CUSTOM = new LinkedHashMap<>();

    /** Πρόσθεσε custom ένζυμο με offset (0..len(site)). */
    public static void addCustomEnzyme(String name, String site, int cutOffset) {
        if (name == null || name.isBlank() || site == null || site.isBlank())
            return;
        CUSTOM.put(name.trim(), new Enz(name.trim(), site.trim(), cutOffset));
    }

    public static void clearCustomEnzymes() {
        CUSTOM.clear();
    }

    /** Επιστρέφει όλα τα CutSite (forward & reverse hits). */
    public static List<CutSite> findSites(String seq) {
        List<CutSite> out = new ArrayList<>();
        if (seq == null || seq.isBlank())
            return out;

        String s = seq.toUpperCase();
        String rc = OrfService.reverseComplement(s);
        int n = s.length();

        LinkedHashMap<String, Enz> all = new LinkedHashMap<>(BUILT_INS);
        all.putAll(CUSTOM);

        for (var e : all.values()) {
            // forward
            Pattern pf = Pattern.compile(iupacToRegex(e.site));
            Matcher mf = pf.matcher(s);
            while (mf.find()) {
                out.add(new CutSite(e.name, e.site, mf.start(), "+"));
            }
            // reverse: ψάχνουμε RC(site) πάνω στο rc, map σε forward
            String rcSite = reverseComplementIupac(e.site);
            Pattern pr = Pattern.compile(iupacToRegex(rcSite));
            Matcher mr = pr.matcher(rc);
            while (mr.find()) {
                int forwardPos = n - (mr.start() + rcSite.length());
                out.add(new CutSite(e.name, e.site, forwardPos, "-"));
            }
        }

        out.sort(Comparator.<CutSite>comparingInt(CutSite::getPosition)
                .thenComparing(CutSite::getEnzyme));
        return out;
    }

    /** Linear digest με offsets (αν δοθεί λίστα ενζύμων, φιλτράρει). */
    public static List<Fragment> digestLinear(String seq, List<String> enzymeNames) {
        return digest(seq, enzymeNames, false);
    }

    /** Circular digest με offsets. */
    public static List<Fragment> digestCircular(String seq, List<String> enzymeNames) {
        return digest(seq, enzymeNames, true);
    }

    // --------------------- helpers ---------------------

    private static List<Fragment> digest(String seq, List<String> enzymeNames, boolean circular) {
        List<Fragment> out = new ArrayList<>();
        if (seq == null || seq.isBlank())
            return out;
        int n = seq.length();

        // χτίσε set από ένζυμα που θα χρησιμοποιηθούν
        Set<String> use = null;
        if (enzymeNames != null && !enzymeNames.isEmpty()) {
            use = new LinkedHashSet<>(enzymeNames);
        }

        // υπολογισμός θέσεων κοπής με offsets
        TreeSet<Integer> cuts = computeCutPositions(seq, use);

        if (cuts.isEmpty()) {
            // κανένα κόψιμο → ένα fragment
            out.add(new Fragment(0, n));
            return out;
        }

        if (!circular) {
            // Linear: [0..c1], [c1..c2], ..., [ck..n)
            int prev = 0;
            for (int c : cuts) {
                if (c > prev)
                    out.add(new Fragment(prev, c));
                prev = c;
            }
            if (prev < n)
                out.add(new Fragment(prev, n));
        } else {
            // Circular: περνάς από όλα τα cuts και τυλίγεις το τελευταίο στο 0
            List<Integer> list = new ArrayList<>(cuts);
            for (int i = 0; i < list.size(); i++) {
                int a = list.get(i);
                int b = list.get((i + 1) % list.size());
                if (b > a)
                    out.add(new Fragment(a, b));
                else { // wrap
                    // δύο κομμάτια: [a..n) και [0..b) αλλά σαν κυκλικό είναι ένα, δώστο ως length
                    // (b-a+n)
                    // Για να έχουμε συνεχόμενα forward coords, σπάμε σε δύο Fragment ώστε να
                    // φαίνεται στο UI
                    out.add(new Fragment(a, n));
                    out.add(new Fragment(0, b));
                }
            }
        }
        return out;
    }

    /**
     * Υπολογίζει τις θέσεις κοπής (0..n) εφαρμόζοντας τα offsets σε forward &
     * reverse hits.
     */
    private static TreeSet<Integer> computeCutPositions(String seq, Set<String> only) {
        TreeSet<Integer> cuts = new TreeSet<>();
        if (seq == null || seq.isBlank())
            return cuts;

        String s = seq.toUpperCase();
        String rc = OrfService.reverseComplement(s);
        int n = s.length();

        LinkedHashMap<String, Enz> all = new LinkedHashMap<>(BUILT_INS);
        all.putAll(CUSTOM);

        for (var e : all.values()) {
            if (only != null && !only.contains(e.name))
                continue;

            // forward hits -> cut = start + offset
            Pattern pf = Pattern.compile(iupacToRegex(e.site));
            Matcher mf = pf.matcher(s);
            while (mf.find()) {
                int cut = clamp(mf.start() + e.cutOffset, 0, n);
                cuts.add(cut);
            }

            // reverse hits: βρίσκουμε RC(site) πάνω στο rc → map σε forward
            String rcSite = reverseComplementIupac(e.site);
            Pattern pr = Pattern.compile(iupacToRegex(rcSite));
            Matcher mr = pr.matcher(rc);
            while (mr.find()) {
                int forwardStart = n - (mr.start() + rcSite.length());
                // Στον reverse προσανατολισμό, ο 5'->3' offset αντιστρέφεται:
                int cut = clamp(forwardStart + (e.site.length() - e.cutOffset), 0, n);
                cuts.add(cut);
            }
        }
        return cuts;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(v, hi));
    }

    // ---- IUPAC ----
    private static final Map<Character, String> IUPAC = Map.ofEntries(
            Map.entry('A', "A"), Map.entry('C', "C"), Map.entry('G', "G"), Map.entry('T', "T"),
            Map.entry('U', "U"), Map.entry('R', "[AG]"), Map.entry('Y', "[CT]"),
            Map.entry('S', "[GC]"), Map.entry('W', "[AT]"), Map.entry('K', "[GT]"),
            Map.entry('M', "[AC]"), Map.entry('B', "[CGT]"), Map.entry('D', "[AGT]"),
            Map.entry('H', "[ACT]"), Map.entry('V', "[ACG]"), Map.entry('N', "[ACGT]"));
    private static final Map<Character, Character> COMP = Map.ofEntries(
            Map.entry('A', 'T'), Map.entry('T', 'A'), Map.entry('U', 'A'),
            Map.entry('C', 'G'), Map.entry('G', 'C'),
            Map.entry('R', 'Y'), Map.entry('Y', 'R'),
            Map.entry('S', 'S'), Map.entry('W', 'W'),
            Map.entry('K', 'M'), Map.entry('M', 'K'),
            Map.entry('B', 'V'), Map.entry('V', 'B'),
            Map.entry('D', 'H'), Map.entry('H', 'D'),
            Map.entry('N', 'N'));

    private static String iupacToRegex(String site) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < site.length(); i++) {
            char c = Character.toUpperCase(site.charAt(i));
            sb.append(IUPAC.getOrDefault(c, String.valueOf(c)));
        }
        return sb.toString();
    }

    private static String reverseComplementIupac(String site) {
        StringBuilder sb = new StringBuilder(site.length());
        for (int i = site.length() - 1; i >= 0; i--) {
            char c = Character.toUpperCase(site.charAt(i));
            sb.append(COMP.getOrDefault(c, 'N'));
        }
        return sb.toString();
    }
}
