package com.itbm.inf2fm.kinetix.model.repository;
import com.itbm.inf2fm.kinetix.model.entity.DisponibilidadeSemanal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalTime;
public interface DisponibilidadeRepository extends JpaRepository<DisponibilidadeSemanal, Long> {
    List<DisponibilidadeSemanal> findByFisioterapeuta_Usuario_IdAndDiaSemanaAndAtivaTrue(Long id, int diaSemana);
    List<DisponibilidadeSemanal> findByFisioterapeuta_Usuario_IdAndAtivaTrueOrderByDiaSemanaAscHoraInicioAsc(Long id);
    boolean existsByFisioterapeuta_Usuario_IdAndDiaSemanaAndHoraInicioLessThanAndHoraFimGreaterThanAndAtivaTrue(
        Long id, int diaSemana, LocalTime fim, LocalTime inicio);
}
