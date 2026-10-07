package com.itbm.inf2fm.kinetix.service;

import com.itbm.inf2fm.kinetix.dto.ApiDtos.*;
import com.itbm.inf2fm.kinetix.model.entity.*;
import com.itbm.inf2fm.kinetix.model.enums.PerfilUsuario;
import com.itbm.inf2fm.kinetix.model.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;

@Service
public class AuthService {
    private final UsuarioRepository usuarios;
    private final PacienteRepository pacientes;
    private final FisioterapeutaRepository fisioterapeutas;
    private final EspecialidadeRepository especialidades;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarios, PacienteRepository pacientes,
        FisioterapeutaRepository fisioterapeutas, EspecialidadeRepository especialidades,
        PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarios = usuarios;
        this.pacientes = pacientes;
        this.fisioterapeutas = fisioterapeutas;
        this.especialidades = especialidades;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse cadastrarPaciente(RegisterPatientRequest request) {
        validarEmailLivre(request.email());
        Usuario usuario = novoUsuario(request.nome(), request.email(), request.senha(), request.telefone(),
            request.documentoIdentidade(), request.fotoUrl(), PerfilUsuario.PACIENTE);
        usuarios.save(usuario);
        Paciente paciente = new Paciente(usuario);
        paciente.dataNascimento = request.dataNascimento();
        paciente.cidade = request.cidade();
        paciente.bairro = request.bairro();
        paciente.uf = normalizarUf(request.uf());
        pacientes.save(paciente);
        return jwtService.emitir(usuario);
    }

    @Transactional
    public AuthResponse cadastrarFisioterapeuta(RegisterPhysioRequest request) {
        validarEmailLivre(request.email());
        String crefitoUf = normalizarUf(request.crefitoUf());
        if (fisioterapeutas.existsByCrefitoUfAndCrefitoNumero(crefitoUf, request.crefitoNumero().trim()))
            throw ErroNegocio.conflito("Já existe um profissional com este registro CREFITO.");
        Usuario usuario = novoUsuario(request.nome(), request.email(), request.senha(), request.telefone(),
            request.documentoIdentidade(), request.fotoUrl(), PerfilUsuario.FISIO);
        usuarios.save(usuario);
        Fisioterapeuta fisio = new Fisioterapeuta(usuario);
        fisio.crefitoNumero = request.crefitoNumero().trim();
        fisio.crefitoUf = crefitoUf;
        fisio.crefitoVerificado = false;
        fisio.nomeClinica = request.nomeClinica();
        fisio.apresentacao = request.apresentacao();
        fisio.cidade = request.cidade().trim();
        fisio.uf = normalizarUf(request.uf());
        fisio.logradouro = request.logradouro();
        fisio.numero = request.numero();
        fisio.complemento = request.complemento();
        fisio.bairro = request.bairro();
        fisio.cep = request.cep();
        fisio.latitude = request.latitude();
        fisio.longitude = request.longitude();
        fisio.atendeOnline = request.atendeOnline();
        if (request.especialidadesIds() != null && !request.especialidadesIds().isEmpty()) {
            var encontrados = especialidades.findByIdInAndAtivaTrue(request.especialidadesIds());
            if (encontrados.size() != request.especialidadesIds().size())
                throw ErroNegocio.invalido("Uma ou mais especialidades não existem ou estão inativas.");
            fisio.especialidades = new HashSet<>(encontrados);
        }
        fisioterapeutas.save(fisio);
        return jwtService.emitir(usuario);
    }

    @Transactional(readOnly = true)
    public AuthResponse entrar(LoginRequest request) {
        Usuario usuario = usuarios.findByEmailIgnoreCase(request.email().trim())
            .filter(u -> u.ativo)
            .orElseThrow(() -> ErroNegocio.naoAutorizado("E-mail ou senha inválidos."));
        if (!passwordEncoder.matches(request.senha(), usuario.senhaHash))
            throw ErroNegocio.naoAutorizado("E-mail ou senha inválidos.");
        return jwtService.emitir(usuario);
    }

    private void validarEmailLivre(String email) {
        if (usuarios.existsByEmailIgnoreCase(email.trim()))
            throw ErroNegocio.conflito("Já existe uma conta com este e-mail.");
    }

    private Usuario novoUsuario(String nome, String email, String senha, String telefone,
        String documento, String foto, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.nome = nome.trim();
        usuario.email = email.trim().toLowerCase();
        usuario.senhaHash = passwordEncoder.encode(senha);
        usuario.telefone = telefone;
        usuario.documentoIdentidade = documento;
        usuario.fotoUrl = foto;
        usuario.perfil = perfil;
        usuario.ativo = true;
        return usuario;
    }

    private String normalizarUf(String uf) { return uf == null ? null : uf.trim().toUpperCase(); }
}
