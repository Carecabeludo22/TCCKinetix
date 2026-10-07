package com.itbm.inf2fm.kinetix.controller;
import com.itbm.inf2fm.kinetix.dto.ApiDtos.EspecialidadeResumo;
import com.itbm.inf2fm.kinetix.service.ProfissionalService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/v1/especialidades")
public class EspecialidadeController {
    private final ProfissionalService profissionais;
    public EspecialidadeController(ProfissionalService profissionais) { this.profissionais = profissionais; }
    @GetMapping public List<EspecialidadeResumo> listar() { return profissionais.listarEspecialidades(); }
}
