package br.com.fiap.soulie.domain.chat;

import br.com.fiap.soulie.api.dto.ChatReply;
import br.com.fiap.soulie.api.dto.ChatRequest;
import br.com.fiap.soulie.domain.mission.DailyMission;
import br.com.fiap.soulie.domain.mission.DailyMissionRepository;
import br.com.fiap.soulie.domain.mission.UserMissionCompletionRepository;
import br.com.fiap.soulie.domain.user.User;
import br.com.fiap.soulie.domain.user.UserRepository;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class ChatApiService {

    @Autowired
    private DailyMissionRepository dailyMissionRepository;

    @Autowired
    private UserMissionCompletionRepository completionRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public ChatReply reply(User user, ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new ValidationException("Mensagem é obrigatória");
        }
        if (request.context() != null && request.context().mission() != null && request.context().mission().name() != null) {
            var mission = request.context().mission();
            String desc = mission.desc() == null ? "sua ação sustentável" : mission.desc();
            int xp = mission.xp() == null ? 0 : mission.xp();
            return new ChatReply("Para a missão **" + mission.name() + "** (" + desc + "), minha recomendação é focar em pequenas atitudes contínuas. Ao concluir, tire uma foto clara para validar e garantir seus +" + xp + " XP! 🌿✨");
        }

        String question = request.message().toLowerCase(Locale.ROOT);
        if (question.contains("missão") || question.contains("missoes") || question.contains("missões")) {
            return new ChatReply(openMissionsMessage(user));
        }
        if (question.contains("dica") || question.contains("dicas")) {
            return new ChatReply("Dica verde do dia 💡: Reduza o banho para menos de 5 minutos e economize até 60L de água! Além de ajudar o planeta, você ganha +25 XP no seu Soulie!");
        }
        if (question.contains("xp") || question.contains("nível") || question.contains("nivel")) {
            return new ChatReply("O sistema de XP funciona assim: complete missões diárias para acumular pontos ecológicos. Seu Soulie evolui conforme você age! 🌱 → 🌿 → 🌳 Semente até 50 XP, Broto até 100, Planta Jovem até 200 e depois Árvore.");
        }
        if (question.contains("equipe") || question.contains("fiap") || question.contains("ranking")) {
            return new ChatReply(rankingMessage(user));
        }
        return new ChatReply("Ótima pergunta! Complete suas missões diárias e registre ações sustentáveis para fazer seu Soulie crescer. Cada ação conta! 🌍🌱");
    }

    private String openMissionsMessage(User user) {
        List<Long> done = completionRepository.findByUser_IdAndCompletedOn(user.getId(), LocalDate.now()).stream()
                .map(completion -> completion.getMission().getId())
                .toList();
        List<DailyMission> open = dailyMissionRepository.findAllByOrderByIdAsc().stream()
                .filter(mission -> !done.contains(mission.getId()))
                .limit(3)
                .toList();
        if (open.isEmpty()) {
            return "Você concluiu as missões de hoje. Amanhã tem um novo ciclo esperando o seu Soulie! 🌟";
        }
        String listed = open.stream()
                .map(mission -> mission.getName() + " (+" + mission.getXp() + " XP)")
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return "Para suas missões de hoje: " + listed + " ainda estão abertas! Qual você quer completar primeiro? 🌟";
    }

    private String rankingMessage(User user) {
        List<User> ranking = userRepository.findAllByActiveTrueOrderByPointsDescIdAsc();
        if (ranking.isEmpty()) {
            return "O ranking ainda está vazio. Complete uma missão para aparecer nele! 🏆";
        }
        User leader = ranking.get(0);
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
        return "Você está em #" + position + " no ranking. " + leader.getName() + " lidera com " + leader.getPoints() + " XP. Dá pra alcançar! 🏆";
    }
}
