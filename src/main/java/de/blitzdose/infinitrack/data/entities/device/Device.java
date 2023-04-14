package de.blitzdose.infinitrack.data.entities.device;

import de.blitzdose.infinitrack.data.entities.AbstractEntity;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Device extends AbstractEntity {

    private String address;
    private String color;
    private String name;
    private double signal;
    private String status;
    @OneToMany
    private List<Location> locationHistory = new ArrayList<>();
    @OneToOne
    private Location lastLocation;

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

    public List<Location> getLocationHistory() {
        return this.locationHistory;
    }

    public void addToLocationHistory(Location location) {
        if (!this.locationHistory.contains(location)) {
            this.locationHistory.add(location);
        }
        this.lastLocation = location;
    }

    public void clearLocationHistory() {
        this.locationHistory.clear();
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
    }
}
