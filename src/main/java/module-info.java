module com.konsel.genelab {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.swing;

    // Ο FXML loader χρησιμοποιεί reflection -> χρειάζεται "opens" στο πακέτο με τον
    // controller
    opens com.konsel.genelab.ui to javafx.fxml;

    // (προαιρετικά) αν κάποια πακέτα τα χρειάζεται εξωτερικός κώδικας, μπορείς να
    // τα exportάρεις:
    exports com.konsel.genelab;
    exports com.konsel.genelab.model;
    exports com.konsel.genelab.bio;
    exports com.konsel.genelab.io;
}
