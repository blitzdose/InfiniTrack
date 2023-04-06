package de.blitzdose.infinitrack.data.entities;

import com.vaadin.flow.component.map.configuration.Coordinate;

import javax.persistence.Entity;
import java.util.Locale;

@Entity
public class Device extends AbstractEntity{

    private String address;
    private String color;
    private String name;
    private double signal;
    private String status;
    private String firmwareVersion;
    private Coordinate coordinate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSignal() {
        return signal;
    }

    public void setSignal(double signal) {
        this.signal = signal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public String getCoordinateAsString() {
        double x = coordinate.getX();
        double y = coordinate.getY();

        return String.format(Locale.ROOT, "%f, %f", y, x);
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
