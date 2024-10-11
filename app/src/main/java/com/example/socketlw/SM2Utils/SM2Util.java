package com.example.socketlw.SM2Utils;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.engines.SM2Engine.Mode;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.PlainDSAEncoding;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.pqc.legacy.math.linearalgebra.ByteUtils;


import java.math.BigInteger;
import java.security.SecureRandom;

public class SM2Util {
    static final byte MODE_NO_COMPRESS = 4;

    private static final Mode SM2_CRYPT_MODE = Mode.C1C3C2;

    private SM2Util() {
        throw new UnsupportedOperationException("util class cant be instantiation");
    }

    public static boolean isValidPrivateKey(byte[] privateKey) {
        return SM2.Instance().isValidPrivateKey(privateKey);
    }

    public static byte[] getPublicKeyFromPrivateKey(byte[] privateKey) {
        return SM2.Instance().getPublicKeyFromPrivateKey(privateKey);
    }

    public static byte[][] generateKeyPair() {
        AsymmetricCipherKeyPair key = SM2.Instance().generateKeyPair();
        ECPrivateKeyParameters ecpriv = (ECPrivateKeyParameters) key.getPrivate();
        ECPublicKeyParameters ecpub = (ECPublicKeyParameters) key.getPublic();
        BigInteger privateKey = ecpriv.getD();
        ECPoint publicKey = ecpub.getQ();
        byte[] publicKeyEncoded = publicKey.getEncoded(false);
        if (publicKeyEncoded.length == 65) {
            publicKeyEncoded = ByteUtils.subArray(publicKeyEncoded, 1, publicKeyEncoded.length);
        }
        byte[] privateKeyEncoded = privateKey.toByteArray();
        if (privateKeyEncoded.length == 33) {
            privateKeyEncoded = ByteUtils.subArray(privateKeyEncoded, 1, privateKeyEncoded.length);
        }
        return new byte[][]{publicKeyEncoded, privateKeyEncoded};
    }

    public static byte[] encrypt(byte[] publicKey, byte[] data) {
        if (publicKey != null && publicKey.length != 0) {
            if (data != null && data.length != 0) {
                byte[] source = new byte[data.length];
                System.arraycopy(data, 0, source, 0, data.length);
                ECPublicKeyParameters publicKeyParameters = SM2.Instance().getPublicKeyParameters(publicKey);
                SM2Engine sm2Engine = new SM2Engine(SM2_CRYPT_MODE);
                sm2Engine.init(true, new ParametersWithRandom(publicKeyParameters, new SecureRandom()));
                byte[] bytes = new byte[0];

                try {
                    bytes = sm2Engine.processBlock(source, 0, source.length);
                    // 密文数据头部一位表示压缩模式，我们仅使用不压缩模式 0x04，因此移除该位
                    return ByteUtils.subArray(bytes, 1);
                } catch (InvalidCipherTextException var7) {
                    var7.printStackTrace();
                    return new byte[0];
                }
            } else {
                return new byte[0];
            }
        } else {
            return new byte[0];
        }
    }

    public static byte[] decrypt(byte[] privateKey, byte[] encryptedData) {
        if (privateKey != null && privateKey.length != 0) {
            if (encryptedData.length == 0) {
                return new byte[0];
            } else {
                encryptedData = ByteUtils.concatenate(new byte[]{4}, encryptedData);
                ECPrivateKeyParameters privateKeyParameters = SM2.Instance().getPrivateKeyParameters(privateKey);
                SM2Engine sm2Engine = new SM2Engine(SM2_CRYPT_MODE);
                sm2Engine.init(false, privateKeyParameters);
                try {
                    return sm2Engine.processBlock(encryptedData, 0, encryptedData.length);
                } catch (InvalidCipherTextException var5) {
                    var5.printStackTrace();
                    return new byte[0];
                }
            }
        } else {
            return new byte[0];
        }
    }

    public static byte[] sign(byte[] privateKey, byte[] sourceData) {
        ECPrivateKeyParameters privateKeyParameters = SM2.Instance().getPrivateKeyParameters(privateKey);
        SM2Signer signer = new SM2Signer(new PlainDSAEncoding());
        signer.init(true, privateKeyParameters);
        signer.update(sourceData, 0, sourceData.length);
        try {
            return signer.generateSignature();
        } catch (CryptoException var5) {
            return new byte[0];
        }
    }

    public static boolean verifySign(byte[] publicKey, byte[] sourceData, byte[] signData) {
        ECPublicKeyParameters publicKeyParameters = SM2.Instance().getPublicKeyParameters(publicKey);
        SM2Signer signer = new SM2Signer(new PlainDSAEncoding());
        signer.init(false, publicKeyParameters);
        signer.update(sourceData, 0, sourceData.length);
        return signer.verifySignature(signData);
    }
}
