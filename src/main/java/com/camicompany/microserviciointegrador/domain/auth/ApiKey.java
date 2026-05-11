package com.camicompany.microserviciointegrador.domain.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "api_keys")
public class ApiKey {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "key_prefix", unique = true, nullable = false)
  private String keyPrefix;

  @Column(name = "key_hash", nullable = false)
  private String keyHash;

  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(name = "created_at")
  @CreationTimestamp
  private LocalDateTime createdAt;

  @ManyToOne
  @JoinColumn(name = "user_id", nullable = false)
  private User user;
}
