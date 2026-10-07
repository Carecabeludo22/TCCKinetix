package com.itbm.inf2fm.kinetix.model.repository;
import com.itbm.inf2fm.kinetix.model.entity.Avaliacao;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {
    boolean existsByConsulta_Id(Long consultaId);
    List<Avaliacao> findTop10ByFisioterapeuta_Usuario_IdAndStatusOrderByCriadoEmDesc(Long id, String status);
    @Query("select avg(a.nota) from Avaliacao a where a.fisioterapeuta.usuario.id = :id and a.status = 'APROVADA'")
    Double mediaPublica(@Param("id") Long id);
    @Query("select count(a) from Avaliacao a where a.fisioterapeuta.usuario.id = :id and a.status = 'APROVADA'")
    long totalPublicas(@Param("id") Long id);
}
