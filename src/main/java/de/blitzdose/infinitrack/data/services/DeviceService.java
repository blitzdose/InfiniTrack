package de.blitzdose.infinitrack.data.services;

import de.blitzdose.infinitrack.data.entities.device.Device;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

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