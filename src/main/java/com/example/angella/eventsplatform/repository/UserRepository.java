package com.example.angella.eventsplatform.repository;

import com.example.angella.eventsplatform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsernameOrEmail(String username, String email);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.createdEvents LEFT JOIN FETCH u.events")
    List<User> findAllWithEvents();

    @Modifying
    @Transactional
    @Query("DELETE FROM User u WHERE u.id = :userId")
    void deleteUserCascade(@Param("userId") Long userId);
}
