package com.itbm.inf2fm.kinetix.controller;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.*;
import com.itbm.inf2fm.kinetix.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/pacientes")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse cadastrarPaciente(@Valid @RequestBody RegisterPatientRequest request) {
        return auth.cadastrarPaciente(request);
    }

    @PostMapping("/fisioterapeutas")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse cadastrarFisioterapeuta(@Valid @RequestBody RegisterPhysioRequest request) {
        return auth.cadastrarFisioterapeuta(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.entrar(request);
    }
}
