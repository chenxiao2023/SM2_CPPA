package com.example.socketlw;

import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Test;

import static org.junit.Assert.*;

import java.math.BigInteger;


import cn.mtjsoft.lib_encryption.SM2.SM2Util;
import cn.mtjsoft.lib_encryption.utils.Util;


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    private ECDomainParameters ecParams; // SM2曲线参数
    private byte[] publicKeySM2 = new byte[0];
    private byte[] privateKeySM2 = new byte[0];
    private byte[] chainroot = new byte[] {0x01, 0x02, 0x03, 0x04};

    @Test
    public void addition_isCorrect() {
        //产生密钥
        byte[][] key = SM2Util.generateKeyPair();
        publicKeySM2 = key[0];
        System.out.println("publicKeySM2BBB:"+ java.util.Arrays.toString(publicKeySM2));
        privateKeySM2 = key[1];
        System.out.println("publicKeySM2:"+ Util.byte2HexStr(publicKeySM2));
        System.out.println("privateKeySM2:"+Util.byte2HexStr(privateKeySM2));

        int midIndex1 = publicKeySM2.length / 2;
        byte[] firstHalfpublicKeySM2 = Arrays.copyOfRange(publicKeySM2, 0, midIndex1); // 前半部分
        System.out.println("firstHalfpublicKeySM2:"+Util.byte2HexStr(firstHalfpublicKeySM2));
        //
        // 创建哈希实例
        SM3Digest digest = new SM3Digest();

        // 准备输入字节数组
        BigInteger i = BigInteger.valueOf(1);
        byte[] input = new byte[publicKeySM2.length + i.toByteArray().length + chainroot.length];

        // 将公钥、索引i和chainroot合并到输入数组中
        System.arraycopy(publicKeySM2, 0, input, 0, publicKeySM2.length); // 公钥
        System.arraycopy(i.toByteArray(), 0, input, publicKeySM2.length, i.toByteArray().length); // 索引i
        System.arraycopy(chainroot, 0, input, publicKeySM2.length + i.toByteArray().length, chainroot.length); // chainroot

        // 更新哈希
        digest.update(input, 0, input.length);// 32 字节（256 位）
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);

        System.out.println("Hash (Hex): " + Util.byte2HexStr(hash));

        //截取哈希的前半部分和后半部分
        int midIndex = hash.length / 2;
        byte[] firstHalf = Arrays.copyOfRange(hash, 0, midIndex); // 前半部分
        byte[] secondHalf = Arrays.copyOfRange(hash, midIndex, hash.length); // 后半部分

        // 输出结果
        System.out.println("First Half: " + Util.byte2HexStr(firstHalf));
        System.out.println("Second Half: " + Util.byte2HexStr(secondHalf));

        // 将字节数组转换为密钥对象
        BigInteger skRoot = new BigInteger(1, privateKeySM2);
        System.out.println("skRoot="+skRoot);

        ECPoint pkRoot = ExampleUnitTest.convertToPublicKey(publicKeySM2);
        // 获取 x 和 y 坐标
        BigInteger xCoordinate = pkRoot.getXCoord().toBigInteger();

        BigInteger yCoordinate = pkRoot.getYCoord().toBigInteger();

        // 输出坐标
        System.out.println("X Coordinate: " + xCoordinate); // 以十六进制格式输出
        System.out.println("Y Coordinate: " + yCoordinate.toString(16)); // 以十六进制格式输出

        // 获取曲线阶数 (n)
        X9ECParameters x9 = ECNamedCurveTable.getByName("sm2p256v1");
        BigInteger curveOrder = x9.getN();
        System.out.println("阶数为："+curveOrder);

        BigInteger hashValue = new BigInteger(1, firstHalf);
        System.out.println("hashvalue="+hashValue);
        BigInteger hashValueInRange = hashValue.mod(curveOrder);
        System.out.println("hashValueInRange="+hashValueInRange);

        // 推导新的私钥
        BigInteger newPrivateKey = ExampleUnitTest.derivePrivateKey(skRoot, hashValueInRange, curveOrder);
        //输出新私钥和对应公钥
        System.out.println("New Derived Private Key: " + newPrivateKey.toString(16));
        if(SM2Util.isValidPrivateKey(newPrivateKey.toByteArray())){
            System.out.println("密钥形式正确！");
            // 获取 x 坐标的十六进制表示
            String newPr = newPrivateKey.toString(16);
            // 转换为字节数组
            byte[] newPrB = hexStringToByteArray(newPr);
            System.out.println("toString对应的公钥为："+Util.byte2HexStr(SM2Util.getPublicKeyFromPrivateKey(newPrB)));
            System.out.println("toByteArray()对应的公钥为："+Util.byte2HexStr(SM2Util.getPublicKeyFromPrivateKey(newPrivateKey.toByteArray())));
        };


        // 推导新的公钥
        ECPoint newPublicKey = ExampleUnitTest.derivePublicKey(pkRoot, hashValueInRange);
        String fullPublicKeyHex = getFullPublicKeyHex(newPublicKey);

        System.out.println("New Derived Public Key:" + fullPublicKeyHex);


    }




    // 将字节数组转换为椭圆曲线公钥
    public static ECPoint convertToPublicKey(byte[] publicKeySM2) {
        // 打印公钥字节数组
        System.out.println("Public Key (Hex): " + Util.byte2HexStr(publicKeySM2));
        X9ECParameters x9 = ECNamedCurveTable.getByName("sm2p256v1");
        ECCurve mCurve = x9.getCurve();
        // 添加前缀
        byte[] fullPublicKey = new byte[publicKeySM2.length + 1];
        fullPublicKey[0] = 0x04;  // 添加未压缩前缀
        System.arraycopy(publicKeySM2, 0, fullPublicKey, 1, publicKeySM2.length);
        System.out.println("fullPublicKey: " + Util.byte2HexStr(fullPublicKey));
        return mCurve.decodePoint(fullPublicKey);  // 解码公钥点
    }

    // 推导新的私钥
    public static BigInteger derivePrivateKey(BigInteger skRoot, BigInteger hashValue, BigInteger curveOrder) {
        // (skRoot * hashValue) mod n (曲线阶数)
        System.out.println("skRoot="+skRoot);
        System.out.println("skRoot.multiply(hashValue)="+skRoot.multiply(hashValue));
        System.out.println("skRoot.multiply(hashValue).mod(curveOrder)="+skRoot.multiply(hashValue).mod(curveOrder));
        return skRoot.multiply(hashValue).mod(curveOrder);
    }

    // 推导新的公钥
    public static ECPoint derivePublicKey(ECPoint pkRoot, BigInteger hashValue) {
        // hashValue * pkRoot (椭圆曲线上的点乘)
        return pkRoot.multiply(hashValue);
    }

    // 将 ECPoint 转换为十六进制字符串
    public static String getFullPublicKeyHex(ECPoint point) {
        // 获取 x 和 y 坐标
     //   byte[] xBytes = point.getXCoord().toBigInteger().toByteArray();
      //  byte[] yBytes = point.getYCoord().toBigInteger().toByteArray();
        String xBytes = point.getXCoord().toBigInteger().toString(16);
        String yBytes = point.getYCoord().toBigInteger().toString(16);
        // 转换为字节数组
        byte[] newx = hexStringToByteArray(xBytes);
        byte[] newy = hexStringToByteArray(yBytes);
        System.out.println("x坐标为："+Util.byte2HexStr(newx));
        System.out.println("y坐标为："+Util.byte2HexStr(newy));
        // 创建完整的公钥字节数组
        byte[] fullPublicKey = new byte[1 + newx.length + newy.length];

        // 设置前缀
        fullPublicKey[0] = 0x04; // 未压缩公钥前缀

        // 将 x 和 y 坐标复制到字节数组中
        System.arraycopy(newx, 0, fullPublicKey, 1, newx.length);
        System.arraycopy(newy, 0, fullPublicKey, 1 + newy.length, newy.length);

        // 返回十六进制字符串
        return Util.byte2HexStr(fullPublicKey);
    }

    //字符串转字节函数
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}

