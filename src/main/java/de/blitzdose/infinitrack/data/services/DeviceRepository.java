package de.blitzdose.infinitrack.data.services;

import de.blitzdose.infinitrack.data.entities.device.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface DeviceRepository extends
        JpaRepository<Device, Long>,
        JpaSpecificationExecutor<Device> {
    Optional<Device> findByAddressIgnoreCase(String address);
}
