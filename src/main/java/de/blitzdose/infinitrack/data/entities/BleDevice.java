package de.blitzdose.infinitrack.data.entities;

import org.json.JSONObject;

public class BleDevice {
    private String address;
    private String name;
    private int rssi;
    private JSONObject payload;

    public BleDevice(String address, String name, int rssi) {
        this.address = address;
        this.name = name;
        this.rssi = rssi;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public JSONObject getPayload() {
        return payload;
    }

    public void setPayload(JSONObject payload) {
        this.payload = payload;
    }

    @Override
    public int hashCode() {
        return this.getAddress().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BleDevice device) {
            return device.getAddress().equals(this.getAddress());
        }
        return false;
    }
}
