package com.example.socketlw.SM2Utils;

public class User {

    private byte[] publicKeySM2 = new byte[0];
    private byte[] privateKeySM2 = new byte[0];
    private int pkIndex=0;
    private byte[] publicKeySM2Cert= new byte[0];
    private String TxID="";


    public User(){}
    public User(byte[] publicKeySM2, byte[] privateKeySM2, int pkIndex) {
        this.privateKeySM2 = privateKeySM2;
        this.publicKeySM2 = publicKeySM2;
        this.pkIndex = pkIndex;
    }


    public byte[] getPublicKeySM2() {
        return publicKeySM2;
    }

    public byte[] getPrivateKeySM2() {
        return privateKeySM2;
    }

    public int getPkIndex() {
        return pkIndex;
    }

    public byte[] getPublicKeySM2Cert() {
        return publicKeySM2Cert;
    }

    public String getTxID() {
        return TxID;
    }

    public void setPublicKeySM2(byte[] publicKeySM2) {
        this.publicKeySM2 = publicKeySM2;
    }

    public void setPrivateKeySM2(byte[] privateKeySM2) {
        this.privateKeySM2 = privateKeySM2;
    }

    public void setPkIndex(int pkIndex) {
        this.pkIndex = pkIndex;
    }

    public void setPublicKeySM2Cert(byte[] publicKeySM2Cert) {
        this.publicKeySM2Cert = publicKeySM2Cert;
    }

    public void setTxID(String txID) {
        TxID = txID;
    }
}
