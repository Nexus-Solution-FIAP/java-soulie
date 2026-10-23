package br.com.fiap.soulie.domain.activity;

import br.com.fiap.soulie.api.TimeLabels;
import br.com.fiap.soulie.api.dto.ActivityResponse;
import br.com.fiap.soulie.api.dto.CreateActivityRequest;
import br.com.fiap.soulie.domain.achievement.AchievementApiService;
import br.com.fiap.soulie.domain.mission.MissionApiService;
import br.com.fiap.soulie.domain.soulie.SoulieApiService;
import br.com.fiap.soulie.domain.user.User;
import jakarta.validation.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ActivityApiService {

    @Autowired
    private UserActivityRepository activityRepository;

    @Autowired
    private MissionApiService missionApiService;

    @Autowired
    private SoulieApiService soulieApiService;

    @Autowired
    private AchievementApiService achievementApiService;

    @Transactional(readOnly = true)
    public List<ActivityResponse> list(User user) {
        return activityRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Teto de XP por dia vindo de atividade livre.
     *
     * <p>50 e o XP da missao diaria mais valiosa: atividade livre, que nao tem
     * comprovacao nenhuma, nao deve render mais que um dia inteiro de missao
     * validada por IA.
     */
    private static final int TETO_DIARIO_DE_XP_LIVRE = 50;

    @Transactional
    public ActivityResponse create(User user, CreateActivityRequest request) {
        if (request == null || request.label() == null || request.label().isBlank()) {
            throw new ValidationException("Descrição da atividade é obrigatória");
        }
        // Missão do catálogo NÃO se conclui por aqui. A versão anterior casava
        // o texto livre com o nome da missão e entregava a recompensa cheia —
        // XP, impacto, streak e conquistas — sem foto e sem IA. Bastava escrever
        // "Ir de bicicleta" para contornar todo o pipeline de validação, o que
        // tornava o pipeline decorativo.
        //
        // Recusar com orientação, em vez de registrar em silêncio uma atividade
        // que não vale nada: assim o app consegue mandar a pessoa para o fluxo
        // certo, e ela não fica achando que concluiu a missão.
        var missaoMencionada = missionApiService.missaoMencionadaNoTexto(request.label());
        if (missaoMencionada.isPresent()) {
            throw new ValidationException(
                    "Para concluir a missão \"%s\" envie a foto em /missions/%d/validate."
                            .formatted(missaoMencionada.get().getName(), missaoMencionada.get().getId()));
        }

        LocalDateTime now = LocalDateTime.now();

        // O TETO E DIARIO E AGREGADO, nao por requisicao. O @Max(50) do DTO
        // limita uma chamada; o numero de chamadas nunca foi limitado, e por
        // isso a correcao anterior nao fechou nada: medido, 5 chamadas de 50
        // davam 250 pontos, com nivel, streak e conquista. O cliente continua
        // podendo PEDIR xp, mas quem decide quanto entra e o servidor.
        int pedido = request.xp() == null ? 0 : Math.max(request.xp(), 0);
        int jaConcedidoHoje = activityRepository.somaXpNaJanela(
                user.getId(), now.toLocalDate().atStartOfDay(), now.toLocalDate().plusDays(1).atStartOfDay());
        int xp = Math.max(Math.min(pedido, TETO_DIARIO_DE_XP_LIVRE - jaConcedidoHoje), 0);

        if (xp > 0) {
            var avatar = soulieApiService.getOrCreate(user);
            user.addPoints(xp);
            avatar.addXp(xp);
            user.registerActivityDay(now.toLocalDate());
            achievementApiService.sync(user, avatar);
        }
        // Grava o XP CONCEDIDO, nao o pedido: a resposta precisa dizer a verdade,
        // senao o app mostra um numero que o saldo nao tem -- e a soma do dia,
        // que e lida daqui, ficaria errada na proxima chamada.
        UserActivity activity = activityRepository.save(UserActivity.of(user, request.emoji(), request.label().trim(), xp, now));
        return toResponse(activity);
    }

    public ActivityResponse toResponse(UserActivity activity) {
        return new ActivityResponse(
                activity.getId(),
                activity.getEmoji(),
                activity.getLabel(),
                activity.getXp(),
                TimeLabels.activityDate(activity.getCreatedAt()),
                activity.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }
}
