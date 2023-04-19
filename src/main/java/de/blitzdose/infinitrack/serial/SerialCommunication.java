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
import de.blitzdose.infinitrack.serial.SerialCommunicationListener.*;

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
                handleSerialEvent(serialPortEvent);
            }
        });
    }

    private void handleSerialEvent(SerialPortEvent serialPortEvent) {
        if (serialPortEvent.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
            handleSerialData();
        } else if (serialPortEvent.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
            closeConnection();
        }
    }

    private void handleSerialData() {
        readNewData();

        int newLineIndex = lastMsg.indexOf("\r\n");
        while (newLineIndex != -1) {
            String line = getLine(newLineIndex);
            fullConsole = String.format("%s%s", fullConsole, line);
            Message message = Message.parseMessage(line);

            notifyDataListener(message);

            if (message.type().equals(Message.TYPE_STATUS)) {
                if (message.msg().equals(Message.STATUS_READY_GLOBAL)) {
                    notifyConnectListeners();
                }
            } else if (message.type().equals(Message.TYPE_LORA_MSG)) {
                handleLoRaMessage(message);
            }
            newLineIndex = lastMsg.indexOf("\r\n");
        }
    }

    private void handleLoRaMessage(Message message) {
        try {
            JSONObject jsonMsg = new JSONObject(message.msg());
            Location location = Location.parsePayload(jsonMsg.getString("payload"));
            int rssi = jsonMsg.getInt("rssi");
            if (location != null) {
                LoRaHeaderParser loRaHeaderParser = new LoRaHeaderParser(jsonMsg.getString("header"));
                String sourceAddress = loRaHeaderParser.getSourceAddressFormatted();
                gpsUpdater.updateDevice(sourceAddress, location, rssi);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notifyConnectListeners() {
        connectListener.forEach(ConnectListener::connect);
        connected = true;
    }

    private void notifyDataListener(Message message) {
        if (dataListener != null) {
            dataListener.dataReceived(message, fullConsole);
        }
    }

    private String getLine(int newLineIndex) {
        String line = lastMsg.substring(0, newLineIndex);
        lastMsg = lastMsg.substring(newLineIndex +1);
        return line;
    }

    private void readNewData() {
        byte[] buffer = new byte[SerialCommunication.this.serialPort.bytesAvailable()];
        SerialCommunication.this.serialPort.readBytes(buffer, buffer.length);
        lastMsg += new String(buffer);
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
        serialPort.removeDataListener();
        fullConsole = "";
        lastMsg = "";
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
}
