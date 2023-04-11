package de.blitzdose.infinitrack.serial;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortDataListenerWithExceptions;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.vaadin.flow.spring.annotation.SpringComponent;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.context.annotation.ApplicationScope;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ApplicationScope
@SpringComponent
public class SerialCommunication {
    private SerialPort serialPort = null;
    public DataListener dataListener = null;
    public List<ConnectListener> connectListener = new ArrayList<>();
    public List<DisconnectListener> disconnectListener = new ArrayList<>();

    private String lastMsg = "";
    private String fullConsole = "";

    public final static String TYPE_STATUS = "status";
    public final static String MSG_GET_READY = "get_ready";
    public final static String MSG_START_SCAN = "ble_start_scan";
    public final static String MSG_STOP_SCAN = "ble_stop_scan";
    public final static String TYPE_SCAN_RESULT = "ble_scan_result";
    public final static String MSG_SCAN_STARTED = "ble_scan_started";
    public final static String MSG_SCAN_STOPPED = "ble_scan_stopped";
    public static final String MSG_BLE_CONNECT = "ble_connect:%s";
    public static final String MSG_BLE_DATA_SEND = "ble_data_send";

    public final static String BLE_SIGNATURE = "4954496e66696e69747261636b4d6f64";

    public SerialCommunication() {

    }

    public void initialize(SerialPort serialPort, int baudRate) {
        this.serialPort = serialPort;
        this.serialPort.setBaudRate(baudRate);
        this.serialPort.addDataListener(new SerialPortDataListenerWithExceptions() {
            @Override
            public void catchException(Exception e) {
                e.printStackTrace();
            }

            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
            }

            @Override
            public void serialEvent(SerialPortEvent serialPortEvent) {
                if (serialPortEvent.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    byte[] buffer = new byte[SerialCommunication.this.serialPort.bytesAvailable()];
                    SerialCommunication.this.serialPort.readBytes(buffer, buffer.length);
                    lastMsg += new String(buffer);

                    int newLineIndex;
                    do {
                        newLineIndex = lastMsg.indexOf("\r\n");

                        if (newLineIndex != -1) {
                            String line = lastMsg.substring(0, newLineIndex);
                            fullConsole = String.format("%s%s", fullConsole, line);
                            if (dataListener != null) {
                                JSONObject jsonObject = new JSONObject("{\"type\": \"unknown\", \"msg\": \"\"}");
                                try {
                                    jsonObject = new JSONObject(line);
                                } catch (JSONException ignored) { }
                                dataListener.dataReceived(jsonObject, fullConsole);
                            }
                            lastMsg = lastMsg.substring(newLineIndex+1);

                            Message message = SerialParser.parseMessage(line);
                            if (message != null &&
                                    message.getType().equals(Message.TYPE_STATUS)
                                    && message.getMsg().equals(Message.STATUS_READY_GLOBAL)) {
                                connectListener.forEach(ConnectListener::connect);
                            }
                        }
                    } while (newLineIndex != -1);
                } else if (serialPortEvent.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
                    closeConnection();
                }
            }
        });
    }

    public boolean isInitialized() {
        return serialPort != null;
    }

    public SerialPort getSerialPort() {
        return serialPort;
    }

    public void openConnection() {
        if (serialPort.openPort()) {
            sendMessage(MSG_GET_READY);
        }
    }

    public boolean isOpen() {
        if (serialPort == null) {
            return false;
        }
        return serialPort.isOpen();
    }

    public void closeConnection() {
        SerialCommunication.this.serialPort.removeDataListener();
        SerialCommunication.this.fullConsole = "";
        SerialCommunication.this.lastMsg = "";
        if (disconnectListener != null) {
            disconnectListener.forEach(DisconnectListener::disconnect);
        }
        serialPort.closePort();
        serialPort = null;
    }

    public String getConsole() {
        return fullConsole;
    }

    public boolean sendMessage(String msg) {
        if (serialPort == null || !serialPort.isOpen()) {
            return false;
        }
        msg = String.format("%s\r\n", msg);
        byte[] msgBytes = msg.getBytes(Charset.defaultCharset());
        int bytesWritten = serialPort.writeBytes(msgBytes, msgBytes.length);
        return bytesWritten != -1;
    }

    public void setOnDataListener(DataListener dataListener) {
        this.dataListener = dataListener;
    }

    public void addOnConnectListener(ConnectListener connectListener) {
        this.connectListener.add(connectListener);
    }

    public void addOnDisconnectListener(DisconnectListener disconnectListener) {
        this.disconnectListener.add(disconnectListener);
    }

    @ApplicationScope
    @SpringComponent
    public interface DataListener {
        void dataReceived(JSONObject msg, String console);
    }

    @ApplicationScope
    @SpringComponent
    public interface DisconnectListener {
        void disconnect();
    }

    @ApplicationScope
    @SpringComponent
    public interface ConnectListener {
        void connect();
    }

}
