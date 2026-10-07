package com.itbm.inf2fm.kinetix.controller;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.*;
import com.itbm.inf2fm.kinetix.service.AdminService;
import com.itbm.inf2fm.kinetix.service.ConsultaService;
import com.itbm.inf2fm.kinetix.service.ProfissionalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final ProfissionalService profissionais;
    private final ConsultaService consultas;
    private final AdminService admin;
    public AdminController(ProfissionalService profissionais, ConsultaService consultas, AdminService admin) {
        this.profissionais=profissionais; this.consultas=consultas; this.admin=admin;
    }

    @PatchMapping("/fisioterapeutas/{usuarioId}/verificacao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verificarCrefito(@PathVariable Long usuarioId, @Valid @RequestBody VerificacaoRequest request) {
        profissionais.verificarCrefito(usuarioId, request.verificado());
    }

    @PatchMapping("/avaliacoes/{avaliacaoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void moderarAvaliacao(@PathVariable Long avaliacaoId, @Valid @RequestBody AvaliacaoModeracaoRequest request) {
        consultas.moderarAvaliacao(avaliacaoId, request.status());
    }

    @PostMapping("/especialidades")
    @ResponseStatus(HttpStatus.CREATED)
    public EspecialidadeResumo criarEspecialidade(@Valid @RequestBody EspecialidadeRequest request) {
        return admin.criarEspecialidade(request);
    }

    @PostMapping("/exercicios")
    @ResponseStatus(HttpStatus.CREATED)
    public Long criarExercicio(@Valid @RequestBody ExercicioRequest request) {
        return admin.criarExercicio(request);
    }

    @PostMapping("/planos-assinatura")
    @ResponseStatus(HttpStatus.CREATED)
    public Long criarPlano(@Valid @RequestBody PlanoAssinaturaRequest request) {
        return admin.criarPlano(request);
    }
}
