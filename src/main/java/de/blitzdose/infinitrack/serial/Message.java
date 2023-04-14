package de.blitzdose.infinitrack.serial;

public class Message {

    public final static String TYPE_STATUS = "status";
    public final static String TYPE_LORA_MSG = "lora_msg";
    public final static String STATUS_READY_GLOBAL = "ready_global";

    private String type;
    private String msg;

    public Message(String type, String msg) {
        this.type = type;
        this.msg = msg;
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
