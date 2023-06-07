package com.station.stationkitkt

import android.util.Base64
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

object RSATools {
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun getPublicKey(publicKeyStr: String): PublicKey {
        val keySpec = X509EncodedKeySpec(Base64.decode(publicKeyStr, Base64.DEFAULT))
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }

    /**
     * 用公钥加密,默认填充模式 RSA/ECB/PKCS1Padding
     * <br></br>每次加密的字节数，不能超过密钥的长度值减去11
     *
     * @param data  需加密数据的byte数据
     * @param publicKey 公钥
     * @return 加密后的byte型数据
     */
    @Throws(Exception::class)
    fun encrypt(data: ByteArray, publicKey: PublicKey): ByteArray {
        return encrypt(data, publicKey, "RSA/ECB/PKCS1Padding")
    }

    /**
     * 用公钥加密
     * <br></br>每次加密的字节数，不能超过密钥的长度值减去11
     * <br></br>加密时只能一次性加密秘钥长度减11字节的数据，假设秘钥长度为1024（下同），那么每次只能加密117字节（1024 / 8 - 11），若数据过长请分段加密。
     *
     * @param data  需加密数据的byte数据
     * @param publicKey 公钥
     * @param transformation 加密模式和填充方式
     * @return 加密后的byte型数据
     */
    @Throws(Exception::class)
    fun encrypt(data: ByteArray, publicKey: PublicKey, transformation: String): ByteArray {
        val cipher = Cipher.getInstance(transformation)
        // 编码前设定编码方式及密钥
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        // 传入编码数据并返回编码结果
        return cipher.doFinal(data)
    }

    fun base64Bytes(data: ByteArray): String {
        return Base64.encodeToString(data, Base64.DEFAULT)
    }
}