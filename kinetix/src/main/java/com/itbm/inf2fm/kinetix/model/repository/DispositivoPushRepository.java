package com.itbm.inf2fm.kinetix.model.repository;
import com.itbm.inf2fm.kinetix.model.entity.DispositivoPush;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface DispositivoPushRepository extends JpaRepository<DispositivoPush, Long> {
    List<DispositivoPush> findByUsuario_IdAndAtivoTrue(Long usuarioId);
    Optional<DispositivoPush> findByToken(String token);
}
