package de.blitzdose.infinitrack.data.services;

import de.blitzdose.infinitrack.data.entities.device.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface LocationRepository extends
        JpaRepository<Location, Long>,
        JpaSpecificationExecutor<Location> {
    @Query("""
            select l from Location l
            where l.timestamp = ?1 and l.latitude = ?2 and l.longitude = ?3 and l.speed = ?4 and l.satelliteCount = ?5 and l.altitude = ?6 and l.pdop = ?7""")
    Location findByAllAttributes(long timestamp, float latitude, float longitude, float speed, short satelliteCount, float altitude, float pdop);
}
