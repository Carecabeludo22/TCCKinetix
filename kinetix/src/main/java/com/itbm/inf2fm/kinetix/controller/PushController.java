package com.itbm.inf2fm.kinetix.controller;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.*;
import com.itbm.inf2fm.kinetix.service.NotificacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PushController {
    private final NotificacaoService notificacoes;
    public PushController(NotificacaoService notificacoes) { this.notificacoes = notificacoes; }

    @PostMapping("/dispositivos-push")
    @ResponseStatus(HttpStatus.CREATED)
    public void registrar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PushTokenRequest request) {
        notificacoes.registrarDispositivo(Long.valueOf(jwt.getSubject()), request.token(), request.plataforma());
    }

    @GetMapping("/notificacoes")
    public List<NotificacaoResumo> minhas(@AuthenticationPrincipal Jwt jwt) {
        return notificacoes.minhasNotificacoes(Long.valueOf(jwt.getSubject()));
    }
}
