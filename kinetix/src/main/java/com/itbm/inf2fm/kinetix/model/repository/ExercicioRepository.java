package com.itbm.inf2fm.kinetix.model.repository;
import com.itbm.inf2fm.kinetix.model.entity.Exercicio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ExercicioRepository extends JpaRepository<Exercicio, Long> {
    Optional<Exercicio> findByIdAndAtivoTrue(Long id);
}
