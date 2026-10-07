package com.itbm.inf2fm.kinetix.controller;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.*;
import com.itbm.inf2fm.kinetix.service.ConsultaService;
import com.itbm.inf2fm.kinetix.service.ProfissionalService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/profissionais")
public class ProfissionalController {
    private final ProfissionalService profissionais;
    private final ConsultaService consultas;
    public ProfissionalController(ProfissionalService profissionais, ConsultaService consultas) {
        this.profissionais = profissionais; this.consultas = consultas;
    }

    @GetMapping
    public List<ProfissionalResumo> buscar(@RequestParam(required=false) String cidade,
        @RequestParam(required=false) String bairro, @RequestParam(required=false) String uf,
        @RequestParam(required=false) Long especialidadeId, @RequestParam(required=false) Double latitude,
        @RequestParam(required=false) Double longitude, @RequestParam(required=false) Double raioKm) {
        return profissionais.buscar(cidade, bairro, uf, especialidadeId, latitude, longitude, raioKm);
    }

    @GetMapping("/{id}")
    public ProfissionalDetalhe perfil(@PathVariable Long id) { return profissionais.perfil(id); }

    @GetMapping("/{id}/horarios")
    public List<OffsetDateTime> horarios(@PathVariable Long id, @RequestParam Long servicoId,
        @RequestParam LocalDate de, @RequestParam LocalDate ate) {
        return consultas.horariosDisponiveis(id, servicoId, de, ate);
    }
}
