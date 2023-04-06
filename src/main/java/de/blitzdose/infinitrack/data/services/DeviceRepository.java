package de.blitzdose.infinitrack.data.services;

import de.blitzdose.infinitrack.data.entities.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeviceRepository extends
        JpaRepository<Device, Long>,
        JpaSpecificationExecutor<Device> {
}
