package de.blitzdose.infinitrack.data.entities.device;

import de.blitzdose.infinitrack.data.entities.AbstractEntity;

import javax.persistence.Entity;

@Entity
public class Location extends AbstractEntity {
    private long timestamp;
    private int satelliteCount;
    private double longitude;
    private double latitude;
    private double altitude;
    private double speed;
    private double pdop;

    public long getTimestamp() {
        return timestamp;
    }

    public Location setTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public double getLongitude() {
        return longitude;
    }

    public Location setLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    public double getLatitude() {
        return latitude;
    }

    public Location setLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public int getSatelliteCount() {
        return satelliteCount;
    }

    public Location setSatelliteCount(int satelliteCount) {
        this.satelliteCount = satelliteCount;
        return this;
    }

    public double getAltitude() {
        return altitude;
    }

    public Location setAltitude(double altitude) {
        this.altitude = altitude;
        return this;
    }

    public double getSpeed() {
        return speed;
    }

    public Location setSpeed(double speed) {
        this.speed = speed;
        return this;
    }

    public double getPdop() {
        return pdop;
    }

    public Location setPdop(double pdop) {
        this.pdop = pdop;
        return this;
    }
}
