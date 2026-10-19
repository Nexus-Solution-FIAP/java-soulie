package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.infra.security.record.AuthData;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.infra.security.record.TokenDataJWT;
import br.com.fiap.soulie.infra.security.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
@Tag(name = "Autenticação", description = "Login público. O token vale 2 horas.")
public class AuthController {

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private TokenService tokenService;

    @PostMapping
    @SecurityRequirements
    @Operation(summary = "Entra com e-mail e senha", description = "Devolve tokenJWT. Use esse valor no botão Authorize, no formato Bearer.")
    public ResponseEntity makeLogin(@RequestBody @Valid AuthData authData){
        var authToken = new UsernamePasswordAuthenticationToken(authData.login(), authData.password());
        var auth = manager.authenticate(authToken);

        var tokenJWT = tokenService.generateToken((User) auth.getPrincipal());

        return ResponseEntity.ok(new TokenDataJWT(tokenJWT));
    }
}
