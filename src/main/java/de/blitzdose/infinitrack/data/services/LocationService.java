package de.blitzdose.infinitrack.data.services;

import de.blitzdose.infinitrack.data.entities.device.Device;
import de.blitzdose.infinitrack.data.entities.device.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.List;
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
        Location existingLocation = repository.findByTimestampAndLatitudeAndLongitudeAndSpeedAndSatelliteCountAndAltitudeAndPdop(entity.getTimestamp(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getSpeed(),
                entity.getSatelliteCount(),
                entity.getAltitude(),
                entity.getPdop());
        if (existingLocation != null) {
            return repository.save(existingLocation);
        }
        return repository.save(entity);
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