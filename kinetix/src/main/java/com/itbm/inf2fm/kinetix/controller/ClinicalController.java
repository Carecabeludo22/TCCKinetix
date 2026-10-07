package com.itbm.inf2fm.kinetix.controller;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.*;
import com.itbm.inf2fm.kinetix.model.enums.PerfilUsuario;
import com.itbm.inf2fm.kinetix.service.ClinicalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ClinicalController {
    private final ClinicalService clinical;
    public ClinicalController(ClinicalService clinical) { this.clinical = clinical; }

    @PutMapping("/fisioterapeuta/consultas/{consultaId}/prontuario")
    @PreAuthorize("hasRole('FISIO')")
    public ProntuarioResumo salvarProntuario(@AuthenticationPrincipal Jwt jwt, @PathVariable Long consultaId,
        @Valid @RequestBody ProntuarioRequest request) {
        return clinical.salvarProntuario(consultaId, id(jwt), request);
    }

    @GetMapping("/paciente/prontuarios")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<ProntuarioResumo> prontuarioProprio(@AuthenticationPrincipal Jwt jwt) {
        return clinical.prontuarioPaciente(id(jwt), perfil(jwt), id(jwt));
    }

    @GetMapping("/fisioterapeuta/pacientes/{pacienteId}/prontuarios")
    @PreAuthorize("hasRole('FISIO')")
    public List<ProntuarioResumo> prontuarioPaciente(@AuthenticationPrincipal Jwt jwt, @PathVariable Long pacienteId) {
        return clinical.prontuarioPaciente(id(jwt), perfil(jwt), pacienteId);
    }

    @PostMapping("/paciente/diario-dor")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PACIENTE')")
    public RegistroDorResumo registrarDor(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(required=false) Long consultaId, @Valid @RequestBody RegistroDorRequest request) {
        return clinical.registrarDor(id(jwt), request, consultaId);
    }

    @GetMapping("/paciente/diario-dor")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<RegistroDorResumo> diarioProprio(@AuthenticationPrincipal Jwt jwt) {
        return clinical.diarioDor(id(jwt), perfil(jwt), id(jwt));
    }

    @GetMapping("/fisioterapeuta/pacientes/{pacienteId}/diario-dor")
    @PreAuthorize("hasRole('FISIO')")
    public List<RegistroDorResumo> diarioPaciente(@AuthenticationPrincipal Jwt jwt, @PathVariable Long pacienteId) {
        return clinical.diarioDor(id(jwt), perfil(jwt), pacienteId);
    }

    @PostMapping("/fisioterapeuta/pacientes/{pacienteId}/prescricoes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FISIO')")
    public PrescricaoResumo prescrever(@AuthenticationPrincipal Jwt jwt, @PathVariable Long pacienteId,
        @Valid @RequestBody PrescricaoRequest request) {
        return clinical.prescrever(id(jwt), pacienteId, request);
    }

    @GetMapping("/paciente/prescricoes")
    @PreAuthorize("hasRole('PACIENTE')")
    public List<PrescricaoResumo> prescricoes(@AuthenticationPrincipal Jwt jwt) {
        return clinical.prescricoesPaciente(id(jwt));
    }

    private Long id(Jwt jwt) { return Long.valueOf(jwt.getSubject()); }
    private PerfilUsuario perfil(Jwt jwt) { return PerfilUsuario.valueOf(jwt.getClaimAsString("role")); }
}
