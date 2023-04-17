package de.blitzdose.infinitrack.serial;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListenerWithExceptions;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.vaadin.flow.spring.annotation.SpringComponent;
import de.blitzdose.infinitrack.data.entities.device.Location;
import de.blitzdose.infinitrack.gps.GPSUpdater;
import de.blitzdose.infinitrack.lora.LoRaHeaderParser;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.ApplicationScope;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@ApplicationScope
@SpringComponent
public class SerialCommunication {
    private SerialPort serialPort = null;
    public DataListener dataListener = null;
    public List<ConnectListener> connectListener = new ArrayList<>();
    public List<DisconnectListener> disconnectListener = new ArrayList<>();

    private String lastMsg = "";
    private String fullConsole = "";

    private final GPSUpdater gpsUpdater;

    private boolean connected = false;

    public SerialCommunication(@Autowired GPSUpdater gpsUpdater) {
        this.gpsUpdater = gpsUpdater;
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

                            Message message = Message.parseMessage(line);

                            if (dataListener != null) {
                                dataListener.dataReceived(message, fullConsole);
                            }
                            lastMsg = lastMsg.substring(newLineIndex+1);

                            if (message.getType().equals(Message.TYPE_STATUS)) {
                                if (message.getMsg().equals(Message.STATUS_READY_GLOBAL)) {
                                    connectListener.forEach(ConnectListener::connect);
                                    connected = true;
                                }
                            } else if (message.getType().equals(Message.TYPE_LORA_MSG)) {
                                try {
                                    JSONObject jsonObject = new JSONObject(message.getMsg());
                                    Location location = Location.parsePayload(jsonObject.getString("payload"));
                                    int rssi = jsonObject.getInt("rssi");
                                    if (location != null) {
                                        LoRaHeaderParser loRaHeaderParser = new LoRaHeaderParser(jsonObject.getString("header"));
                                        String sourceAddress = loRaHeaderParser.getSourceAddressFormatted();

                                        gpsUpdater.updateDevice(sourceAddress, location, rssi);
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();

                                }
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
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (connected) {
                        this.cancel();
                    }
                    sendMessage(Message.MSG_GET_READY);
                }
            }, 0, 2000);
        }
    }

    public boolean isOpen() {
        if (serialPort == null) {
            return false;
        }
        return serialPort.isOpen();
    }

    public boolean isConnected() {
        return connected && isOpen();
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
        connected = false;
    }

    public String getConsole() {
        return fullConsole;
    }

    public void sendMessage(String msg) {
        if (serialPort == null || !serialPort.isOpen()) {
            return;
        }
        msg = String.format("%s\r\n", msg);
        byte[] msgBytes = msg.getBytes(Charset.defaultCharset());
        serialPort.writeBytes(msgBytes, msgBytes.length);
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
        void dataReceived(Message msg, String console);
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
