package de.blitzdose.infinitrack.gps;

import com.vaadin.flow.spring.annotation.SpringComponent;
import de.blitzdose.infinitrack.data.entities.device.Device;
import de.blitzdose.infinitrack.data.entities.device.Location;
import de.blitzdose.infinitrack.data.services.DeviceService;
import de.blitzdose.infinitrack.data.services.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.ApplicationScope;

import javax.transaction.Transactional;
import java.util.*;

@ApplicationScope
@SpringComponent
public class GPSUpdater {

    private final DeviceService deviceService;
    private final LocationService locationService;

    private boolean recording = false;

    private final Map<Long, Long> lastUpdate = new HashMap<>();

    public GPSUpdater(@Autowired DeviceService deviceService, @Autowired LocationService locationService) {
        this.deviceService = deviceService;
        this.locationService = locationService;
        this.startDeviceStatusUpdater();
    }

    @Transactional
    public void updateDevice(String address, Location location, int rssi) {
        Optional<Device> deviceOptional = deviceService.getByAddress(address);
        if (deviceOptional.isPresent()) {
            Device device = deviceOptional.get();
            device.setChargeLevel(location.getChargeLevel());
            device.setSignal(rssi);
            device.setStatus("Connected");

            if ((location.getLatitude() != 0 || location.getLongitude() != 0)) {
                Location location1 = locationService.update(location);
                if (recording) {
                    device.addToLocationHistory(location1);
                    SAPHandler.sendLocationData(device.getSapUUID(), location1);
                } else {
                    device.setLastLocation(location1);
                }
                if (location.getLatitude() != 0 || location.getLongitude() != 0) {
                    device.setStatusGPS("Connected");
                }
            }
            deviceService.update(device);
            lastUpdate.put(device.getId(), System.currentTimeMillis());
        }
    }

    private void startDeviceStatusUpdater() {
        new Timer().schedule(new TimerTask() {
            @Override
            @Transactional
            public void run() {
                List<Device> deviceList = deviceService.list();
                deviceList.stream()
                        .filter(device -> !lastUpdate.containsKey(device.getId()) || lastUpdate.get(device.getId()) + 30000 < System.currentTimeMillis())
                        .forEach(device -> {
                            device.setStatus("Offline");
                            device.setStatusGPS("Offline");
                            deviceService.update(device);
                        });
            }
        }, 0, 5000);
    }

    public boolean isRecording() {
        return recording;
    }

    public void setRecording(boolean recording) {
        if (recording) {
            deviceService.listWithLocations().forEach(device -> {
                device.clearLocationHistory();
                deviceService.update(device);
            });
        }
        this.recording = recording;
    }
}
