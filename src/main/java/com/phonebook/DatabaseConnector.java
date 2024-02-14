package com.phonebook;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {
    private static final String URL = "jdbc:postgresql://localhost:5432/phoneBase";
    private static final String USER = "postgres";
    private static final String PASSWORD = "12345";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
// wyszukiwanie
// zmiana id
// eskport, import do bazy z pliku