package br.com.fiap.soulie.domain.social;

import br.com.fiap.soulie.domain.social.dto.FollowResponse;
import br.com.fiap.soulie.domain.social.dto.SocialUserResponse;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.exception.BusinessException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seguir, deixar de seguir e consultar o grafo.
 *
 * <p>Todos os métodos de leitura são {@code @Transactional(readOnly = true)}, e
 * isso é obrigatório, não estilo: com {@code open-in-view: false} a entidade
 * volta destacada e o primeiro acesso a uma associação {@code LAZY} estoura.
 * Esse bug já derrubou duas rotas e o pipeline inteiro de validação neste
 * projeto — ver a regra no {@code CLAUDE.md}.
 */
@Service
public class FollowService {

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Passa a seguir alguém.
     *
     * <p>Idempotente: seguir de novo não cria linha nem é erro — o
     * {@code UNIQUE} do banco garantiria de qualquer forma, e devolver erro para
     * quem clicou duas vezes seria ruído.
     */
    @Transactional
    public FollowResponse seguir(User atual, Long alvoId) {
        User alvo = carregarAlvo(atual, alvoId);

        if (!followRepository.existsByFollower_IdAndFollowee_Id(atual.getId(), alvoId)) {
            followRepository.save(new Follow(atual, alvo));
        }

        boolean amigos = followRepository.saoAmigos(atual.getId(), alvoId);
        return new FollowResponse(true, amigos, SocialUserResponse.of(alvo, amigos));
    }

    /**
     * Deixa de seguir.
     *
     * <p>Desfaz a amizade por consequência: sem os dois sentidos, não há
     * amizade. É o efeito pretendido de amizade ser derivada e não armazenada.
     */
    @Transactional
    public FollowResponse deixarDeSeguir(User atual, Long alvoId) {
        User alvo = userRepository.findById(alvoId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

        followRepository.findByFollower_IdAndFollowee_Id(atual.getId(), alvoId)
                .ifPresent(followRepository::delete);

        return new FollowResponse(false, false, SocialUserResponse.of(alvo, false));
    }

    @Transactional(readOnly = true)
    public Page<SocialUserResponse> seguidores(User atual, Pageable pageable) {
        return followRepository.findByFollowee_IdAndFollower_ActiveTrue(atual.getId(), pageable)
                .map(follow -> SocialUserResponse.of(
                        follow.getFollower(),
                        followRepository.saoAmigos(atual.getId(), follow.getFollower().getId())));
    }

    @Transactional(readOnly = true)
    public Page<SocialUserResponse> seguindo(User atual, Pageable pageable) {
        return followRepository.findByFollower_IdAndFollowee_ActiveTrue(atual.getId(), pageable)
                .map(follow -> SocialUserResponse.of(
                        follow.getFollowee(),
                        followRepository.saoAmigos(atual.getId(), follow.getFollowee().getId())));
    }

    @Transactional(readOnly = true)
    public List<SocialUserResponse> amigos(User atual) {
        return followRepository.amigosDe(atual.getId()).stream()
                .map(amigo -> SocialUserResponse.of(amigo, true))
                .toList();
    }

    /** Amizade mútua. É o que libera cutucada sem jardim em comum. */
    @Transactional(readOnly = true)
    public boolean saoAmigos(Long umId, Long outroId) {
        return followRepository.saoAmigos(umId, outroId);
    }

    @Transactional(readOnly = true)
    public List<Long> idsDosAmigos(Long userId) {
        return followRepository.idsDosAmigos(userId);
    }

    /**
     * Busca por handle **exato**.
     *
     * <p>Não é busca parcial de propósito. A auditoria encontrou que
     * {@code GET /user} entregava o e-mail de toda a base com {@code ?size=2000};
     * fechar lá e reabrir a enumeração aqui, por nome parcial, não faria sentido.
     * Com handle exato não dá para varrer nem descobrir quem existe por
     * tentativa — a pessoa compartilha o {@code @} dela por fora.
     */
    @Transactional(readOnly = true)
    public SocialUserResponse buscarPorHandle(User atual, String handle) {
        if (handle == null || handle.isBlank()) {
            throw new ValidationException("Informe o @ da pessoa que você procura");
        }
        String normalizado = handle.trim().startsWith("@") ? handle.trim() : "@" + handle.trim();

        User encontrado = userRepository.findByHandleAndActiveTrue(normalizado)
                .orElseThrow(() -> new EntityNotFoundException("Ninguém encontrado com esse @"));

        return SocialUserResponse.of(
                encontrado,
                followRepository.saoAmigos(atual.getId(), encontrado.getId()));
    }

    private User carregarAlvo(User atual, Long alvoId) {
        if (alvoId == null || alvoId.equals(atual.getId())) {
            // O CHECK do banco também barra. Aqui é só para a mensagem ser clara.
            throw new ValidationException("Você não pode seguir a si mesmo");
        }
        User alvo = userRepository.findById(alvoId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        if (!alvo.isEnabled()) {
            // 404 e não 403: a existência de uma conta desativada não precisa ser
            // confirmada a quem tentou segui-la.
            throw new EntityNotFoundException("Usuário não encontrado");
        }
        return alvo;
    }
}
