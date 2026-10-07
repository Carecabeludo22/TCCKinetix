package com.itbm.inf2fm.kinetix.controller;
import com.itbm.inf2fm.kinetix.dto.ApiDtos.PlanoAssinaturaResumo;
import com.itbm.inf2fm.kinetix.service.ProfissionalService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/v1/planos-assinatura")
public class PlanoAssinaturaController {
    private final ProfissionalService profissionais;
    public PlanoAssinaturaController(ProfissionalService profissionais) { this.profissionais = profissionais; }
    @GetMapping public List<PlanoAssinaturaResumo> listar() { return profissionais.listarPlanos(); }
}
