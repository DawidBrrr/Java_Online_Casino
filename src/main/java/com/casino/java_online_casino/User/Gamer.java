package com.casino.java_online_casino.User;

public class Gamer {
    private int userId;
    private String name;
    private String lastName;
    private String nickName;
    private float credits;

    public Gamer(int useId, String name, String lastName, String nickName, float credits) {
        this.userId = useId;
        this.name = name;
        this.lastName = lastName;
        this.nickName = nickName;
        this.credits = credits;
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
}
