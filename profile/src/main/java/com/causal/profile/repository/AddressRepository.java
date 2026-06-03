package com.causal.profile.repository;

import com.causal.profile.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM Address a WHERE a.id = :id AND a.userId = :userId " +
            "AND a.id NOT IN (SELECT p.defaultAddressId FROM Profile p WHERE p.userId = :userId AND p.defaultAddressId IS NOT NULL)")
    int deleteIfNotDefault(Long id, Long userId);
}
