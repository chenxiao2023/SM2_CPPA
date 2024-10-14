package com.example.socketlw;

import com.example.socketlw.SM2Utils.Util;

import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import androidx.appcompat.app.AppCompatActivity;

import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.encoders.Hex;


import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

//import org.bouncycastle.util.encoders.Hex;



import java.io.ByteArrayInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;

import java.io.InputStream;
import java.math.BigInteger;

import java.security.*;


import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.junit.Test;
import org.bouncycastle.jce.provider.BouncyCastleProvider;




public class CertificateGenerator {

    public static String makeCert(String publicKeyUser) throws Exception{
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // 读取CA私钥
        PEMParser pemParsersk = new PEMParser(new FileReader("/data/user/0/com.example.socketlw/files/pem"));
        Object objectsk = pemParsersk.readObject();
       JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
       // JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        PrivateKey privateKeyCA = converter.getPrivateKey((PrivateKeyInfo) objectsk);
        pemParsersk.close();
        // 读取CA公钥
        PEMParser pemParserpk = new PEMParser(new FileReader("/data/user/0/com.example.socketlw/files/pub"));
        Object objectpk = pemParserpk.readObject();
        PublicKey publicKeyCA = converter.getPublicKey((SubjectPublicKeyInfo) objectpk);
        System.out.println("公钥为："+Hex.toHexString(publicKeyCA.getEncoded()));
        pemParserpk.close();


        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // 证书信息
        // 发行者issuer
        X500Name issuer = new X500Name("CN=Issuer, O=My Organization");
        // 主体subject
        X500Name subject = new X500Name("CN=Subject, O=My Organization");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        Date notAfter = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L);

        // 将十六进制字符串pk转换为字节数组
        byte[] publicKeyBytes = Hex.decode(publicKeyUser);


        // 构建算法标识符，使用 SM2 OID
        ASN1ObjectIdentifier sm2Oid = new ASN1ObjectIdentifier(GMObjectIdentifiers.sm2p256v1.toString()); // SM2 OID
        AlgorithmIdentifier algorithmIdentifier = new AlgorithmIdentifier(sm2Oid);

        // 创建 SubjectPublicKeyInfo
        ASN1BitString keyData = new DERBitString(publicKeyBytes);
        SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo(
                algorithmIdentifier,
                keyData
        );


        // 创建证书构建器
        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
                issuer,
                serial,
                notBefore,
                notAfter,
                subject,
                // subjectPublicKeyInfo: 用户的公钥信息
                subjectPublicKeyInfo
        );


        // 创建内容签名者
        ContentSigner contentSigner = new JcaContentSignerBuilder("SM3WITHSM2")
                .build(privateKeyCA);

        // 生成证书
        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(certificateBuilder.build(contentSigner));

        String type = certificate.getType();
        System.out.println("Certificate: " + Hex.toHexString(certificate.getEncoded()));

        //DER编码的字节流转十六进制字符串
        return Hex.toHexString(certificate.getEncoded());
    }
