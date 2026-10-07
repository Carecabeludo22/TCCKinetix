package com.itbm.inf2fm.kinetix.model.enums;
public enum StatusConsulta {
    SOLICITADA, CONFIRMADA, CANCELADA_PACIENTE, CANCELADA_FISIO, CANCELADA_ADMIN, REALIZADA, FALTOU;
    public boolean reservaAtiva() {
        return this == SOLICITADA || this == CONFIRMADA;
    }
    public boolean cancelada() {
        return this == CANCELADA_PACIENTE || this == CANCELADA_FISIO || this == CANCELADA_ADMIN;
    }
}
