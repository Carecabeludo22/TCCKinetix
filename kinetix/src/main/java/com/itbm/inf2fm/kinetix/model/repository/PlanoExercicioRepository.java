package com.itbm.inf2fm.kinetix.model.repository;
import com.itbm.inf2fm.kinetix.model.entity.PlanoExercicio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PlanoExercicioRepository extends JpaRepository<PlanoExercicio, Long> {
    List<PlanoExercicio> findByPaciente_Usuario_IdAndAtivoTrueOrderByInicioDesc(Long pacienteId);
    java.util.Optional<PlanoExercicio> findByIdAndPaciente_Usuario_IdAndAtivoTrue(Long id, Long pacienteId);
    List<PlanoExercicio> findByPaciente_Usuario_IdAndFisioterapeuta_Usuario_IdOrderByInicioDesc(Long pacienteId, Long fisioId);
}
