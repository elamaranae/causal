package com.causal.orders.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "order_addresses")
@Getter
@Setter
public class OrderAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String label;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String country;
    private String pincode;
    private String phoneNumber;

    @CreationTimestamp
    private Instant createdAt;
}
