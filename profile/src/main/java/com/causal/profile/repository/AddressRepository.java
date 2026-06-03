package com.causal.profile.repository;

import com.causal.profile.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    @Query(value = "SELECT delete_address_if_not_default(:addressId, :userId)", nativeQuery = true)
    int deleteIfNotDefault(Long addressId, Long userId);
}
