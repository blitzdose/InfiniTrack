package de.blitzdose.infinitrack.lora;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HexFormat;
import java.util.Locale;

public class LoRaHeaderParser {

    private final String signature;
    private final byte[] destinationAddress;
    private final byte[] sourceAddress;

    public LoRaHeaderParser(String payloadHex) throws IOException {
        byte[] payload = HexFormat.of().parseHex(payloadHex);
        ByteArrayInputStream payloadStream = new ByteArrayInputStream(payload);

        byte[] signatureBytes = payloadStream.readNBytes(4);
        signature = new String(signatureBytes);

        destinationAddress = payloadStream.readNBytes(6);

        sourceAddress = payloadStream.readNBytes(6);
    }

    public String getSignature() {
        return signature;
    }

    public byte[] getDestinationAddress() {
        return destinationAddress;
    }

    public byte[] getSourceAddress() {
        return sourceAddress;
    }

    public String getDestinationAddressFormatted() {
        return getAddressFormatted(this.destinationAddress);
    }

    public String getSourceAddressFormatted() {
        return getAddressFormatted(this.sourceAddress);
    }

    private String getAddressFormatted(byte[] address) {
        StringBuilder builder = new StringBuilder();
        char[] charArray = HexFormat.of().formatHex(address).toCharArray();
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
