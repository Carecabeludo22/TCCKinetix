package com.itbm.inf2fm.kinetix.model.repository;
import com.itbm.inf2fm.kinetix.model.entity.Assinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AssinaturaRepository extends JpaRepository<Assinatura,Long> {
    List<Assinatura> findByFisioterapeuta_Usuario_IdOrderByInicioDesc(Long id);
}
