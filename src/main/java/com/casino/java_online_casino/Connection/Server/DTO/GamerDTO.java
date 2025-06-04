package com.casino.java_online_casino.Connection.Server.DTO;

import java.util.Date;

public class GamerDTO {
    private String name;
    private String lastName;
    private String nickName;
    private String email;
    private float credits;

    public GamerDTO(String name, String lastName, String nickName, String email, float credits) {
        this.name = name;
        this.lastName = lastName;
        this.nickName = nickName;
        this.email = email;
        this.credits = credits;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public float getCredits() {
        return credits;
    }

    public void setCredits(float credits) {
        this.credits = credits;
    }

}
