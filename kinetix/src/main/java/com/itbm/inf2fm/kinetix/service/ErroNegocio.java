package com.itbm.inf2fm.kinetix.service;
import org.springframework.http.HttpStatus;
public class ErroNegocio extends RuntimeException {
    private final HttpStatus status;
    public ErroNegocio(HttpStatus status, String message) { super(message); this.status = status; }
    public HttpStatus getStatus() { return status; }
    public static ErroNegocio naoEncontrado(String m) { return new ErroNegocio(HttpStatus.NOT_FOUND, m); }
    public static ErroNegocio conflito(String m) { return new ErroNegocio(HttpStatus.CONFLICT, m); }
    public static ErroNegocio proibido(String m) { return new ErroNegocio(HttpStatus.FORBIDDEN, m); }
    public static ErroNegocio invalido(String m) { return new ErroNegocio(HttpStatus.BAD_REQUEST, m); }
    public static ErroNegocio naoAutorizado(String m) { return new ErroNegocio(HttpStatus.UNAUTHORIZED, m); }
}
