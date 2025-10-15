package com.konsel.genelab.io;

import com.konsel.genelab.model.SequenceRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FastaParser {
    public static List<SequenceRecord> parse(File file) throws IOException {
        List<SequenceRecord> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line; String id = null; String desc = null; StringBuilder seq = new StringBuilder();
            while ((line = br.readLine()) != null) {
                if (line.startsWith(">")) {
                    if (id != null) list.add(new SequenceRecord(id, desc, seq.toString()));
                    String header = line.substring(1).trim();
                    int sp = header.indexOf(' ');
                    id = sp >= 0 ? header.substring(0, sp) : header;
                    desc = sp >= 0 ? header.substring(sp + 1) : "";
                    seq.setLength(0);
                } else {
                    seq.append(line.trim());
                }
            }
            if (id != null) list.add(new SequenceRecord(id, desc, seq.toString()));
        }
        return list;
    }
}
