package com.itbm.inf2fm.kinetix.model.repository;
import com.itbm.inf2fm.kinetix.model.entity.Especialidade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
public interface EspecialidadeRepository extends JpaRepository<Especialidade, Long> {
    List<Especialidade> findByIdInAndAtivaTrue(Collection<Long> ids);\n    List<Especialidade> findByAtivaTrueOrderByNome();\n    boolean existsByNomeIgnoreCase(String nome);
}
