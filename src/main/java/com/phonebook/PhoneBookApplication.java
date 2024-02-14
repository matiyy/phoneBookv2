package com.phonebook;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Główna klasa aplikacji PhoneBookApplication.
 */
public class PhoneBookApplication extends Application {
    /**
     * Metoda start inicjuje główne okno aplikacji.
     *
     * @param stage Główne okno aplikacji.
     * @throws IOException Błąd podczas wczytywania pliku FXML.
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PhoneBookApplication.class.getResource("phonebook.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        stage.setTitle("PhoneBook");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Metoda main rozpoczyna działanie aplikacji.
     *
     * @param args Argumenty wiersza poleceń.
     */
    public static void main(String[] args) {
        launch();
    }
}