// 添加 BouncyCastle 提供程序

    // 读取CA公钥的方法
    public static PublicKey readPublicKey(String path) throws Exception {

       // Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        // 读取CA公钥
        PEMParser pemParserpk = new PEMParser(new FileReader(path));
        Object objectpk = pemParserpk.readObject();
        PublicKey publicKeyCA = converter.getPublicKey((SubjectPublicKeyInfo) objectpk);
        pemParserpk.close();
        return publicKeyCA;
    }



    //返回的是证书内的公钥
    public static String PkRecover(String encodeCert)throws Exception{
        byte[] Cert= Util.hexStr2Bytes(encodeCert);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificateRecover = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(Cert));
        PublicKey pk=certificateRecover.getPublicKey();
        String publickey=PKtoStr(pk);
        return publickey;
    }

    //把字符串转为证书
    public static X509Certificate certRecoverfromtx(String encodeCert)throws Exception{
        byte[] Cert= Hex.decode(encodeCert.substring(2));
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificateRecover = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(Cert));
        return certificateRecover;
    }

    public static  X509Certificate certRecover(String pkCert)throws Exception{

        byte[] encodeCert= Hex.decode(pkCert);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificateRecover = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(encodeCert));
        return certificateRecover;

    }

    public  X509Certificate certRecover2(String pkCert)throws Exception{
        byte[] encodeCert= Util.hexStr2Bytes(pkCert);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificateRecover = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(encodeCert));
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


    public static String verifyCert(String encodecert)throws Exception{
        //String pk="FC5B2396034B0C1807EED779B7D20F8C97E22CE4E6BA18156458BBCF76172AB9D0074745EB713CDDCB5C21A95A79631EE626F8F2266EF7BC9D8DF2B8C652D530";
       // String encodecert="3082013d3081e5a003020102020601927b8fea34300a06082a811ccf55018375302b310f300d06035504030c0649737375657231183016060355040a0c0f4d79204f7267616e697a6174696f6e301e170d3234313031303132333132325a170d3235313031313132333132325a302c3110300e06035504030c075375626a65637431183016060355040a0c0f4d79204f7267616e697a6174696f6e304f300a06082a811ccf5501822d034100fc5b2396034b0c1807eed779b7d20f8c97e22ce4e6ba18156458bbcf76172ab9d0074745eb713cddcb5c21a95a79631ee626f8f2266ef7bc9d8df2b8c652d530300a06082a811ccf5501837503470030440220492731d11d8c2aaeaede3ee6b7a605ed5d6d41e1ab3a0d9e466c0045227961e402206d19f37cfd5950341ea0c3068c165130739be5469257825bf33eed644cf2fb9d";
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        X509Certificate cert=certRecoverfromtx(encodecert);
        System.out.println("cert="+cert.toString());

        byte[] PKK=cert.getPublicKey().getEncoded();
        System.out.println("未截取公钥："+Util.byte2HexStr(PKK));
        int headerLength = 20;
        byte[] keyBits = new byte[PKK.length - headerLength];
       System.arraycopy(PKK, headerLength, keyBits, 0, keyBits.length);
        System.out.println("截取后的公钥："+Util.byte2HexStr(keyBits));
//        System.out.println("证书内部的公钥为："+(Arrays.toString(cert.getPublicKey().getEncoded())));
//        System.out.println("证书内部的公钥为："+Util.byte2HexStr(cert.getPublicKey().getEncoded()));
//        // 更新 Signature 对象以包含原始数据
//        System.out.println("cert.getTBSCertificate()="+Util.byte2HexStr(cert.getTBSCertificate()));
//        // 创建 Signature 实例
//        Signature signature = Signature.getInstance("1.2.156.10197.1.501"); // 使用证书的签名算法
//        PublicKey PkCA=MygetPublicKey();
//        signature.initVerify(PkCA);
//        signature.update(cert.getTBSCertificate());
//        boolean isValid = signature.verify(signedData);

//        System.out.println("sigalgname: "+cert.getSigAlgName());
//        System.out.println("signedData="+Util.byte2HexStr(signedData));
//        System.out.println("验证通过"+isValid);
        return Util.byte2HexStr(keyBits);

        //String pkinCert=PkRecover(encodecert);
        //System.out.println("证书内的公钥为："+pkinCert);;
       // PublicKey PkCA=cert.getPublicKey();
       // System.out.println("公钥CA="+PkCA);
//       // cert.verify(PkCA,"BC");
//        System.out.println("ok");
//
//
//        // 创建 Signature 实例
//        Signature signature = Signature.getInstance(cert.getSigAlgName()); // 使用证书的签名算法
//        System.out.println("签名算法为："+cert.getSigAlgName());
//       // signature.initVerify(PkCA);
//        System.out.println("cert.getTBSCertificate():"+Util.byte2HexStr(cert.getTBSCertificate()));
//        byte[] signedData=cert.getSignature();
//        // 更新 Signature 对象以包含原始数据
//        signature.update(cert.getTBSCertificate());
//
//        // 验证签名
//        boolean isValid = signature.verify(signedData);
//        System.out.println("signedData="+Util.byte2HexStr(signedData));
//        System.out.println("验证通过"+isValid);
//
//        certRecover(encodecert);

    }


    public static PublicKey MygetPublicKey()throws Exception{
        // 现在你可以获取文件路径
        String filePath="/data/user/0/com.example.socketlw/files/pub";
        // 在此处使用filePath
        PublicKey pk=readPublicKey(filePath);
        return pk;

    }


}
