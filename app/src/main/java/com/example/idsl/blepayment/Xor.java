package com.example.idsl.blepayment;

import java.io.IOException;

/**
 * Created by IDSL on 2016/12/1.
 */

public class Xor {


    private static String encryptDecrypt(String input) {
        char[] key = {'K', 'C', 'Q'}; //Can be any chars, and any length array
        StringBuilder output = new StringBuilder();

        for(int i = 0; i < input.length(); i++) {
            output.append((char) (input.charAt(i) ^ key[i % key.length]));
        }

        return output.toString();
    }

    public static void main(String[] args) {
        String encrypted = Xor.encryptDecrypt("kylewbanks.com");
        System.out.println("Encrypted:" + encrypted);

        String decrypted = Xor.encryptDecrypt(encrypted);
        System.out.println("Decrypted:" + decrypted);
    }




}
