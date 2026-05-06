package com.camicompany.microserviciointegrador.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referencia_externa", nullable = false, unique = true)
    private String referenciaExterna;

    @Column(name = "id_sp")
    private String idSp;

    @Column(nullable = false)
    private Long importe;

    private String descripcion;

    @Column(name = "fecha_vto", nullable = false)
    private LocalDate fechaVto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_interno", nullable = false)
    private PaymentStatus estadoInterno;

    @Column(name = "estado_externo")
    private String estadoExterno;

    @Column(name = "checkout_url")
    private String checkoutUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

}
