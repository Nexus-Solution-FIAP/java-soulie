package br.com.fiap.soulie.infra.security;

import br.com.fiap.soulie.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * O contrato de {@code UserDetailsService} exige lançar
     * {@code UsernameNotFoundException} quando não há usuário — devolver
     * {@code null}, como antes, faz o provedor de autenticação do Spring
     * quebrar internamente em vez de responder "credenciais inválidas".
     *
     * <p>A mensagem é genérica de propósito: dizer "e-mail não cadastrado"
     * entregaria a quem está sondando quais contas existem.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("Credenciais inválidas");
        }
        return user;
    }
}
