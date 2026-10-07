package com.itbm.inf2fm.kinetix.model.repository;
import com.itbm.inf2fm.kinetix.model.entity.PlanoAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PlanoAssinaturaRepository extends JpaRepository<PlanoAssinatura, Long> {
    List<PlanoAssinatura> findByAtivoTrueOrderByPrecoMensal();
}
