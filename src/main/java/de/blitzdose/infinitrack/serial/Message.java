package de.blitzdose.infinitrack.serial;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {

    public final static String TYPE_UNKNOWN = "unknown";
    public final static String TYPE_STATUS = "status";
    public final static String TYPE_LORA_MSG = "lora_msg";
    public final static String TYPE_SCAN_RESULT = "ble_scan_result";
    public final static String STATUS_READY_GLOBAL = "ready_global";
    public final static String MSG_GET_READY = "get_ready";
    public final static String MSG_START_SCAN = "ble_start_scan";
    public final static String MSG_STOP_SCAN = "ble_stop_scan";
    public final static String MSG_SCAN_STARTED = "ble_scan_started";
    public final static String MSG_SCAN_STOPPED = "ble_scan_stopped";
    public static final String MSG_BLE_CONNECT = "ble_connect:%s";
    public static final String MSG_BLE_DATA_SEND = "ble_data_send";

    public static final String MSG_UNKNOWN_ERROR = "unknown_error";

    public final static String BLE_SIGNATURE = "4954496e66696e69747261636b4d6f64";

    private String type;
    private String msg;

    public Message(String type, String msg) {
        this.type = type;
        this.msg = msg;
    }

    public static Message parseMessage(String msg) {
        try {
            JSONObject jsonObject = new JSONObject(msg);
            try {
                JSONObject messageJson = jsonObject.getJSONObject("msg");
                return new Message(jsonObject.getString("type"), messageJson.toString());
            } catch (JSONException ignored) {
                return new Message(jsonObject.getString("type"), jsonObject.getString("msg"));
            }
        } catch (JSONException ignored) {
            return new Message(Message.TYPE_UNKNOWN, "");
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
