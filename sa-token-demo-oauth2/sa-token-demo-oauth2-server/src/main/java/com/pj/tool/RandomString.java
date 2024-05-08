package com.pj.tool;

import java.util.Random;

public class RandomString {

    // Generate a random string with a random length between 6 and 10
    public static String generateRandomString() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_";
        Random random = new Random();
        int length = random.nextInt(5) + 6; // Generate a random length between 6 and 10
        StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            result.append(characters.charAt(index));
        }

        return result.toString();
    }
}

