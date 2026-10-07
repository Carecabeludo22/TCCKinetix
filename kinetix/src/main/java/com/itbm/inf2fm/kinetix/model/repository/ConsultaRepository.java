package com.itbm.inf2fm.kinetix.model.repository;

import com.itbm.inf2fm.kinetix.model.entity.Consulta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Consulta c where c.id = :id")
    Optional<Consulta> bloquearPorId(@Param("id") Long id);

    @Query("select case when count(c) > 0 then true else false end from Consulta c " +
        "where c.fisioterapeuta.usuario.id = :fisioId and c.reservaAtiva = true " +
        "and c.inicio < :fim and c.fim > :inicio")
    boolean existeConflito(@Param("fisioId") Long fisioId, @Param("inicio") OffsetDateTime inicio,
        @Param("fim") OffsetDateTime fim);

    @Query("select case when count(c) > 0 then true else false end from Consulta c " +
        "where c.fisioterapeuta.usuario.id = :fisioId and c.paciente.usuario.id = :pacienteId " +
        "and (c.reservaAtiva = true or c.status = com.itbm.inf2fm.kinetix.model.enums.StatusConsulta.REALIZADA " +
        "or c.status = com.itbm.inf2fm.kinetix.model.enums.StatusConsulta.FALTOU)")
    boolean existeVinculoClinico(@Param("fisioId") Long fisioId, @Param("pacienteId") Long pacienteId);

    @EntityGraph(attributePaths = {"paciente.usuario", "fisioterapeuta.usuario", "servico"})
    List<Consulta> findByPaciente_Usuario_IdOrderByInicioDesc(Long pacienteId);

    @EntityGraph(attributePaths = {"paciente.usuario", "fisioterapeuta.usuario", "servico"})
    List<Consulta> findByFisioterapeuta_Usuario_IdOrderByInicioDesc(Long fisioId);

    @EntityGraph(attributePaths = {"paciente.usuario", "fisioterapeuta.usuario", "servico"})
    @Query("select c from Consulta c where c.id = :id")
    Optional<Consulta> buscarDetalhesPorId(@Param("id") Long id);
}
