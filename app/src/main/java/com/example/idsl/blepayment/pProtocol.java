package com.example.idsl.blepayment;


import android.util.Log;

import java.math.BigInteger;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by IDSL on 2016/12/6.
 */

public class pProtocol {

    //var

    private BigInteger prime;

    public final String Sk;
    public final String IDp;
    public final String IDc;
    public final String N;
    public final String T;

    public pProtocol(final String Sk, final String N, final String T, final String IDc, final String IDp){
        this.Sk = Sk;
        this.IDp = IDp;
        this.IDc = IDc;
        this.N = N;
        this.T = T;
    }
    public void run(){

        //step 1
        byte[] r1 = new byte[33]; //Means 90 x 8 bit
        Random rn = new Random();
        rn.nextBytes(r1);
        final byte[] V1 = xor(r1,(this.Sk+this.N).getBytes());
        String X = hmacSha1(this.Sk+new String(r1),new String(V1)+IDc);

        //step 2
        //transmit X and V1


        byte[] r2 = new byte[33]; //Means 90 x 8 bit
        rn.nextBytes(r2);
        final byte[] r1_ = xor(V1,(this.Sk+this.N).getBytes());
        String X_ = hmacSha1(this.Sk+new String(r1_),new String(V1)+IDc);
        //auth X
        if(X_.equals(X)){
            Log.d("BERHASIL",X);
        }else {Log.d("wew","gagal");}
        final byte[] V2 = xor(xor(r2,r1_),(this.Sk+this.N).getBytes());
        String Y = hmacSha1(this.Sk+new String(r2),new String(r1_)+new String(r2)+new String(V2)+IDp);

        //step 3
        //transmit Y and V2
        final byte[] r2_ = xor(xor(V2,(this.Sk+this.N).getBytes()),r1);
        String Y_ = hmacSha1(this.Sk+new String(r2_),new String(r1)+new String(r2_)+new String(V2)+IDp);
        if(X_.equals(X)){
            Log.d("BERHASIL",Y_);
        }else {Log.d("wew","gagal");}
        byte[] xor12 = xor(r1,r2);
        byte[] keyEnc = new byte[32];
       // System.arraycopy(this.Sk.getBytes(), 0, keyEnc, 0, this.Sk.getBytes().length);
        System.arraycopy(xor12, 0, keyEnc, 0, xor12.length-1);
        Integer length = keyEnc.length;
        Log.d("Length",length.toString());
       // byte[] rx = new byte[32];
        Long TS = System.currentTimeMillis();
        String Data = this.T+","+ TS.toString();
        byte[] encryptedData = encrypt(keyEnc,Data.getBytes());
        String Z = hmacSha1(this.Sk+new String(xor12),this.T+new String(r1)+new String(r2_)+this.N+TS);

        //step 4
        byte[] decryptedData = decrypt(keyEnc,encryptedData);
        String Decrypted = new String(decryptedData);
        String TS_ = Decrypted.substring(Decrypted.length() - 13);
        if(!checkFreshnesh(Long.parseLong(TS_))){
            Log.d("ABBORT PROTOCOL","TIMESTAMP NOT FRESH");
        }
        String Z_ = hmacSha1(this.Sk+new String(xor12),this.T+new String(r1)+new String(r2_)+this.N+TS);
        if(Z.equals(Z_)){
            Log.d("BERHASIL",Z_);
        }else {
            Log.d("ABBORT PROTOCOL","FALSE Z");
        }
        Log.d("Protocol Suceed","YEAS, CHARGE");
    }


    public static byte[] xor(final byte[] input, final byte[] secret) {
        final byte[] output = new byte[input.length];
        if (secret.length == 0) {
            throw new IllegalArgumentException("empty security key");
        }
        int spos = 0;
        for (int pos = 0; pos < input.length; ++pos) {
            output[pos] = (byte) (input[pos] ^ secret[spos]);
            ++spos;
            if (spos >= secret.length) {
                spos = 0;
            }
        }
        return output;
    }
    public static String hmacSha1(String key, String value) {
        try {
            // Get an hmac_sha1 key from the raw key bytes
            byte[] keyBytes = key.getBytes();
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");

            // Get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);

            // Compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(value.getBytes());

            // Convert raw bytes to Hex
            //byte[] hexBytes = new Hex().encode(rawHmac);

            //  Covert array of Hex bytes to a String
            return new String(rawHmac, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encrypt(byte[] raw, byte[] clear) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(clear);
            return encrypted;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static byte[] decrypt(byte[] raw, byte[] encrypted) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return decrypted;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public Boolean checkFreshnesh(Long TS){
        Long Current = TS - System.currentTimeMillis();
        if(Math.abs(Current)<2000) {
            return true;
        }else {return  false;}
    }
}
