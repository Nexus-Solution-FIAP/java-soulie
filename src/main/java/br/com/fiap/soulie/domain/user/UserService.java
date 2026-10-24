package br.com.fiap.soulie.domain.user;


import br.com.fiap.soulie.domain.avatar.Avatar;
import br.com.fiap.soulie.domain.avatar.AvatarRepository;
import br.com.fiap.soulie.domain.user.dto.UpdateUserData;
import br.com.fiap.soulie.domain.user.dto.UserRequest;
import br.com.fiap.soulie.domain.user.dto.UserResponse;
import br.com.fiap.soulie.infra.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Rotas legadas de usuário.
 *
 * <p>Todas as operações sobre uma conta existente passaram a ser **sobre a
 * própria conta**. Antes, `PATCH /user/{id}` recebia um id qualquer e reativava
 * aquela conta sem conferir posse: qualquer usuário autenticado podia desfazer o
 * `DELETE /v1/users/me` de outra pessoa, isto é, cancelar a exclusão de conta
 * que o titular pediu.
 *
 * <p>O contrato novo do app usa `/v1/users/me` ({@code ProfileApiService}).
 */
@Service
public class UserService {

    @Autowired
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper mapper;

    @Autowired
    private AvatarRepository avatarRepository;

    /**
     * Campos pelos quais a listagem pode ser ordenada.
     *
     * <p>Remover o {@code email} do {@link UserResponse} fechou a coleta direta,
     * mas o {@code Pageable} aceita qualquer propriedade da entidade como
     * {@code sort} — inclusive as que saíram do corpo da resposta. Com
     * {@code GET /user?sort=email} dava para receber id e nome de toda a base
     * <b>em ordem alfabética de e-mail</b> e inferir o endereço de cada pessoa
     * pelos vizinhos. Um {@code sort} inexistente também virava 500.
     */
    private static final Set<String> ORDENACAO_PERMITIDA = Set.of("id", "name", "points");

    // Get
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsersDetails(Pageable pageable) {
        return repository.findAllByActiveTrue(sanitizarOrdenacao(pageable)).map(mapper::toDetail);
    }

    /** Detalhe de uma conta, apenas se for a do próprio solicitante. */
    @Transactional(readOnly = true)
    public UserResponse userDetails(Long id, Long requesterId) {
        return mapper.toDetail(findOwn(id, requesterId));
    }

    // Post
    @Transactional
    public UserResponse registerUser(UserRequest userRequest) {
        // Pré-checagem para não deixar a violação de unicidade subir até o handler
        // de DataIntegrityViolationException, que respondia 409 e logava o e-mail
        // duplicado.
        //
        // ATENÇÃO — isto NÃO fecha a enumeração de contas, só torna a resposta
        // deliberada e sem detalhe. O cadastro ainda responde diferente para
        // e-mail existente (400) e novo (201), então continua sendo possível
        // descobrir se um endereço tem conta. Fechar de verdade exige responder
        // igual nos dois casos e mandar a confirmação por e-mail — ou seja,
        // infraestrutura de e-mail, que o projeto não tem. Registrado como
        // pendência em docs/07-seguranca.md.
        if (repository.existsByEmail(userRequest.email())) {
            throw new BusinessException("Não foi possível concluir o cadastro com estes dados");
        }
        var user = mapper.toEntity(userRequest);
        user.assignHandle(uniqueHandle(userRequest.email()));
        // Criptografar ANTES de salvar. A ordem anterior salvava primeiro e só
        // depois criptografava, funcionando por dirty-checking do Hibernate —
        // correto por acidente, e quebraria se o método deixasse de ser
        // transacional ou se alguém inserisse um flush no meio.
        user.encryptPassword(passwordEncoder);
        repository.save(user);
        avatarRepository.save(Avatar.starter(user));
        return mapper.toDetail(user);
    }

    // Put
    @Transactional
    public UserResponse updateUser(UpdateUserData updateUserData, Long id) {
        var user = findExisting(id);
        user.updateData(updateUserData);
        // A senha nunca é atribuída crua: updateData não mexe nela.
        user.changePassword(updateUserData.password(), passwordEncoder);
        return mapper.toDetail(user);
    }

    // Patch
    @Transactional
    public UserResponse activeUser(Long id, Long requesterId) {
        var user = findOwn(id, requesterId);
        user.activateUser();
        return mapper.toDetail(user);
    }

    // Delete
    @Transactional
    public void excludeUser(Long id) {
        findExisting(id).deleteUser();
    }

    /**
     * Mantém só os campos de ordenação permitidos, descartando o resto em
     * silêncio em vez de devolver erro — a listagem continua funcionando, apenas
     * sem obedecer a um {@code sort} que não deveria ser aceito.
     */
    private Pageable sanitizarOrdenacao(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        var permitida = Sort.by(pageable.getSort().stream()
                .filter(ordem -> ORDENACAO_PERMITIDA.contains(ordem.getProperty()))
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), permitida);
    }

    private User findExisting(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }

    /**
     * Carrega a conta só se ela for do solicitante.
     *
     * <p>Devolve 403 em vez de 404 quando o id existe mas é de outra pessoa —
     * a existência da conta já é observável por outros caminhos, e um 404 aqui
     * esconderia do próprio titular um erro de uso legítimo.
     */
    private User findOwn(Long id, Long requesterId) {
        if (id == null || requesterId == null || !id.equals(requesterId)) {
            throw BusinessException.acessoNegado("Você só pode acessar a sua própria conta");
        }
        return findExisting(id);
    }

    private String uniqueHandle(String email) {
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        String base = local.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (base.isBlank()) {
            base = "soulie";
        }
        String candidate = "@" + base;
        int suffix = 1;
        while (repository.existsByHandle(candidate)) {
            candidate = "@" + base + suffix++;
        }
        return candidate;
    }
}
