package com.phonebook;
import javafx.application.Platform;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import javafx.scene.control.TextFormatter.Change;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class PhoneBookController implements Initializable {

    @FXML
    private TextField nameTextField;

    @FXML
    private TextField surnameTextField;

    @FXML
    private TextField idTextField;

    @FXML
    private TextField phoneNumberTextField;

    @FXML
    private TableView<Contact> contactTableView;

    @FXML
    private TableColumn<Contact, String> idColumn;

    @FXML
    private TableColumn<Contact, String> nameColumn;

    @FXML
    private TableColumn<Contact, String> surnameColumn;

    @FXML
    private TableColumn<Contact, String> phoneNumberColumn;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) { //przedstawia inicjalizację widoku,
        // konfigurację kolumn tabeli oraz wczytanie kontaktów z bazy danych
        configureTableColumns();
        loadContactsFromDatabase();

        // Dodaj walidację do phoneNumberTextField
        phoneNumberTextField.setTextFormatter(new TextFormatter<>(this::validatePhoneNumber));
    } // dzieki temu w polu phonNumber mozna wpisywac cyfry

    // Metoda walidująca wprowadzony numer telefonu
    private Change validatePhoneNumber(Change change) {
        if (!change.getControlNewText().matches("\\d*")) { // sprawdza czy zawiera tylko cyfry
            return null;
        }
        return change;
    }
    @FXML
    private void handleExportButton() {
        try {
            FileWriter fileWriter = new FileWriter("contacts.txt");
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for (Contact contact : contactTableView.getItems()) {
                bufferedWriter.write(contact.getId() + "," + contact.getName() + "," + contact.getSurname() + "," + contact.getPhoneNumber());
                bufferedWriter.newLine();
            }

            bufferedWriter.close();
            System.out.println("Eksportowano kontakty do pliku tekstowego.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        clearInputFields();
    }




    // Sprawdza, czy wprowadzony numer telefonu zawiera tylko cyfry
    private boolean validatePhoneNumberInput(String phoneNumber) {
        return phoneNumber.matches("\\d*");
    }

    // Konfiguruje kolumny tabeli
    private void configureTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        surnameColumn.setCellValueFactory(new PropertyValueFactory<>("surname"));
        phoneNumberColumn.setCellValueFactory(cellData -> cellData.getValue().phoneNumberProperty());

        contactTableView.setEditable(true);

        nameColumn.setCellFactory(getCustomTextFieldTableCell());
        nameColumn.setOnEditCommit(this::handleEditCommit);

        surnameColumn.setCellFactory(getCustomTextFieldTableCell());
        surnameColumn.setOnEditCommit(this::handleEditCommit);

        phoneNumberColumn.setCellFactory(getCustomTextFieldTableCell());
        phoneNumberColumn.setOnEditCommit(this::handlePhoneNumberEditCommit);
    }

    // Uzyskuje niestandardową komórkę tekstową dla edycji w tabeli
    private Callback<TableColumn<Contact, String>, TableCell<Contact, String>> getCustomTextFieldTableCell() {
        return TextFieldTableCell.forTableColumn(); // zwraca niestandardowa komorke
    }

    // Obsługuje zatwierdzenie edycji dla kolumny nameColumn i surnameColumn
    private void handleEditCommit(TableColumn.CellEditEvent<Contact, String> event) {
        Contact contact = event.getRowValue();
        contact.setValue(event.getTableColumn().getText(), event.getNewValue());
        updateContactInDatabase(contact);
    }

    // Obsługuje zatwierdzenie edycji dla kolumny phoneNumberColumn
    private void handlePhoneNumberEditCommit(TableColumn.CellEditEvent<Contact, String> event) {
        Contact contact = event.getRowValue();
        contact.setPhoneNumber(event.getNewValue());
        contact.updatePhoneNumberInDatabase();
    }

    // Aktualizuje dane kontaktu w bazie danych
    private void updateContactInDatabase(Contact contact) {
        try (Connection connection = DatabaseConnector.connect()) {
            String query = "UPDATE contacts SET name = ?, surname = ? WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) { //tworzy obiekt
                preparedStatement.setString(1, contact.getName());
                preparedStatement.setString(2, contact.getSurname());
                preparedStatement.setInt(3, Integer.parseInt(contact.getId()));

                int rowsAffected = preparedStatement.executeUpdate(); // wykonuje zapytanie

                if (rowsAffected > 0) {
                    System.out.println("Zaktualizowano dane kontaktu w bazie danych."); // walidacja czy sie udalo
                } else {
                    System.out.println("Nie zaktualizowano danych kontaktu. Sprawdź, czy kontakt istnieje w bazie danych.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    // Zapisuje kontakt do bazy danych
    // Zapisuje kontakt do bazy danych
    private void saveContactToDatabase(Contact contact) {
        try (Connection connection = DatabaseConnector.connect()) {
            String query;
            if (contact.getId() != null && !contact.getId().isEmpty()) {
                query = "INSERT INTO contacts (id, name, surname, phone_number) VALUES (?, ?, ?, ?)";
            } else {
                query = "INSERT INTO contacts (name, surname, phone_number) VALUES (?, ?, ?)";
            }


            try (PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                if (contact.getId() != null && !contact.getId().isEmpty()) {
                    preparedStatement.setInt(1, Integer.parseInt(contact.getId()));
                    preparedStatement.setString(2, contact.getName());
                    preparedStatement.setString(3, contact.getSurname());
                    preparedStatement.setString(4, contact.getPhoneNumber());
                } else {
                    preparedStatement.setString(1, contact.getName());
                    preparedStatement.setString(2, contact.getSurname());
                    preparedStatement.setString(3, contact.getPhoneNumber());
                }

                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected > 0) {
                    if (contact.getId() == null || contact.getId().isEmpty()) {
                        // Jeżeli nie było podane ID, odczytaj wygenerowane automatycznie
                        ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            contact.setId(String.valueOf(generatedKeys.getInt(1)));
                        }
                    }

                    System.out.println("Zapisano kontakt do bazy danych.");
                } else {
                    System.out.println("Nie zapisano kontaktu do bazy danych.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // Wczytuje kontakty z bazy danych
    private void loadContactsFromDatabase() {
        try (Connection connection = DatabaseConnector.connect()) {
            String query = "SELECT id, name, surname, phone_number FROM contacts";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                ResultSet resultSet = preparedStatement.executeQuery();

                while (resultSet.next()) {
                    String id = String.valueOf(resultSet.getInt("id"));
                    String name = resultSet.getString("name");
                    String surname = resultSet.getString("surname");
                    String phoneNumber = resultSet.getString("phone_number");

                    Contact contact = new Contact(id, name, surname, phoneNumber);
                    contactTableView.getItems().add(contact);
                }
            }


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Obsługuje przycisk dodawania kontaktu
    @FXML
    private void handleAddButton() {
        if (validatePhoneNumberInput(phoneNumberTextField.getText())) { // sprawdza czy podany numer spelnia walidacje
            Contact newContact = new Contact(idTextField.getText(), nameTextField.getText(), surnameTextField.getText(), phoneNumberTextField.getText());
            saveContactToDatabase(newContact); // zapisuje w bazie danych
            contactTableView.getItems().add(newContact); // dodaje kontakt na interfejsie uzytkownika



        } else {
            System.out.println("Błąd: Niepoprawny numer telefonu. Wprowadź tylko cyfry.");
        }
        clearInputFields();
    }

    // Obsługuje przycisk usuwania kontaktu
    @FXML
    private void handleRemoveButton() {
        Contact selectedContact = contactTableView.getSelectionModel().getSelectedItem();
        if (selectedContact != null) {
            deleteContactFromDatabase(selectedContact);
            contactTableView.getItems().remove(selectedContact);
        }
    }

    // Obsługuje przycisk czyszczenia listy kontaktów
    @FXML
    private void handleClearButton() {
        clearContactsInDatabase();
        contactTableView.getItems().clear();
    }

    // Usuwa kontakt z bazy danych
    private void deleteContactFromDatabase(Contact contact) {
        try (Connection connection = DatabaseConnector.connect()) {
            String query = "DELETE FROM contacts WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) { //tworzy bezpieczny obiekt
                int id = Integer.parseInt(contact.getId());
                preparedStatement.setInt(1, id); // ustawia pierwszy paramentr w zapytaniu na 'id'

                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();  // obsluga bledow z baza danych
        }
    }

    // Czyści kontakty w bazie danych
    private void clearContactsInDatabase() {
        try (Connection connection = DatabaseConnector.connect()) {
            String query = "DELETE FROM contacts";
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(query);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleImportButton() {
        try {
            FileReader fileReader = new FileReader("contacts.txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            List<Contact> importedContacts = new ArrayList<>();

            while ((line = bufferedReader.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length == 4) {
                    Contact contact = new Contact(data[0], data[1], data[2], data[3]);
                    importedContacts.add(contact);
                }
            }

            bufferedReader.close();

            // Clear existing data in the table
            contactTableView.getItems().clear();

            // Add imported contacts to the table
            for (Contact contact : importedContacts) {
                contactTableView.getItems().add(contact);

                // Save imported contact to the database
                saveContactToDatabase(contact);
                clearInputFields();
            }

            System.out.println("Zaimportowano kontakty z pliku tekstowego i zapisano w bazie danych.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Błąd podczas importu danych z pliku.");
        }
    }


        // Czyści pola tekstowe do wprowadzania danych
    private void clearInputFields() {
        idTextField.clear();
        nameTextField.clear();
        surnameTextField.clear();
        phoneNumberTextField.clear();
    }

    // Klasa reprezentująca Kontakt
    public static class Contact {
        private final StringProperty id;
        private final StringProperty name;
        private final StringProperty surname;
        private final StringProperty phoneNumber;

        public Contact(String id, String name, String surname, String phoneNumber) {
            this.id = new SimpleStringProperty(id);
            this.name = new SimpleStringProperty(name);
            this.surname = new SimpleStringProperty(surname);
            this.phoneNumber = new SimpleStringProperty(phoneNumber);
            // tworzy obiekty i przpisuje
        }

        // Pobiera ID kontaktu
        public String getId() {
            return id.get();
        }
        public void setId(String id) {
            this.id.set(id);
        }

        // Pobiera imię kontaktu
        public String getName() {
            return name.get();
        }

        // Pobiera nazwisko kontaktu
        public String getSurname() {
            return surname.get();
        }

        // Pobiera numer telefonu kontaktu
        public String getPhoneNumber() {
            return phoneNumber.get();
        }

        // Ustawia wartość dla danej właściwości
        public void setValue(String propertyName, String value) {
            switch (propertyName) {
                case "name":
                    name.set(value);
                    break;
                case "surname":
                    surname.set(value);
                    break;
                case "phoneNumber":
                    phoneNumber.set(value);
                    break;
            }
        }

        // Ustawia numer telefonu
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber.set(phoneNumber);
        }

        // Zwraca właściwość dla imienia
        public StringProperty nameProperty() {
            return name;
        }

        // Zwraca właściwość dla nazwiska
        public StringProperty surnameProperty() {
            return surname;
        }

        // Zwraca właściwość dla numeru telefonu
        public StringProperty phoneNumberProperty() {
            return phoneNumber;
        }

        // Aktualizuje numer telefonu w bazie danych
        public void updatePhoneNumberInDatabase() {
            try (Connection connection = DatabaseConnector.connect()) {
                String query = "UPDATE contacts SET phone_number = ? WHERE id = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setString(1, getPhoneNumber());
                    preparedStatement.setInt(2, Integer.parseInt(getId()));
                    //jest używany do wykonywania zapytań SQL na bazie danych.

                    int rowsAffected = preparedStatement.executeUpdate();

                    if (rowsAffected > 0) {
                        System.out.println("Zaktualizowano numer telefonu w bazie danych.");
                    } else {
                        System.out.println("Nie zaktualizowano numeru telefonu. Sprawdź, czy kontakt istnieje w bazie danych.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Przesłania metoda toString() dla wygodnego wyświetlania kontaktu
        @Override
        public String toString() {
            return id.get() + " - " + name.get() + " " + surname.get() + " - " + phoneNumber.get();
        }

    }

}