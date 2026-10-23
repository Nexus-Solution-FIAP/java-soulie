package br.com.fiap.soulie.domain.chat;

import br.com.fiap.soulie.api.dto.ChatContext;
import br.com.fiap.soulie.api.dto.ChatReply;
import br.com.fiap.soulie.api.dto.ChatRequest;
import br.com.fiap.soulie.domain.mission.DailyMission;
import br.com.fiap.soulie.domain.mission.DailyMissionRepository;
import br.com.fiap.soulie.domain.mission.UserMissionCompletionRepository;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import br.com.fiap.soulie.infra.chat.LlmClient;
import br.com.fiap.soulie.validations.chat.ChatRequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatApiService {

    private static final String SYSTEM_PROMPT = """
            Você é a Alma, guia do app Soulie (pt-BR).
            Responda só com os FATOS abaixo. Não invente missão, XP, ranking nem regra.
            Se o fato não estiver no bloco, diga para olhar as missões do dia no app.
            Tom curto, amigável, no máximo 6 linhas. Pode usar **negrito**.
            Não fale de política, saúde clínica nem de outros apps.
            A foto da missão é validada por outra IA: peça evidência nítida, sem recorte da internet.
            """;

    @Autowired
    private DailyMissionRepository dailyMissionRepository;

    @Autowired
    private UserMissionCompletionRepository completionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private List<ChatRequestValidator> validators;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public ChatReply reply(User user, ChatRequest request) {
        validators.forEach(validator -> validator.validate(user, request));

        String question = request.message().strip();
        String questionLower = question.toLowerCase(Locale.ROOT);
        Grounding grounding = transactionTemplate.execute(status -> montarGrounding(user, request.context()));
        if (grounding == null) {
            return new ChatReply(respostaEstruturada(questionLower, Grounding.vazio()));
        }

        if (intencaoEstruturada(questionLower) && grounding.missionOnScreen() == null) {
            return new ChatReply(respostaEstruturada(questionLower, grounding));
        }

        Optional<String> gerada = llmClient.complete(SYSTEM_PROMPT, userPrompt(question, grounding));
        return gerada.map(ChatReply::new)
                .orElseGet(() -> new ChatReply(respostaEstruturada(questionLower, grounding)));
    }

    private Grounding montarGrounding(User user, ChatContext context) {
        List<DailyMission> catalogo = dailyMissionRepository.findAllByOrderByIdAsc();
        List<Long> feitasHoje = completionRepository
                .findByUser_IdAndCompletedOn(user.getId(), LocalDate.now()).stream()
                .map(completion -> completion.getMission().getId())
                .toList();
        List<MissaoFato> abertas = catalogo.stream()
                .filter(mission -> !feitasHoje.contains(mission.getId()))
                .limit(3)
                .map(MissaoFato::da)
                .toList();
        MissaoFato naTela = missaoDaTela(context, catalogo);
        Integer nivel = nivelDoSoulie(context);
        return new Grounding(user.getPoints(), nivel, abertas, rankingMessage(user), naTela);
    }

    private static Integer nivelDoSoulie(ChatContext context) {
        if (context == null || context.userLevel() == null) {
            return null;
        }
        int nivel = context.userLevel();
        if (nivel < 0 || nivel > 3) {
            return null;
        }
        return nivel;
    }

    private MissaoFato missaoDaTela(ChatContext context, List<DailyMission> catalogo) {
        if (context == null || context.mission() == null || context.mission().name() == null
                || context.mission().name().isBlank()) {
            return null;
        }
        String nome = context.mission().name();
        return catalogo.stream()
                .filter(mission -> mission.getName().equalsIgnoreCase(nome))
                .findFirst()
                .map(MissaoFato::da)
                .orElse(null);
    }

    private static boolean intencaoEstruturada(String question) {
        return perguntaMissao(question) || perguntaDica(question)
                || perguntaXp(question) || perguntaRanking(question);
    }

    private String respostaEstruturada(String question, Grounding grounding) {
        if (grounding.missionOnScreen() != null) {
            return dicaDaMissao(grounding.missionOnScreen());
        }
        if (perguntaMissao(question)) {
            return openMissionsMessage(grounding.openMissions());
        }
        if (perguntaDica(question)) {
            return dicaDoDia(grounding.openMissions());
        }
        if (perguntaXp(question)) {
            return textoXp(grounding.points());
        }
        if (perguntaRanking(question)) {
            return grounding.rankingLine();
        }
        return "Ótima pergunta! Complete suas missões diárias e registre ações sustentáveis para fazer seu Soulie crescer. Cada ação conta! 🌍🌱";
    }

    private static String userPrompt(String question, Grounding grounding) {
        StringBuilder fatos = new StringBuilder();
        fatos.append("FATOS:\n");
        fatos.append("- XP do usuário: ").append(grounding.points());
        if (grounding.userLevel() != null) {
            fatos.append(", nível do Soulie ").append(grounding.userLevel());
        }
        fatos.append('\n');
        if (grounding.openMissions().isEmpty()) {
            fatos.append("- missões abertas de hoje: nenhuma (ciclo concluído)\n");
        } else {
            String listed = grounding.openMissions().stream()
                    .map(mission -> mission.name() + " — " + mission.description()
                            + " (+" + mission.xp() + " XP, tag " + mission.tag() + ")")
                    .collect(Collectors.joining("; "));
            fatos.append("- missões abertas de hoje: ").append(listed).append('\n');
        }
        fatos.append("- ranking: ").append(grounding.rankingLine()).append('\n');
        if (grounding.missionOnScreen() != null) {
            var mission = grounding.missionOnScreen();
            fatos.append("- missão da tela: ").append(mission.name())
                    .append(" — ").append(mission.description())
                    .append(" (+").append(mission.xp()).append(" XP, tag ")
                    .append(mission.tag()).append(")\n");
        }
        fatos.append("PERGUNTA:\n").append(question);
        return fatos.toString();
    }

    private static boolean perguntaMissao(String question) {
        return question.contains("missão") || question.contains("missoes") || question.contains("missões");
    }

    private static boolean perguntaDica(String question) {
        return question.contains("dica") || question.contains("dicas");
    }

    private static boolean perguntaXp(String question) {
        return question.contains("xp") || question.contains("nível") || question.contains("nivel");
    }

    private static boolean perguntaRanking(String question) {
        return question.contains("equipe") || question.contains("fiap") || question.contains("ranking");
    }

    private static String openMissionsMessage(List<MissaoFato> open) {
        if (open.isEmpty()) {
            return "Você concluiu as missões de hoje. Amanhã tem um novo ciclo esperando o seu Soulie! 🌟";
        }
        String listed = open.stream()
                .map(mission -> mission.name() + " (+" + mission.xp() + " XP)")
                .collect(Collectors.joining(", "));
        return "Para suas missões de hoje: " + listed + " ainda estão abertas! Qual você quer completar primeiro? 🌟";
    }

    private static String dicaDoDia(List<MissaoFato> open) {
        if (open.isEmpty()) {
            return "Dica verde: você já fechou o ciclo de hoje. Amanhã tem missão nova — até lá, repita a ação sem marcar de novo. 🌿";
        }
        MissaoFato primeira = open.getFirst();
        return "Dica verde do dia 💡: comece por **" + primeira.name() + "** — "
                + primeira.description()
                + " Tire uma foto nítida da evidência para validar e ganhar +"
                + primeira.xp() + " XP.";
    }

    private static String dicaDaMissao(MissaoFato mission) {
        return "Para a missão **" + mission.name() + "** (" + mission.description()
                + "), faça a ação de verdade e tire uma foto clara da evidência. "
                + "A IA da foto valida o que a imagem mostra. Ao concluir, você garante +"
                + mission.xp() + " XP! 🌿✨";
    }

    private static String textoXp(int points) {
        return "O sistema de XP funciona assim: complete missões para acumular pontos ecológicos. "
                + "Seu Soulie evolui conforme você age! 🌱 → 🌿 → 🌳 "
                + "Semente até 50 XP, Broto até 100, Planta Jovem até 200 e depois Árvore. "
                + "Você está com " + points + " XP.";
    }

    private String rankingMessage(User user) {
        List<User> ranking = userRepository.findAllByActiveTrueOrderByPointsDescIdAsc();
        if (ranking.isEmpty()) {
            return "O ranking ainda está vazio. Complete uma missão para aparecer nele! 🏆";
        }
        User leader = ranking.getFirst();
        int position = 1;
        for (User member : ranking) {
            if (member.getId().equals(user.getId())) {
                break;
            }
            position++;
        }
        if (leader.getId().equals(user.getId())) {
            return "Você lidera o ranking com " + leader.getPoints() + " XP. Continua cuidando do seu Soulie! 🏆";
        }
        return "Você está em #" + position + " no ranking. " + leader.publicName()
                + " lidera com " + leader.getPoints() + " XP. Dá pra alcançar! 🏆";
    }

    private record MissaoFato(String name, String description, int xp, String tag) {
        static MissaoFato da(DailyMission mission) {
            return new MissaoFato(mission.getName(), mission.getDescription(), mission.getXp(), mission.getTag());
        }
    }

    private record Grounding(
            int points,
            Integer userLevel,
            List<MissaoFato> openMissions,
            String rankingLine,
            MissaoFato missionOnScreen
    ) {
        static Grounding vazio() {
            return new Grounding(0, null, List.of(), "O ranking ainda está vazio. Complete uma missão para aparecer nele! 🏆", null);
        }
    }
}
