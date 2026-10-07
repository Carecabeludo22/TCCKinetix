package com.itbm.inf2fm.kinetix.controller;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.*;
import com.itbm.inf2fm.kinetix.model.enums.PerfilUsuario;
import com.itbm.inf2fm.kinetix.service.ConsultaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/consultas")
public class ConsultaController {
    private final ConsultaService consultas;
    public ConsultaController(ConsultaService consultas) { this.consultas = consultas; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PACIENTE')")
    public ConsultaResumo agendar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ConsultaRequest request) {
        return consultas.agendar(id(jwt), request);
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasAnyRole('PACIENTE','FISIO')")
    public List<ConsultaResumo> minhas(@AuthenticationPrincipal Jwt jwt) {
        return consultas.minhas(id(jwt), perfil(jwt));
    }

    @PatchMapping("/{consultaId}/cancelar")
    @PreAuthorize("hasAnyRole('PACIENTE','FISIO','ADMIN')")
    public ConsultaResumo cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long consultaId,
        @Valid @RequestBody CancelamentoRequest request) {
        return consultas.cancelar(consultaId, id(jwt), perfil(jwt), request.motivo());
    }

    @PatchMapping("/{consultaId}/concluir")
    @PreAuthorize("hasRole('FISIO')")
    public ConsultaResumo concluir(@AuthenticationPrincipal Jwt jwt, @PathVariable Long consultaId) {
        return consultas.concluir(consultaId, id(jwt));
    }

    @PostMapping("/{consultaId}/avaliacao")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PACIENTE')")
    public void avaliar(@AuthenticationPrincipal Jwt jwt, @PathVariable Long consultaId,
        @Valid @RequestBody AvaliacaoRequest request) {
        consultas.avaliar(consultaId, id(jwt), request);
    }

    private Long id(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
    private PerfilUsuario perfil(Jwt jwt) { return PerfilUsuario.valueOf(jwt.getClaimAsString("role")); }
}
