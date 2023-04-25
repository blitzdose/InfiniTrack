package de.blitzdose.infinitrack.data.services;

import de.blitzdose.infinitrack.data.entities.device.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class LocationService  {

    private final LocationRepository repository;

    public LocationService(LocationRepository repository) {
        this.repository = repository;
    }

    public Optional<Location> get(Long id) {
        return repository.findById(id);
    }

    public Location update(Location entity) {
        Location existingLocation = repository.findByAllAttributes(entity.getTimestamp(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getSpeed(),
                entity.getCourse(),
                entity.getSatelliteCount(),
                entity.getAltitude(),
                entity.getPdop());
        return repository.save(Objects.requireNonNullElse(existingLocation, entity));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<Location> list() {
        return repository.findAll();
    }

    public Page<Location> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<Location> list(Pageable pageable, Specification<Location> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}