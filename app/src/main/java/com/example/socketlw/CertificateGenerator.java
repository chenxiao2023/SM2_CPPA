package com.example.socketlw;

import com.example.socketlw.SM2Utils.Util;

import org.bouncycastle.util.encoders.Hex;
import org.spongycastle.asn1.ASN1BitString;
import org.spongycastle.asn1.DERBitString;
import org.spongycastle.asn1.x509.*;
import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.gm.GMObjectIdentifiers;
import org.spongycastle.asn1.pkcs.PrivateKeyInfo;
import org.spongycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

//import org.bouncycastle.util.encoders.Hex;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;


import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
//import org.bouncycastle.jce.provider.BouncyCastleProvider;



public class CertificateGenerator {

   /* public static String makeCert(String publicKeyUser) throws Exception{
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // 读取CA私钥
        PEMParser pemParsersk = new PEMParser(new FileReader("src/main/resources/account/gm/x4c26aecee34487d29adff978fd6791578ed8fd28.pem"));
        Object objectsk = pemParsersk.readObject();
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        PrivateKey privateKeyCA = converter.getPrivateKey((PrivateKeyInfo) objectsk);
        pemParsersk.close();
        // 读取CA公钥
        PEMParser pemParserpk = new PEMParser(new FileReader("src/main/resources/account/gm/x.pub"));
        Object objectpk = pemParserpk.readObject();
        PublicKey publicKeyCA = converter.getPublicKey((SubjectPublicKeyInfo) objectpk);
        pemParserpk.close();

        // 创建内容签名者
        ContentSigner contentSigner = new JcaContentSignerBuilder("SM3withSM2")
                .setProvider("BC").build(privateKeyCA);

        // 生成证书
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateBuilder.build(contentSigner));

        String type = certificate.getType();
        System.out.println("Certificate: " + Hex.toHexString(certificate.getEncoded()));

        //DER编码的字节流转十六进制字符串
        return Hex.toHexString(certificate.getEncoded());
    }*/
// 添加 BouncyCastle 提供程序

    // 读取公钥的方法
    public static PublicKey readPublicKey(String path) throws Exception {

        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("SC");
        // 读取CA公钥
        PEMParser pemParserpk = new PEMParser(new FileReader(path));
        Object objectpk = pemParserpk.readObject();
        PublicKey publicKeyCA = converter.getPublicKey((SubjectPublicKeyInfo) objectpk);
        pemParserpk.close();
        return publicKeyCA;
    }




    public static String PkRecover(String encodeCert)throws Exception{
        byte[] Cert= Util.hexStr2Bytes(encodeCert);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificateRecover = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(Cert));
        PublicKey pk=certificateRecover.getPublicKey();
        String publickey=PKtoStr(pk);
        return publickey;
    }


    public static X509Certificate certRecover(String encodeCert)throws Exception{
        byte[] Cert= Util.hexStr2Bytes(encodeCert.substring(2));
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificateRecover = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(Cert));
        return certificateRecover;
    }



    // 提取证书里的公钥，并将其格式化为十六进制的字符串形式
    public static String PKtoStr(PublicKey publicKey) {
        // getEncoded()：PublicKey的字节表示
        byte[] encodedKey = publicKey.getEncoded();
        // 由于unparsed keybits通常是公钥的实际数据部分，解析X.509结构
        // 假设我们跳过X.509的头部，直接获取实际的公钥数据部分
        // 例如对于SM2，前面可能是某些算法标识和序列化信息
        int headerLength = 19;
        byte[] keyBits = new byte[encodedKey.length - headerLength];
        System.arraycopy(encodedKey, headerLength, keyBits, 0, keyBits.length);
        // 返回unparsed keybits部分
        return Hex.toHexString(keyBits);
    }

}
