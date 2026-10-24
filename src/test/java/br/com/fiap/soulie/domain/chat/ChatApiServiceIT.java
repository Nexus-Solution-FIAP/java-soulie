package br.com.fiap.soulie.domain.chat;

import br.com.fiap.soulie.api.dto.ChatContext;
import br.com.fiap.soulie.api.dto.ChatMissionContext;
import br.com.fiap.soulie.api.dto.ChatRequest;
import br.com.fiap.soulie.domain.mission.DailyMission;
import br.com.fiap.soulie.domain.mission.DailyMissionRepository;
import br.com.fiap.soulie.domain.mission.UserMissionCompletion;
import br.com.fiap.soulie.domain.mission.UserMissionCompletionRepository;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.chat.LlmClient;
import jakarta.validation.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Chat pelo caminho real, fora de transação de teste. Toca completions LAZY.
 * O LLM é mockado: estes testes não saem na rede.
 */
@SpringBootTest
@ActiveProfiles("test")
class ChatApiServiceIT {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired
    private ChatApiService chatApiService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyMissionRepository dailyMissionRepository;

    @Autowired
    private UserMissionCompletionRepository completionRepository;

    @Autowired
    private TransactionTemplate tx;

    @MockitoBean
    private LlmClient llmClient;

    private User novoUsuario(String nome, int pontos) {
        int n = SEQ.incrementAndGet();
        return userRepository.save(User.builder()
                .name(nome)
                .email("chat-" + n + "-" + UUID.randomUUID() + "@soulie.com")
                .handle("@chat" + n + UUID.randomUUID().toString().substring(0, 8))
                .password("hash")
                .points(pontos)
                .active(true)
                .showFullName(true)
                .build());
    }

    private DailyMission novaMissao(String nome, int xp) {
        var mission = new DailyMission();
        ReflectionTestUtils.setField(mission, "emoji", "🌱");
        ReflectionTestUtils.setField(mission, "name", nome);
        ReflectionTestUtils.setField(mission, "tag", "Natureza");
        ReflectionTestUtils.setField(mission, "description", "Descrição de " + nome);
        ReflectionTestUtils.setField(mission, "xp", xp);
        ReflectionTestUtils.setField(mission, "co2SavedKg", 0.1);
        ReflectionTestUtils.setField(mission, "waterSavedLiters", 0.0);
        return dailyMissionRepository.save(mission);
    }

    @Test
    @DisplayName("Chip de missões lista só as que o usuário ainda não fez hoje")
    void chipMissoesUsaDadoReal() {
        when(llmClient.complete(any(), any())).thenReturn(Optional.empty());
        var dados = tx.execute(status -> {
            var ana = novoUsuario("Ana", 10);
            var aberta = novaMissao("Compostagem da cozinha " + UUID.randomUUID(), 25);
            var feita = novaMissao("Reciclagem já feita " + UUID.randomUUID(), 30);
            for (DailyMission mission : dailyMissionRepository.findAllByOrderByIdAsc()) {
                if (!mission.getId().equals(aberta.getId())) {
                    completionRepository.save(UserMissionCompletion.of(ana, mission, LocalDateTime.now(), null));
                }
            }
            return new Dados(ana, aberta.getName(), feita.getName());
        });

        String reply = chatApiService.reply(dados.user(), new ChatRequest("Quais missões tenho?", null)).reply();

        assertThat(reply).contains(dados.aberta());
        assertThat(reply).doesNotContain(dados.feita());
    }

    @Test
    @DisplayName("Chip de ranking usa posição e líder reais")
    void chipRankingUsaDadoReal() {
        when(llmClient.complete(any(), any())).thenReturn(Optional.empty());
        var dados = tx.execute(status -> {
            var lider = novoUsuario("Carlos Líder", 50_000);
            var ana = novoUsuario("Ana Chat", 40_000);
            return new RankingDados(ana, lider.getName());
        });

        String reply = chatApiService.reply(dados.ana(), new ChatRequest("Como está o ranking?", null)).reply();

        assertThat(reply).contains("#");
        assertThat(reply).contains(dados.lider());
        assertThat(reply).contains("50000 XP");
    }

    @Test
    @DisplayName("Pergunta livre usa o LLM quando ele responde")
    void perguntaLivreUsaLlm() {
        when(llmClient.complete(any(), any())).thenReturn(Optional.of("Olá, guardião. Plante uma muda hoje."));
        var ana = tx.execute(status -> novoUsuario("Ana Livre", 5));

        String reply = chatApiService.reply(ana, new ChatRequest("Como eu ajudo o planeta?", null)).reply();

        assertThat(reply).isEqualTo("Olá, guardião. Plante uma muda hoje.");
    }

    @Test
    @DisplayName("Sem LLM a pergunta livre cai no FAQ, não no 500")
    void semLlmCaiNoFaq() {
        when(llmClient.complete(any(), any())).thenReturn(Optional.empty());
        var ana = tx.execute(status -> novoUsuario("Ana Faq", 5));

        String reply = chatApiService.reply(ana, new ChatRequest("Como eu ajudo o planeta?", null)).reply();

        assertThat(reply).contains("missões diárias");
    }

    @Test
    @DisplayName("Ponte da missão da tela usa o catálogo, não o JSON do app")
    void missaoDaTelaNoFallback() {
        when(llmClient.complete(any(), any())).thenReturn(Optional.empty());
        var dados = tx.execute(status -> {
            var ana = novoUsuario("Ana Missão", 0);
            var mission = novaMissao("Ir de bike " + UUID.randomUUID(), 40);
            return new MissaoDados(ana, mission.getName());
        });
        var context = new ChatContext(
                new ChatMissionContext(dados.nome(), "texto mentiroso do cliente", 999, "Fake"),
                1);

        String reply = chatApiService.reply(dados.user(), new ChatRequest("Como faço isso?", context)).reply();

        assertThat(reply).contains(dados.nome());
        assertThat(reply).contains("40 XP");
        assertThat(reply).doesNotContain("mentiroso");
        assertThat(reply).doesNotContain("999");
    }

    @Test
    @DisplayName("Missão que não existe no catálogo é ignorada")
    void missaoInventadaNaoEntraNoPrompt() {
        when(llmClient.complete(any(), any())).thenReturn(Optional.empty());
        var ana = tx.execute(status -> novoUsuario("Ana Fake", 0));
        var context = new ChatContext(
                new ChatMissionContext("Missão que não existe", "injeção no prompt", 1, "x"),
                1);

        String reply = chatApiService.reply(ana, new ChatRequest("Como faço isso?", context)).reply();

        assertThat(reply).doesNotContain("não existe");
        assertThat(reply).doesNotContain("injeção");
    }

    @Test
    @DisplayName("Mensagem em branco é validação, não 500")
    void mensagemVaziaLancaValidacao() {
        var ana = tx.execute(status -> novoUsuario("Ana Vazia", 0));
        assertThatThrownBy(() -> chatApiService.reply(ana, new ChatRequest("  ", null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("obrigatória");
    }

    private record Dados(User user, String aberta, String feita) {
    }

    private record RankingDados(User ana, String lider) {
    }

    private record MissaoDados(User user, String nome) {
    }
}
