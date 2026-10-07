package com.itbm.inf2fm.kinetix.controller;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.*;
import com.itbm.inf2fm.kinetix.service.ClinicalService;
import com.itbm.inf2fm.kinetix.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/paciente")
@PreAuthorize("hasRole('PACIENTE')")
public class PacienteController {
    private final PacienteService pacientes;
    private final ClinicalService clinical;
    public PacienteController(PacienteService pacientes, ClinicalService clinical) {
        this.pacientes=pacientes; this.clinical=clinical;
    }

    @GetMapping("/perfil")
    public PacientePerfilResumo meuPerfil(@AuthenticationPrincipal Jwt jwt) {
        return pacientes.meuPerfil(id(jwt));
    }

    @PutMapping("/perfil")
    public PacientePerfilResumo atualizarPerfil(@AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody PerfilPacienteRequest request) {
        return pacientes.atualizarPerfil(id(jwt),request);
    }

    @PostMapping("/prescricoes/{planoId}/lembretes")
    @ResponseStatus(HttpStatus.CREATED)
    public NotificacaoResumo programarLembrete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long planoId,
        @Valid @RequestBody LembreteExercicioRequest request) {
        return clinical.programarLembreteExercicio(id(jwt),planoId,request.enviarEm());
    }

    private Long id(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
}
