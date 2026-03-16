package com.example.stockapp;
import jakarta.persistence.*;

@Entity
@Table(name = "usertable")
public class User {
    @Id
    @Column(name = "userid")
    private String userId;
    @Column(name = "userpw")
    private String userPw;
    private String cookie;

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserPw() { return userPw; }
    public void setUserPw(String userPw) { this.userPw = userPw; }
    public String getCookie() { return cookie; }
    public void setCookie(String cookie) { this.cookie = cookie; }
}

