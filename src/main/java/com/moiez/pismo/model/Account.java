package com.moiez.pismo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String documentNumber;

    @Column(nullable = false, precision = 12, scale = SCALE)
    @PositiveOrZero
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

    @Column(nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP(6)")
    @CreationTimestamp
    private Instant createdAt;

    @Column(nullable = false,
            columnDefinition = "TIMESTAMP(6)")
    @UpdateTimestamp
    private Instant updatedAt;
}