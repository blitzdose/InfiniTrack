package de.blitzdose.infinitrack.data.entities;

import de.blitzdose.infinitrack.data.AddressFormatter;
import org.json.JSONException;
import org.json.JSONObject;

public class BleDevice {
    private String address;
    private String name;
    private int rssi;
    private JSONObject payload;

    public BleDevice(String address, int rssi) {
        this.address = address;
        this.rssi = rssi;
    }

    public String getAddress() {
        return address;
    }

    public String getAddressFormatted() {
        return AddressFormatter.formatAddress(this.address);
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

    public void setNameHEX(String hex) {
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        this.name = output.toString();
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

    public void addPayload(JSONObject payload) {
        deepMerge(payload, this.payload);
    }

    private void deepMerge(JSONObject source, JSONObject target) throws JSONException {
        for (String key: JSONObject.getNames(source)) {
            Object value = source.get(key);
            if (!target.has(key)) {
                // new value for "key":
                target.put(key, value);
            } else {
                // existing value for "key" - recursively deep merge:
                if (value instanceof JSONObject valueJson) {
                    deepMerge(valueJson, target.getJSONObject(key));
                } else {
                    target.put(key, value);
                }
            }
        }
    }

    public void mergeFrom(BleDevice bleDevice) {
        if (this.getName() == null) {
            this.setName(bleDevice.getName());
        } else if (this.getName() != null && bleDevice.getName() != null) {
            this.setName(bleDevice.getName());
        }
        this.addPayload(bleDevice.getPayload());
        this.setRssi(bleDevice.getRssi());
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

    @Override
    public String toString() {
        return "Name: " + this.getName() + ", " +
                "Address: " + this.getAddress() + ", " +
                "rssi: " + this.getRssi() + ", " +
                "Payload: " + this.getPayload() + ", ";
    }
}
