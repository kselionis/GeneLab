package com.konsel.genelab.io;

import com.konsel.genelab.model.SequenceRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FastqParser {
    public static List<SequenceRecord> parse(File file) throws IOException {
        List<SequenceRecord> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String header;
            while ((header = br.readLine()) != null) {
                if (!header.startsWith("@")) throw new IOException("Invalid FASTQ header: " + header);
                    String seq = br.readLine();
                    String plus = br.readLine();
                    String qual = br.readLine();
                    if (seq == null || plus == null || qual == null) throw new IOException("Truncated FASTQ record");
                        String id = header.length()>1 ? header.substring(1).split("\\s",2)[0] : "record" + list.size();
                        list.add(new SequenceRecord(id, "", seq));
            }
        }
        return list;
    }
}
