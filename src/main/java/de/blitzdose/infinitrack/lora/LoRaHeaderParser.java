package de.blitzdose.infinitrack.lora;

import de.blitzdose.infinitrack.data.AddressFormatter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HexFormat;

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
        return AddressFormatter.formatAddress(HexFormat.of().formatHex(address));
    }
}
