package com.camicompany.microserviciointegrador.repository;

import com.camicompany.microserviciointegrador.domain.auth.ApiKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

  Optional<ApiKey> findByKeyPrefix(String prefix);
}
