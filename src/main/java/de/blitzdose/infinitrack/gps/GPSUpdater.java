package de.blitzdose.infinitrack.gps;

import com.vaadin.flow.spring.annotation.SpringComponent;
import de.blitzdose.infinitrack.data.entities.device.Device;
import de.blitzdose.infinitrack.data.entities.device.Location;
import de.blitzdose.infinitrack.data.services.DeviceService;
import de.blitzdose.infinitrack.data.services.LocationRepository;
import de.blitzdose.infinitrack.data.services.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.annotation.ApplicationScope;

import javax.transaction.Transactional;
import java.util.Optional;

@ApplicationScope
@SpringComponent
public class GPSUpdater {

    private final DeviceService deviceService;
    private final LocationService locationService;

    private boolean recording = false;

    public GPSUpdater(@Autowired DeviceService deviceService, @Autowired LocationService locationService) {
        this.deviceService = deviceService;
        this.locationService = locationService;
    }

    @Transactional
    public void updateDevice(String address, Location location, int rssi) {
        if ((location.getLatitude() == 0 && location.getLongitude() == 0)) {
            return;
        }
        Optional<Device> deviceOptional = deviceService.getByAddress(address);
        if (deviceOptional.isPresent()) {
            Device device = deviceOptional.get();
            Location location1 = locationService.update(location);
            System.out.println("loc: " + location1);
            if (recording) {
                device.addToLocationHistory(location1);
            } else {
                device.setLastLocation(location1);
            }
            device.setSignal(rssi);
            deviceService.update(device);
        }
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
