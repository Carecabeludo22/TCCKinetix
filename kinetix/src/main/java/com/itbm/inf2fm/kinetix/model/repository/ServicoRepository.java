package com.itbm.inf2fm.kinetix.model.repository;
import com.itbm.inf2fm.kinetix.model.entity.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ServicoRepository extends JpaRepository<Servico, Long> {
    List<Servico> findByFisioterapeuta_Usuario_IdAndAtivoTrueOrderByNome(Long usuarioId);
    Optional<Servico> findByIdAndFisioterapeuta_Usuario_IdAndAtivoTrue(Long id, Long usuarioId);
}
