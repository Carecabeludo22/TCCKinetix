package com.itbm.inf2fm.kinetix.model.repository;
import com.itbm.inf2fm.kinetix.model.entity.ItemPrescricao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ItemPrescricaoRepository extends JpaRepository<ItemPrescricao, Long> {
    List<ItemPrescricao> findByPlano_IdOrderByOrdem(Long planoId);
}
