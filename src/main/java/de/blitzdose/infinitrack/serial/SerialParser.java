package de.blitzdose.infinitrack.serial;

import elemental.json.JsonObject;
import org.json.JSONException;
import org.json.JSONObject;

public class SerialParser {

    static Message parseMessage(String msg) {
        try {
            JSONObject jsonObject = new JSONObject(msg);
            return new Message(jsonObject.getString("type"), jsonObject.getString("msg"));
        } catch (JSONException ignored) {
            return null;
        }
    }
}