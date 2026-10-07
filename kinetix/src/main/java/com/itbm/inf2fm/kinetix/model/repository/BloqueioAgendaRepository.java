package com.itbm.inf2fm.kinetix.model.repository;
import com.itbm.inf2fm.kinetix.model.entity.BloqueioAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
public interface BloqueioAgendaRepository extends JpaRepository<BloqueioAgenda, Long> {
    boolean existsByFisioterapeuta_Usuario_IdAndInicioLessThanAndFimGreaterThan(Long id,
        OffsetDateTime fim, OffsetDateTime inicio);
}
