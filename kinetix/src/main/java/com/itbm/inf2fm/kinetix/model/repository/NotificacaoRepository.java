package com.itbm.inf2fm.kinetix.model.repository;
import com.itbm.inf2fm.kinetix.model.entity.Notificacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findByStatusAndEnviarEmLessThanEqualOrderByEnviarEm(String status, OffsetDateTime agora, Pageable pageable);
    List<Notificacao> findByUsuario_IdOrderByCriadoEmDesc(Long usuarioId, Pageable pageable);
}
