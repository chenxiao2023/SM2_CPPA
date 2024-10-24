package com.example.SM2_CPPA.MainUse;
public class MessageInfor {
    private int ID;
    private String msg;
    private Long time;
    private Long userID;
    private String type;
    private byte[] signature;
    private byte[] publicKey;
    private String TxID;

    public MessageInfor(String msg, Long time, Long userID, byte[] signature,byte[] publicKey,String TxID,String type) {
        this.msg = msg;
        this.time = time;
        this.userID = userID;
        this.type = type;
        this.signature=signature;
        this.TxID=TxID;
        this.publicKey=publicKey;
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
    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public byte[] getSignature() {return signature;}

    //添加公钥
    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getPublicKey() {return publicKey;}

    //添加了事务标识
    public void setTxID(String TxID) {
        this.TxID = TxID;
    }

    public String getTxID() {return TxID;}


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
