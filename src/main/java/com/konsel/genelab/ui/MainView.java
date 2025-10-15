package com.konsel.genelab.ui;

import com.konsel.genelab.bio.StatsService;
import com.konsel.genelab.bio.OrfService;
import com.konsel.genelab.io.FastaParser;
import com.konsel.genelab.io.FastqParser;
import com.konsel.genelab.model.SequenceRecord;
import com.konsel.genelab.model.Orf;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.text.TextFlow;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javax.imageio.ImageIO;
import java.util.regex.*;

import com.konsel.genelab.model.CutSite;
import com.konsel.genelab.bio.RestrictionService;
import com.konsel.genelab.model.Fragment;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MainView {
    @FXML private ListView<SequenceRecord> sequenceList;
    @FXML private Label idLabel;
    @FXML private Label lenLabel;
    @FXML private Label gcLabel;
    @FXML private TextArea kmerArea;

    // ORF UI
    @FXML private Label orfSeqLabel;
    @FXML private TextField minLenField;
    @FXML private TableView<Orf> orfTable;
    @FXML private TableColumn<Orf, Integer> startCol;
    @FXML private TableColumn<Orf, Integer> endCol;
    @FXML private TableColumn<Orf, Integer> frameCol;
    @FXML private TableColumn<Orf, Integer> lenCol;
    @FXML private TableColumn<Orf, Integer> lenAaCol;

    // Alignment UI
    @FXML private TextField matchField, mismatchField, gapField;
    @FXML private TextArea seqAText, seqBText, alignOut;
    @FXML private CheckBox localMode;
    @FXML private ChoiceBox<String> metricChoice;
    @FXML private Spinner<Integer> windowSpinner;
    @FXML private LineChart<Number, Number> skewChart;

    // Viewer
    @FXML private TextFlow seqViewer;
    @FXML private TextField motifField;
    @FXML private CheckBox regexMode;
    @FXML private Label motifSummary;

    // Restriction Map
    @FXML private Label reSeqLabel;
    @FXML private TableView<CutSite> reTable;
    @FXML private TableColumn<CutSite, String> reEnzCol, reSiteCol, reStrandCol;
    @FXML private TableColumn<CutSite, Integer> rePosCol;
    @FXML private TextField reNameField;
    @FXML private TextField reSiteField;
    @FXML private CheckBox reCircularCheck;
    @FXML private TextField reOffsetField;

    // Fragments (digest)
    @FXML private TableView<Fragment> fragTable;
    @FXML private TableColumn<Fragment, Integer> fragStartCol, fragEndCol, fragLenCol;


    private final ObservableList<SequenceRecord> sequences = FXCollections.observableArrayList();
    private final ObservableList<Orf> orfs = FXCollections.observableArrayList();
    private final ObservableList<CutSite> cutSites = FXCollections.observableArrayList();
    private final ObservableList<Fragment> fragments = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Sidebar
        sequenceList.setItems(sequences);
        sequenceList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        sequenceList.setCellFactory(v -> new ListCell<>(){
            @Override protected void updateItem(SequenceRecord item, boolean empty){
                super.updateItem(item, empty);
                setText(empty || item==null ? null : item.id());
            }
        });
        sequenceList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            updateStats(sel);
            updateOrfHeader(sel);
            if (reSeqLabel != null)
                reSeqLabel.setText(sel == null ? "—" : sel.id());
            cutSites.clear();

        });

        // ORF table
        orfTable.setItems(orfs);
        startCol.setCellValueFactory(new PropertyValueFactory<>("start"));
        endCol.setCellValueFactory(new PropertyValueFactory<>("end"));
        frameCol.setCellValueFactory(new PropertyValueFactory<>("frame"));
        lenCol.setCellValueFactory(new PropertyValueFactory<>("lenNt"));
        lenAaCol.setCellValueFactory(new PropertyValueFactory<>("lenAa"));

        orfTable.getSelectionModel().selectedItemProperty().addListener((obs, oldOrf, selOrf) -> {
            SequenceRecord rec = sequenceList.getSelectionModel().getSelectedItem();
            if (rec != null && selOrf != null) {
                renderSequenceWithHighlight(rec.seq(), selOrf.getStart(), selOrf.getEnd());
            }
        });

        // Plots defaults
        if (metricChoice != null && metricChoice.getItems().isEmpty()) {
            metricChoice.setItems(FXCollections.observableArrayList("GC skew", "AT skew"));
            metricChoice.getSelectionModel().select("GC skew");
        }
        if (windowSpinner != null) {
            windowSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 10000, 100, 10));
        }

        if (reTable != null) {
            reTable.setItems(cutSites);
            reTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            reEnzCol.setCellValueFactory(new PropertyValueFactory<>("enzyme"));
            reSiteCol.setCellValueFactory(new PropertyValueFactory<>("site"));
            rePosCol.setCellValueFactory(new PropertyValueFactory<>("position"));
            reStrandCol.setCellValueFactory(new PropertyValueFactory<>("strand"));
        }

        if (fragTable != null) {
            fragTable.setItems(fragments);
            fragStartCol.setCellValueFactory(new PropertyValueFactory<>("start"));
            fragEndCol.setCellValueFactory(new PropertyValueFactory<>("end"));
            fragLenCol.setCellValueFactory(new PropertyValueFactory<>("length"));
        }
    }

    @FXML
    private void onOpenFiles() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open FASTA/FASTQ files");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("FASTA/FASTQ", "*.fa", "*.fasta", "*.fna", "*.ffn", "*.faa", "*.frn", "*.fastq", "*.fq"),
            new FileChooser.ExtensionFilter("All files", "*.*")
        );
        List<File> files = fc.showOpenMultipleDialog(sequenceList.getScene().getWindow());
        if (files == null) return;

        List<SequenceRecord> loaded = new ArrayList<>();
        for (File f : files) {
            try {
                String firstLine = Files.readAllLines(f.toPath()).get(0).trim();
                if (firstLine.startsWith(">")) {
                    loaded.addAll(FastaParser.parse(f));
                } else if (firstLine.startsWith("@")) {
                    loaded.addAll(FastqParser.parse(f));
                } else {
                    showError("Unrecognized file format: " + f.getName());
                }
            } catch (Exception ex) {
                showError("Failed to read " + f.getName() + "" + ex.getMessage());
            }
        }
        sequences.addAll(loaded);
        if (!sequences.isEmpty()) sequenceList.getSelectionModel().selectFirst();
    }

    private void updateStats(SequenceRecord rec){
        if (rec == null){
            idLabel.setText("-"); lenLabel.setText("-"); gcLabel.setText("-"); kmerArea.clear();
            return;
        }
        idLabel.setText(rec.id());
        lenLabel.setText(String.valueOf(rec.seq().length()));
        double gc = StatsService.gcContent(rec.seq());
        gcLabel.setText(String.format("%.2f%%", gc));
        var kmers = StatsService.kmerCounts(rec.seq(), 3);  // deafult k=3
        StringBuilder sb = new StringBuilder();
        kmers.entrySet().stream().sorted((a,b)->b.getValue()-a.getValue())
            .limit(32).forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue()).append(""));
        kmerArea.setText(sb.toString());    
    }

    private void updateOrfHeader(SequenceRecord rec){
        orfSeqLabel.setText(rec == null ? "-" : rec.id());
        orfs.clear();
        clearViewer();
    }

    @FXML
    private void onFindOrfs(){
        SequenceRecord rec = sequenceList.getSelectionModel().getSelectedItem();
        if (rec == null){ showError("Select a sequence first."); return; }
        int minLen = 90;
        try {
            if (minLenField.getText()!=null && !minLenField.getText().isBlank())
              minLen = Integer.parseInt(minLenField.getText().trim());
        } catch (NumberFormatException e){
            showError("Min length must be an integer.");
            return;
        }
        orfs.setAll(OrfService.findOrfs(rec.seq(), minLen));
    }

    @FXML
    private void onExportOrfs() {
        if (orfs.isEmpty()) {
            showError("No ORFs to export.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save ORFs CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showSaveDialog(seqViewer.getScene().getWindow());
        if (f == null)
            return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(f, java.nio.charset.StandardCharsets.UTF_8)) {
            pw.println("seq_id,start,end,frame,len_nt,len_aa");
            String seqId = sequenceList.getSelectionModel().getSelectedItem() != null
                    ? sequenceList.getSelectionModel().getSelectedItem().id()
                    : "";
            for (var o : orfs) {
                pw.printf("%s,%d,%d,%d,%d,%d%n",
                        seqId, o.getStart(), o.getEnd(), o.getFrame(), o.getLenNt(), o.getLenAa());
            }
        } catch (Exception ex) {
            showError("Failed to save CSV:\n" + ex.getMessage());
        }
    }

    @FXML
    private void onAlignSelected() {
        var sel = sequenceList.getSelectionModel().getSelectedItems();
        if (sel == null || sel.size() < 2) {
            showError("Select two sequences in the list (Ctrl/Cmd-click).");
            return;
        }
        String a = sel.get(0).seq();
        String b = sel.get(1).seq();
        doAlignAndShow(a, b);
    }

    @FXML
    private void onAlignCustom() {
        doAlignAndShow(seqAText.getText(), seqBText.getText());
    }

    private void doAlignAndShow(String a, String b) {
        int match = parseOrDefault(matchField, 1);
        int mismatch = parseOrDefault(mismatchField, -1);
        int gap = parseOrDefault(gapField, -2);

        var res = localMode != null && localMode.isSelected()
                ? com.konsel.genelab.bio.AlignmentService.smithWaterman(a, b, match, mismatch, gap)
                : com.konsel.genelab.bio.AlignmentService.needlemanWunsch(a, b, match, mismatch, gap);

        String out = (localMode.isSelected() ? "Local score: " : "Global score: ")
                + res.getScore() + "\n"
                + res.getAlignedA() + "\n"
                + res.getMarkers() + "\n"
                + res.getAlignedB() + "\n";
        alignOut.setText(out);
    }

    private int parseOrDefault(TextField tf, int def) {
        try {
            return tf.getText() == null || tf.getText().isBlank() ? def : Integer.parseInt(tf.getText().trim());
        } catch (Exception e) {
            return def;
        }
    }

    private void renderSequenceWithHighlights(String seq, List<int[]> ranges) {
        seqViewer.getChildren().clear();
        if (seq == null || seq.isEmpty())
            return;
        // merge & clamp ranges
        List<int[]> rs = new ArrayList<>();
        for (int[] r : ranges) {
            int s = Math.max(0, Math.min(r[0], seq.length()));
            int e = Math.max(0, Math.min(r[1], seq.length()));
            if (e > s)
                rs.add(new int[] { s, e });
        }
        rs.sort(java.util.Comparator.comparingInt(a -> a[0]));
        // build flow
        int cur = 0;
        for (int[] r : rs) {
            int s = r[0], e = r[1];
            if (s > cur)
                seqViewer.getChildren().add(new javafx.scene.text.Text(seq.substring(cur, s)));
            javafx.scene.text.Text t = new javafx.scene.text.Text(seq.substring(s, e));
            t.setStyle("-fx-fill: white; -fx-font-weight: bold; -fx-background-color: #ff6b6b; -fx-padding: 1px;");
            seqViewer.getChildren().add(t);
            cur = e;
        }
        if (cur < seq.length())
            seqViewer.getChildren().add(new javafx.scene.text.Text(seq.substring(cur)));
    }

    private void renderSequenceWithHighlight(String seq, int start, int end) {
        java.util.List<int[]> one = new java.util.ArrayList<>();
        one.add(new int[] { start, end });
        renderSequenceWithHighlights(seq, one);
    }

    private void clearViewer() {
        seqViewer.getChildren().clear();
    }

    @FXML
    private void onPlotSkew() {
        SequenceRecord rec = sequenceList.getSelectionModel().getSelectedItem();
        if (rec == null) {
            showError("Select a sequence first.");
            return;
        }

        String seq = rec.seq();
        int win = 100;
        try {
            win = windowSpinner.getValue();
        } catch (Exception ignore) {
        }
        if (win <= 0 || win > seq.length()) {
            showError("Window must be > 0 and <= sequence length.");
            return;
        }

        String metric = metricChoice.getSelectionModel().getSelectedItem();
        double[] ys = "AT skew".equalsIgnoreCase(metric)
                ? computeSkew(seq, win, 'A', 'T')
                : computeSkew(seq, win, 'G', 'C');

        XYChart.Series<Number, Number> s = new XYChart.Series<>();
        s.setName(rec.id() + " — " + metric + " (win=" + win + ")");
        for (int i = 0; i < ys.length; i++) {
            double x = i + win / 2.0;
            s.getData().add(new XYChart.Data<>(x, ys[i]));
        }
        skewChart.getData().clear();
        skewChart.getData().add(s);
    }

    private double[] computeSkew(String seq, int w, char x, char y) {
        seq = seq.toUpperCase();
        int n = seq.length();
        int outLen = Math.max(n - w + 1, 0);
        double[] out = new double[outLen];
        if (outLen == 0)
            return out;

        int cx = 0, cy = 0;
        for (int i = 0; i < w; i++) {
            char c = seq.charAt(i);
            if (c == x)
                cx++;
            else if (c == y)
                cy++;
        }
        out[0] = skewValue(cx, cy);

        for (int i = 1; i < outLen; i++) {
            char left = seq.charAt(i - 1);
            char right = seq.charAt(i + w - 1);
            if (left == x)
                cx--;
            else if (left == y)
                cy--;
            if (right == x)
                cx++;
            else if (right == y)
                cy++;
            out[i] = skewValue(cx, cy);
        }
        return out;
    }

    private double skewValue(int a, int b) {
        int denom = a + b;
        return denom == 0 ? 0.0 : (a - b) / (double) denom;
    }

    @FXML
    private void onExportPlot() {
        if (skewChart == null || skewChart.getData().isEmpty()) {
            showError("Nothing to export. Create a plot first.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Plot as PNG");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File f = fc.showSaveDialog(skewChart.getScene().getWindow());
        if (f == null)
            return;

        try {
            var img = skewChart.snapshot(new SnapshotParameters(), null);
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", f);
        } catch (Exception e) {
            showError("Failed to export PNG:\n" + e.getMessage());
        }
    }

    @FXML
    private void onFindMotif() {
        SequenceRecord rec = sequenceList.getSelectionModel().getSelectedItem();
        if (rec == null) {
            showError("Select a sequence first.");
            return;
        }
        String motif = motifField != null ? motifField.getText() : null;
        if (motif == null || motif.isBlank()) {
            showError("Enter a motif (plain text or regex).");
            return;
        }

        String seq = rec.seq().toUpperCase();
        List<int[]> hits = new ArrayList<>();
        try {
            if (regexMode != null && regexMode.isSelected()) {
                Pattern p = Pattern.compile(motif.toUpperCase());
                Matcher m = p.matcher(seq);
                while (m.find())
                    hits.add(new int[] { m.start(), m.end() });
            } else {
                String q = motif.toUpperCase();
                int from = 0;
                while (true) {
                    int idx = seq.indexOf(q, from);
                    if (idx < 0)
                        break;
                    hits.add(new int[] { idx, idx + q.length() });
                    from = idx + 1;
                }
            }
        } catch (PatternSyntaxException e) {
            showError("Invalid regex: " + e.getDescription());
            return;
        }

        motifSummary.setText(hits.isEmpty()
                ? "No matches"
                : ("Matches: " + hits.size()));
        renderSequenceWithHighlights(seq, hits);
    }

    @FXML
    private void onScanRestrictionSites() {
        SequenceRecord rec = sequenceList.getSelectionModel().getSelectedItem();
        if (rec == null) {
            showError("Select a sequence first.");
            return;
        }
        cutSites.setAll(RestrictionService.findSites(rec.seq()));
        if (cutSites.isEmpty()) {
            showError("No sites found for the built-in enzymes.");
        }
    }

    @FXML
    private void onExportRestrictionCsv() {
        if (cutSites.isEmpty()) {
            showError("No sites to export.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Restriction Sites CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showSaveDialog(reTable.getScene().getWindow());
        if (f == null)
            return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(f, java.nio.charset.StandardCharsets.UTF_8)) {
            pw.println("seq_id,enzyme,site,position,strand");
            String seqId = sequenceList.getSelectionModel().getSelectedItem() != null
                    ? sequenceList.getSelectionModel().getSelectedItem().id()
                    : "";
            for (var cs : cutSites) {
                pw.printf("%s,%s,%s,%d,%s%n", seqId, cs.getEnzyme(), cs.getSite(), cs.getPosition(), cs.getStrand());
            }
        } catch (Exception ex) {
            showError("Failed to save CSV:\n" + ex.getMessage());
        }
    }

    @FXML
    private void onAddCustomEnzyme() {
        String name = reNameField != null ? reNameField.getText() : null;
        String site = reSiteField != null ? reSiteField.getText() : null;
        int offset = 0;
        try {
            if (reOffsetField != null && reOffsetField.getText() != null && !reOffsetField.getText().isBlank()) {
                offset = Integer.parseInt(reOffsetField.getText().trim());
            }
        } catch (NumberFormatException ex) {
            showError("Cut offset must be an integer (0-based).");
            return;
        }
        if (name == null || name.isBlank() || site == null || site.isBlank()) {
            showError("Enter enzyme name and site (IUPAC).");
            return;
        }
        com.konsel.genelab.bio.RestrictionService.addCustomEnzyme(name, site, offset);
        if (reNameField != null)
            reNameField.clear();
        if (reSiteField != null)
            reSiteField.clear();
        if (reOffsetField != null)
            reOffsetField.clear();

        // optional rescan
        SequenceRecord rec = sequenceList.getSelectionModel().getSelectedItem();
        if (rec != null)
            cutSites.setAll(com.konsel.genelab.bio.RestrictionService.findSites(rec.seq()));
    }

    @FXML
    private void onClearCustomEnzymes() {
        RestrictionService.clearCustomEnzymes();
        // Optional refresh
        SequenceRecord rec = sequenceList.getSelectionModel().getSelectedItem();
        if (rec != null) {
            cutSites.setAll(RestrictionService.findSites(rec.seq()));
        } else {
            cutSites.clear();
        }
    }

    @FXML
    private void onRunDigestLinear() { // μπορείς να κρατήσεις το ίδιο όνομα
        SequenceRecord rec = sequenceList.getSelectionModel().getSelectedItem();
        if (rec == null) {
            showError("Select a sequence first.");
            return;
        }

        var sel = reTable != null ? reTable.getSelectionModel().getSelectedItems() : null;
        java.util.List<String> enzymes = new java.util.ArrayList<>();
        if (sel != null && !sel.isEmpty()) {
            java.util.Set<String> set = new java.util.LinkedHashSet<>();
            for (var cs : sel)
                set.add(cs.getEnzyme());
            enzymes.addAll(set);
        }

        boolean circular = reCircularCheck != null && reCircularCheck.isSelected();
        var frags = circular
                ? com.konsel.genelab.bio.RestrictionService.digestCircular(rec.seq(),
                        enzymes.isEmpty() ? null : enzymes)
                : com.konsel.genelab.bio.RestrictionService.digestLinear(rec.seq(), enzymes.isEmpty() ? null : enzymes);

        fragments.setAll(frags);
    }

    @FXML
    private void onExportFragmentsCsv() {
        if (fragments.isEmpty()) {
            showError("No fragments to export.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Fragments CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File f = fc.showSaveDialog(
                (fragTable != null ? fragTable.getScene().getWindow() : sequenceList.getScene().getWindow()));
        if (f == null)
            return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(f, java.nio.charset.StandardCharsets.UTF_8)) {
            pw.println("seq_id,start,end,length");
            String seqId = sequenceList.getSelectionModel().getSelectedItem() != null
                    ? sequenceList.getSelectionModel().getSelectedItem().id()
                    : "";
            for (var fr : fragments) {
                pw.printf("%s,%d,%d,%d%n", seqId, fr.getStart(), fr.getEnd(), fr.getLength());
            }
        } catch (Exception ex) {
            showError("Failed to save CSV:\n" + ex.getMessage());
        }
    }

    private void showError(String msg){
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
