package de.blitzdose.infinitrack.gps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HexFormat;

public class GPSParser {

    private int hours = 0;
    private int minutes = 0;
    private int seconds = 0;

    private int year = 0;
    private int month = 0;
    private int day = 0;

    private float latitude = 0F;
    private float longitude = 0f;

    private int satellites = 0;

    private float altitude = 0F;

    private float pdop = 0F;

    public GPSParser() {

    }

    // TODO: Direkt zu Location machen
    public void parsePayload(String payloadHex) throws IOException {
        payloadHex = "01517f17040a42420f300041111fcc0000064398800040e47ae1";
        byte[] payload = HexFormat.of().parseHex(payloadHex);

        ByteArrayInputStream payloadStream = new ByteArrayInputStream(payload);

        byte[] timestampBytes = payloadStream.readNBytes(3);
        long timestamp = getLongFromBytes(timestampBytes);

        this.hours = (int) (timestamp / 3600);
        timestamp = timestamp - (this.hours * 3600L);
        this.minutes = (int) (timestamp / 60);
        timestamp = timestamp - (this.minutes * 60L);
        this.seconds = (int) timestamp;

        byte[] yearBytes = payloadStream.readNBytes(1);
        byte[] monthBytes = payloadStream.readNBytes(1);
        byte[] dayBytes = payloadStream.readNBytes(1);

        this.year = getIntFromBytes(yearBytes) + 2000;
        this.month = getIntFromBytes(monthBytes);
        this.day = getIntFromBytes(dayBytes);

        byte[] latitudeBytes = payloadStream.readNBytes(4);
        byte[] isN = payloadStream.readNBytes(1);

        this.latitude = getFloatFromBytes(latitudeBytes);

        byte[] longitudeBytes = payloadStream.readNBytes(4);
        byte[] isE = payloadStream.readNBytes(1);

        this.longitude = getFloatFromBytes(longitudeBytes);

        byte[] satellitesBytes = payloadStream.readNBytes(1);
        this.satellites = getIntFromBytes(satellitesBytes);

        byte[] altitudeBytes = payloadStream.readNBytes(4);
        this.altitude = getFloatFromBytes(altitudeBytes);

        byte[] pdopBytes = payloadStream.readNBytes(4);
        this.pdop = getFloatFromBytes(pdopBytes);
    }

    private float getFloatFromBytes(byte[] src) {
        return allocateByteBuffer(src, 4).getFloat();
    }

    private int getIntFromBytes(byte[] src) {
        return allocateByteBuffer(src, 4).getInt();
    }

    private Long getLongFromBytes(byte[] src) {
        return allocateByteBuffer(src, 8).getLong();
    }

    private ByteBuffer allocateByteBuffer(byte[] src, int size) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        byteBuffer.put(new byte[size-src.length]);
        byteBuffer.put(src);
        byteBuffer.rewind();
        return byteBuffer;
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getSeconds() {
        return seconds;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public int getSatellites() {
        return satellites;
    }

    public float getAltitude() {
        return altitude;
    }

    public float getPdop() {
        return pdop;
    }
}
