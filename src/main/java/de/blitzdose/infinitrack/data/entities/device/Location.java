package de.blitzdose.infinitrack.data.entities.device;

import de.blitzdose.infinitrack.data.entities.AbstractEntity;
import de.blitzdose.infinitrack.gps.GPSParser;

import javax.persistence.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.HexFormat;

@Entity
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    @Version
    private int version;
    private long timestamp;
    private float latitude;
    private float longitude;
    private float speed;
    private short satelliteCount;
    private float altitude;
    private float pdop;

    public static Location parsePayload(String payloadHex) throws IOException {
        byte[] payload = HexFormat.of().parseHex(payloadHex);
        if (payload.length <= 2) {
            return null;
        }
        ByteArrayInputStream payloadStream = new ByteArrayInputStream(payload);

        GPSParser gpsParser = new GPSParser(payloadStream);

        Location location = new Location();
        location.setTimestamp(gpsParser.getTimestamp());
        location.setLatitude(gpsParser.getLatitude());
        location.setLongitude(gpsParser.getLongitude());
        location.setSpeed(gpsParser.getSpeed());
        location.setSatelliteCount(gpsParser.getSatelliteCount());
        location.setAltitude(gpsParser.getAltitude());
        location.setPdop(gpsParser.getPdop());

        return location;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public short getSatelliteCount() {
        return satelliteCount;
    }

    public void setSatelliteCount(short satelliteCount) {
        this.satelliteCount = satelliteCount;
    }

    public float getAltitude() {
        return altitude;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public float getPdop() {
        return pdop;
    }

    public void setPdop(float pdop) {
        this.pdop = pdop;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long generateId() {
        long generatedId = 0;
        generatedId += timestamp;
        generatedId += latitude * 100000;
        generatedId += longitude * 100000;
        generatedId += speed * 100000000;
        generatedId += satelliteCount;
        generatedId += altitude * 100;
        generatedId += pdop * 1000;
        return generatedId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Timestamp (UTC): " + Instant.ofEpochMilli(timestamp).toString() +
                ", Latitude: " + latitude +
                ", Longitude: " + longitude +
                ", Speed: " + speed +
                ", Satellites: " + satelliteCount +
                ", Altitude: " + altitude +
                ", PDOP: " + pdop;
    }

    @Override
    public int hashCode() {
        return (String.valueOf(timestamp) + latitude + longitude + speed + altitude + satelliteCount + pdop).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Location other)) {
            return false;
        }

        return timestamp == other.timestamp &&
                latitude == other.latitude &&
                longitude == other.longitude &&
                speed == other.speed &&
                satelliteCount == other.satelliteCount &&
                altitude == other.altitude &&
                pdop == other.pdop;
    }
}
