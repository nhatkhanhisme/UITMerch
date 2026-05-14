package com.uitmerch.backend.merch.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "merch_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchImage {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "merch_id", nullable = false, updatable = false)
    private UUID merchId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false)
    private int position;
}
