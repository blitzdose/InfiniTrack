package de.blitzdose.infinitrack.data;

import java.util.Locale;

public class AddressFormatter {

    public static String formatAddress(String address) {
        StringBuilder builder = new StringBuilder();
        char[] charArray = address.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (i != 0 && i % 2 == 0) {
                builder.append(":");
            }
            builder.append(c);
        }
        return builder.toString().toUpperCase(Locale.ROOT);
    }
}
