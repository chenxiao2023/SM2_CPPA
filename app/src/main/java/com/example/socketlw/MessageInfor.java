package com.example.socketlw;
public class MessageInfor {
    private int ID;
    private String msg;
    private Long time;
    private Long userID;
    private String type;
    private String signature;
    /**
     * 作者 LinOwl
     * 2021.02.17
     */
    public MessageInfor(String msg, Long time, Long userID, String signature,String type) {
        this.msg = msg;
        this.time = time;
        this.userID = userID;
        this.type = type;
        this.signature=signature;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Long getUserID() {
        return userID;
    }

    //添加了签名
    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSignature() {return signature;}

    public void setUserID(Long userID) {
        this.userID = userID;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }




}
