package com.camicompany.microserviciointegrador.repository;

import com.camicompany.microserviciointegrador.domain.payment.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByReferenciaExterna(String referenciaExterna);

  Optional<Payment> findByIdSp(String idSp);
}
