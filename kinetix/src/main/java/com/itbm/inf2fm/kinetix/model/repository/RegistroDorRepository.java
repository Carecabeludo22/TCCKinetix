package com.itbm.inf2fm.kinetix.model.repository;
import com.itbm.inf2fm.kinetix.model.entity.RegistroDor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RegistroDorRepository extends JpaRepository<RegistroDor, Long> {
    List<RegistroDor> findByPaciente_Usuario_IdOrderByRegistradoEmDesc(Long pacienteId);
}
