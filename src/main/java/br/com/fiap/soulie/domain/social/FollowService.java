package br.com.fiap.soulie.domain.social;

import br.com.fiap.soulie.domain.social.dto.FollowResponse;
import br.com.fiap.soulie.domain.social.dto.SocialUserResponse;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seguir, deixar de seguir e consultar o grafo.
 *
 * <p>Todos os métodos de leitura são {@code @Transactional(readOnly = true)}: com
 * {@code open-in-view: false} a entidade volta destacada e o acesso a uma
 * associação {@code LAZY} estoura. Ver a regra no {@code CLAUDE.md}.
 */
@Service
public class FollowService {

    /**
     * Campos pelos quais as listas podem ser ordenadas.
     *
     * <p>O {@code Pageable} ia cru para o repository, e a entidade paginada é
     * {@code Follow} — então o Spring Data resolvia caminho aninhado e
     * {@code ?sort=followee.email} funcionava. Com uma conta sonda dava para
     * extrair o e-mail de um alvo por busca binária, comparando posições. É a
     * mesma falha do {@code GET /user?sort=email}, que o {@code UserService} já
     * tratava — e que não foi aplicada aqui.
     *
     * <p>Ordenar por campo do {@code User} associado não deve ser possível de
     * forma alguma: por isso a lista só tem campos do próprio {@code Follow}.
     */
    private static final Set<String> ORDENACAO_PERMITIDA = Set.of("createdAt", "id");

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public FollowResponse seguir(User atual, Long alvoId) {
        User alvo = carregarAlvoAtivo(atual, alvoId);

        if (!followRepository.existsByFollower_IdAndFollowee_Id(atual.getId(), alvoId)) {
            followRepository.save(new Follow(atual, alvo));
        }

        boolean amigos = followRepository.saoAmigos(atual.getId(), alvoId);
        return new FollowResponse(true, amigos, SocialUserResponse.of(alvo, amigos));
    }

    /**
     * Deixa de seguir.
     *
     * <p><b>Não carrega o alvo por id.</b> A versão anterior fazia
     * {@code userRepository.findById(alvoId)} sem exigir relação nenhuma e
     * devolvia nome, handle e pontos com 200 — ou seja, a rota de "deixar de
     * seguir" era uma leitura de perfil por id arbitrário, e varrer
     * {@code 1..N} entregava a base inteira. Isso anulava a decisão de permitir
     * busca só por handle exato, e como o handle deriva do e-mail, reconstruía
     * boa parte do que a Fase 1b tinha removido.
     *
     * <p>Agora o perfil só volta quando havia de fato um follow — e aí é alguém
     * que o solicitante já conhecia.
     */
    @Transactional
    public FollowResponse deixarDeSeguir(User atual, Long alvoId) {
        var follow = followRepository.findByFollower_IdAndFollowee_Id(atual.getId(), alvoId);

        if (follow.isEmpty()) {
            // Mesma resposta para "não existe", "não é ativo" e "você não
            // seguia": nada aqui confirma a existência de uma conta.
            return new FollowResponse(false, false, null);
        }

        User alvo = follow.get().getFollowee();
        followRepository.delete(follow.get());

        // Conta desativada nao volta no corpo nem para quem a seguia: ela pediu
        // exclusao, e o follow antigo nao e titulo para continuar lendo o
        // perfil. O follow e removido de qualquer forma.
        if (!alvo.isEnabled()) {
            return new FollowResponse(false, false, null);
        }
        return new FollowResponse(false, false, SocialUserResponse.of(alvo, false));
    }

    @Transactional(readOnly = true)
    public Page<SocialUserResponse> seguidores(User atual, Pageable pageable) {
        var pagina = followRepository.findByFollowee_IdAndFollower_ActiveTrue(
                atual.getId(), sanitizarOrdenacao(pageable));
        return comAmizade(atual, pagina, Follow::getFollower);
    }

    @Transactional(readOnly = true)
    public Page<SocialUserResponse> seguindo(User atual, Pageable pageable) {
        var pagina = followRepository.findByFollower_IdAndFollowee_ActiveTrue(
                atual.getId(), sanitizarOrdenacao(pageable));
        return comAmizade(atual, pagina, Follow::getFollowee);
    }

    @Transactional(readOnly = true)
    public Page<SocialUserResponse> amigos(User atual, Pageable pageable) {
        return followRepository.amigosDe(atual.getId(), sanitizarOrdenacao(pageable))
                .map(amigo -> SocialUserResponse.of(amigo, true));
    }

    /** Amizade mútua, com as duas contas ativas. É o que libera cutucada sem jardim. */
    @Transactional(readOnly = true)
    public boolean saoAmigos(Long umId, Long outroId) {
        return followRepository.saoAmigos(umId, outroId);
    }

    /**
     * Busca por handle **exato**.
     *
     * <p>Não é busca parcial de propósito: a auditoria achou que
     * {@code GET /user} entregava o e-mail de toda a base com {@code ?size=2000},
     * e não faria sentido fechar lá para reabrir a enumeração aqui.
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

    /**
     * Marca quem é amigo, com <b>uma</b> consulta para a página inteira.
     *
     * <p>Antes isto chamava {@code saoAmigos} dentro do {@code map}: com o teto
     * de 50 por página, eram 51 consultas por requisição. Amplificação barata
     * para quem chama e 50× mais cara para o banco.
     */
    private Page<SocialUserResponse> comAmizade(User atual,
                                                Page<Follow> pagina,
                                                java.util.function.Function<Follow, User> extrair) {
        Set<Long> amigos = new HashSet<>(followRepository.idsDosAmigos(atual.getId()));
        return pagina.map(follow -> {
            User pessoa = extrair.apply(follow);
            return SocialUserResponse.of(pessoa, amigos.contains(pessoa.getId()));
        });
    }

    /**
     * Mantém só os campos de ordenação permitidos, descartando o resto em
     * silêncio — mesmo padrão do {@code UserService.sanitizarOrdenacao}.
     *
     * <p>Descartar em vez de recusar também resolve o {@code ?sort=naoExiste},
     * que virava {@code PropertyReferenceException} e caía no catch-all como
     * 500 com stack no log, por requisição.
     */
    private Pageable sanitizarOrdenacao(Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return pageable;
        }
        var permitida = Sort.by(pageable.getSort().stream()
                .filter(ordem -> ORDENACAO_PERMITIDA.contains(ordem.getProperty()))
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), permitida);
    }

    private User carregarAlvoAtivo(User atual, Long alvoId) {
        if (alvoId == null || alvoId.equals(atual.getId())) {
            // O CHECK do banco também barra. Aqui é só para a mensagem ser clara.
            throw new ValidationException("Você não pode seguir a si mesmo");
        }
        // 404 e não 403: a existência de uma conta desativada não precisa ser
        // confirmada a quem tentou segui-la.
        return userRepository.findById(alvoId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
    }
}
