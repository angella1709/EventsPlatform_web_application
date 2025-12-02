package com.example.angella.eventsplatform.repository;

import com.example.angella.eventsplatform.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByCityAndStreet(String city, String street);

}
