package com.itbm.inf2fm.kinetix.model.repository;

import com.itbm.inf2fm.kinetix.model.entity.Fisioterapeuta;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FisioterapeutaRepository extends JpaRepository<Fisioterapeuta, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fisioterapeuta f where f.usuario.id = :id")
    Optional<Fisioterapeuta> bloquearPorUsuarioId(@Param("id") Long id);

    boolean existsByCrefitoUfAndCrefitoNumero(String uf, String numero);

    @EntityGraph(attributePaths = {"usuario", "especialidades"})
    @Query("select distinct f from Fisioterapeuta f " +
        "where f.ativo = true and f.crefitoVerificado = true and f.usuario.ativo = true " +
        "and (:cidade is null or lower(f.cidade) like lower(concat('%', :cidade, '%'))) " +
        "and (:bairro is null or lower(f.bairro) like lower(concat('%', :bairro, '%'))) " +
        "and (:uf is null or upper(f.uf) = upper(:uf)) " +
        "and (:especialidadeId is null or exists (select e.id from Fisioterapeuta f2 join f2.especialidades e " +
        "where f2.usuario.id = f.usuario.id and e.id = :especialidadeId and e.ativa = true)) " +
        "and (:minLat is null or f.latitude between :minLat and :maxLat) " +
        "and (:minLon is null or f.longitude between :minLon and :maxLon)")
    List<Fisioterapeuta> buscarPublicos(@Param("cidade") String cidade, @Param("bairro") String bairro,
        @Param("uf") String uf, @Param("especialidadeId") Long especialidadeId,
        @Param("minLat") BigDecimal minLat, @Param("maxLat") BigDecimal maxLat,
        @Param("minLon") BigDecimal minLon, @Param("maxLon") BigDecimal maxLon, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "especialidades"})
    @Query("select f from Fisioterapeuta f where f.usuario.id = :id and f.ativo = true and f.crefitoVerificado = true")
    Optional<Fisioterapeuta> buscarPerfilPublico(@Param("id") Long id);

    Optional<Fisioterapeuta> findByUsuario_Id(Long usuarioId);
}
