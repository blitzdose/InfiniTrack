package de.blitzdose.infinitrack.gps;

import de.blitzdose.infinitrack.data.entities.device.Location;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SAPHandler {
    public static void sendLocationData(String SAPDeviceID, Location location) {
        if (SAPDeviceID == null) {
            return;
        }
        long timestamp = System.currentTimeMillis();
        float latitude = location.getLatitude();
        float longitude = location.getLongitude();
        float speed = location.getSpeed();
        float course = location.getCourse();

        JSONObject mainObject = new JSONObject();
        mainObject.put("deviceUuid", SAPDeviceID);

        JSONObject gpsFix = new JSONObject();
        gpsFix.put("timestamp", timestamp);
        gpsFix.put("latitude", latitude);
        gpsFix.put("longitude", longitude);
        gpsFix.put("speed", speed);
        gpsFix.put("course", course);

        JSONArray jsonArray = new JSONArray();
        jsonArray.put(gpsFix);

        mainObject.put("fixes", jsonArray);

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://my.sapsailing.com/sailingserver/api/v1/gps_fixes"))
                .POST(HttpRequest.BodyPublishers.ofString(mainObject.toString()))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }
}
