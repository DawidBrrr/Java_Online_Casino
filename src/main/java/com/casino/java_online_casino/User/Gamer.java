package com.casino.java_online_casino.User;

import java.util.Date;

public class Gamer {
    private int userId;
    private String name;
    private String lastName;
    private String nickName;
    private String email;
    private String password;
    private Date dateOfBirth;
    private float credits;

    public Gamer(int useId, String name, String lastName, String nickName, String email, String password, float credits, Date dateOfBirth) {
        this.userId = useId;
        this.name = name;
        this.lastName = lastName;
        this.nickName = nickName;
        this.credits = credits;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.password = password;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int useId) {
        this.userId = useId;
    }

    public float getCredits() {
        return credits;
    }

    public void setCredits(float credits) {
        this.credits = credits;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public Date getDateOfBirth() {
        return dateOfBirth;
    }
    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

}
