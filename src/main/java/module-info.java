module com.example.phonebook {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens com.phonebook to javafx.fxml;
    exports com.phonebook;
}