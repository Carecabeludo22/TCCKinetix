package com.itbm.inf2fm.kinetix.service;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.AuthResponse;
import com.itbm.inf2fm.kinetix.model.entity.Usuario;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final Duration ttl;
    public JwtService(JwtEncoder encoder, Environment environment) {
        this.encoder = encoder;
        this.ttl = Duration.parse(environment.getProperty("kinetix.security.jwt.ttl", "PT8H"));
    }

    public AuthResponse emitir(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("kinetix-api").issuedAt(agora).expiresAt(expira)
            .subject(usuario.id.toString()).claim("role", usuario.perfil.name())
            .claim("email", usuario.email).build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AuthResponse(usuario.id, usuario.nome, usuario.perfil, token, "Bearer",
            OffsetDateTime.ofInstant(expira, ZoneOffset.UTC));
    }
}
