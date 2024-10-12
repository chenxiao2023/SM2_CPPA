package com.example.socketlw.SM2Utils.TTT;

import com.example.socketlw.CertificateGenerator;
import com.example.socketlw.SM2Utils.Util;

import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.bouncycastle.pqc.legacy.math.linearalgebra.ByteUtils;
import org.bouncycastle.util.Arrays;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest22 {
    private ECDomainParameters ecParams; // SM2曲线参数
    private byte[] publicKeySM2 = new byte[0];
    private byte[] privateKeySM2 = new byte[0];
    String chainS="560AF94CC1C8BB9AE6986502136B425D";
    private byte[] chainroot = chainS.getBytes();

    @Test
    public void addition_isCorrect() throws Exception {
       // CertificateGenerator.verifyCert();
    }

    // 生成哈希
    public static  byte[] Hashpkichain(byte[] publicKeySM2,int i,byte[] chainroot) {
        //切割公钥
        int midIndex1 = publicKeySM2.length / 2;
        byte[] firstHalfpublicKeySM2 = Arrays.copyOfRange(publicKeySM2, 0, midIndex1); // 前半部分
       // System.out.println("firstHalfpublicKeySM2:"+Util.byte2HexStr(firstHalfpublicKeySM2));
        // 创建哈希实例
        SM3Digest digest = new SM3Digest();
        // 准备输入字节数组
        byte[] iBytes = ByteBuffer.allocate(4).putInt(i).array();
        byte[] input = new byte[publicKeySM2.length + iBytes.length + chainroot.length];
        // 将公钥、索引i和chainroot合并到输入数组中
        System.arraycopy(publicKeySM2, 0, input, 0, publicKeySM2.length); // 公钥
        System.arraycopy(iBytes, 0, input, publicKeySM2.length, iBytes.length); // 索引i
        System.arraycopy(chainroot, 0, input, publicKeySM2.length + iBytes.length, chainroot.length); // chainroot
        // 更新哈希
        digest.update(input, 0, input.length);// 32 字节（256 位）
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
       // System.out.println("Hash (Hex): " + Util.byte2HexStr(hash));
        return hash;
    }


    // 将字节数组转换为椭圆曲线公钥
    public static ECPoint convertToPublicKey(byte[] publicKeySM2) {
        // 打印公钥字节数组
     //   System.out.println("Public Key (Hex): " + Util.byte2HexStr(publicKeySM2));
        X9ECParameters x9 = ECNamedCurveTable.getByName("sm2p256v1");
        ECCurve mCurve = x9.getCurve();
        // 添加前缀
        byte[] fullPublicKey = new byte[publicKeySM2.length + 1];
        fullPublicKey[0] = 0x04;  // 添加未压缩前缀
        System.arraycopy(publicKeySM2, 0, fullPublicKey, 1, publicKeySM2.length);
     //   System.out.println("fullPublicKey: " + Util.byte2HexStr(fullPublicKey));
        return mCurve.decodePoint(fullPublicKey);  // 解码公钥点
    }


    // 正确的推导新的私钥
    public static byte[] deriveSk(BigInteger d, BigInteger d2, BigInteger curveOrder) {
        //G乘以skH
        BigInteger skH= d.multiply(d2).mod(curveOrder);//模模数
        byte[] skHB = skH.toByteArray();
        if (skHB.length == 33) {
            skHB = ByteUtils.subArray(skHB, 1, skHB.length);
        }
        X9ECParameters x9 = ECNamedCurveTable.getByName("sm2p256v1");
        ECPoint GskH = (new FixedPointCombMultiplier()).multiply(x9.getG(),skH);
        byte[] GskHB = GskH.getEncoded(false);
        if (GskHB.length == 65) {
            GskHB = ByteUtils.subArray(GskHB, 1, GskHB.length);
        }
      //  System.out.println("skHB:"+Util.byte2HexStr(skHB));
        System.out.println("GSkHB:"+Util.byte2HexStr(GskHB));

        return skHB;
    }


    // 正确的推导新的公钥
    public static byte[] derivePk(byte[] publicKeySM2, BigInteger d2) {
        //将pk乘以哈希
        ECPoint P= ExampleUnitTest22.convertToPublicKey(publicKeySM2);
        ECPoint P2 = (new FixedPointCombMultiplier()).multiply(P, d2);
        byte[] p2B = P2.getEncoded(false);
        if (p2B.length == 65) {
            p2B = ByteUtils.subArray(p2B, 1, p2B.length);
        }
        System.out.println("pk乘hash:"+Util.byte2HexStr(p2B));
        return p2B;
    }
}