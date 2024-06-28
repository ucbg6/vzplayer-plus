module com.vzplayer {
    requires javafx.graphics;
    requires uk.co.caprica.vlcj;
    requires uk.co.caprica.vlcj.javafx;
    requires javafx.fxml;
    requires javafx.controls;
    requires com.google.gson;
    requires com.jfoenix;

    opens com.vzplayer to javafx.graphics, javafx.fxml, javafx.controls, json.simple, com.google.gson, com.jfoenix;
    requires json.simple;
}
