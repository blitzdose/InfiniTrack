package de.blitzdose.infinitrack.data.services;

import de.blitzdose.infinitrack.data.entities.device.Device;
import de.blitzdose.infinitrack.data.entities.device.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface LocationRepository extends
        JpaRepository<Location, Long>,
        JpaSpecificationExecutor<Location> {
    Location findByTimestampAndLatitudeAndLongitudeAndSpeedAndSatelliteCountAndAltitudeAndPdop(long timestamp, float latitude, float longitude, float speed, short satelliteCount, float altitude, float pdop);
}
