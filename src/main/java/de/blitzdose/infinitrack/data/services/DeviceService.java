package de.blitzdose.infinitrack.data.services;

import de.blitzdose.infinitrack.data.entities.device.Device;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DeviceService {

    private final DeviceRepository repository;

    public DeviceService(DeviceRepository repository) {
        this.repository = repository;
    }

    public Optional<Device> get(Long id) {
        return repository.findById(id);
    }

    public Optional<Device> getByAddress(String address) {
        return repository.findByAddressIgnoreCase(address);
    }

    public Device update(Device entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<Device> list() {
        return repository.findAll();
    }

    @Transactional(propagation=Propagation.REQUIRED, readOnly=true, noRollbackFor=Exception.class)
    public List<Device> listWithLocations() {
        List<Device> devices = repository.findAll();
        devices.forEach(device -> Hibernate.initialize(device.getLocationHistory()));
        return devices;
    }

    public Page<Device> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Device> list(Pageable pageable, Specification<Device> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}