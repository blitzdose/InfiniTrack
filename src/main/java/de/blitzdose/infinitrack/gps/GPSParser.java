package de.blitzdose.infinitrack.gps;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class GPSParser {

    InputStream payloadStream;

    public GPSParser(InputStream payloadStream) {
        this.payloadStream = payloadStream;
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

    private short getShortFromBytes(byte[] src) {
        return allocateByteBuffer(src, 2).getShort();
    }

    private boolean getBooleanFromBytes(byte[] src) {
        return allocateByteBuffer(src, 1).get() == 0x01;
    }

    private ByteBuffer allocateByteBuffer(byte[] src, int size) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(size);
        byteBuffer.put(new byte[size-src.length]);
        byteBuffer.put(src);
        byteBuffer.rewind();
        return byteBuffer;
    }

    public long getTimestamp() throws IOException {
        byte[] timestampBytes = payloadStream.readNBytes(3);
        long payloadTimestamp = getLongFromBytes(timestampBytes);

        int hours = (int) (payloadTimestamp / 3600);
        payloadTimestamp = payloadTimestamp - (hours * 3600L);
        int minutes = (int) (payloadTimestamp / 60);
        payloadTimestamp = payloadTimestamp - (minutes * 60L);
        int seconds = (int) payloadTimestamp;

        byte[] yearBytes = payloadStream.readNBytes(1);
        byte[] monthBytes = payloadStream.readNBytes(1);
        byte[] dayBytes = payloadStream.readNBytes(1);

        int year = getIntFromBytes(yearBytes) + 2000;
        int month = getIntFromBytes(monthBytes);
        int day = getIntFromBytes(dayBytes);

        try {
            LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hours, minutes, seconds, LocalDateTime.now().getNano());
            return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        } catch (DateTimeException ignored) {
            return 0;
        }
    }

    public float getLatitude() throws IOException {
        return parseCoordinate();
    }

    public float getLongitude() throws IOException {
        return parseCoordinate();
    }

    private float parseCoordinate() throws IOException {
        byte[] coordinateBytes = payloadStream.readNBytes(4);
        byte[] isPositiveBytes = payloadStream.readNBytes(1);
        boolean isPositive = getBooleanFromBytes(isPositiveBytes);

        float coordinate = getFloatFromBytes(coordinateBytes);
        if (!isPositive) {
            coordinate = -coordinate;
        }
        return coordinate;
    }

    public float getSpeed() throws IOException {
        byte[] speedBytes = payloadStream.readNBytes(4);
        return getFloatFromBytes(speedBytes);
    }

    public float getCourse() throws IOException {
        byte[] courseBytes = payloadStream.readNBytes(4);
        return getFloatFromBytes(courseBytes);
    }

    public short getSatelliteCount() throws IOException {
        byte[] satellitesBytes = payloadStream.readNBytes(1);
        return getShortFromBytes(satellitesBytes);
    }

    public float getAltitude() throws IOException {
        byte[] altitudeBytes = payloadStream.readNBytes(4);
        return getFloatFromBytes(altitudeBytes);
    }

    public float getPdop() throws IOException {
        byte[] pdopBytes = payloadStream.readNBytes(4);
        return getFloatFromBytes(pdopBytes);
    }
}
