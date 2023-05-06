package com.example.securingweb;

import lombok.Data;

import java.sql.ConnectionBuilder;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;

    public User() {

    };
    public User(Long id, String username, String password, String firstName, String lastName, String phone, String email) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
    }
}