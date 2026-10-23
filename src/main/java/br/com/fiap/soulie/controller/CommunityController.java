package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.CommunityGardenResponse;
import br.com.fiap.soulie.api.dto.CreateCommentRequest;
import br.com.fiap.soulie.api.dto.CreateFeedRequest;
import br.com.fiap.soulie.api.dto.GardenImpactResponse;
import br.com.fiap.soulie.api.dto.FeedCommentResponse;
import br.com.fiap.soulie.api.dto.FeedPostResponse;
import br.com.fiap.soulie.api.dto.JoinGardenRequest;
import br.com.fiap.soulie.api.dto.LikeResponse;
import br.com.fiap.soulie.api.dto.NotificationResponse;
import br.com.fiap.soulie.api.dto.NudgeRequest;
import br.com.fiap.soulie.api.dto.NudgeResponse;
import br.com.fiap.soulie.api.dto.RankingItemResponse;
import br.com.fiap.soulie.api.dto.ReadNotificationsRequest;
import br.com.fiap.soulie.domain.garden.GardenService;
import br.com.fiap.soulie.domain.community.CommunityApiService;
import br.com.fiap.soulie.domain.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/community", "/v1/community"})
@SecurityRequirement(name = "bearer-key")
@Tag(name = "Comunidade", description = "Ranking, feed, jardim compartilhado, lembretes e notificações")
public class CommunityController {

    @Autowired
    private CommunityApiService communityApiService;

    @Autowired
    private GardenService gardenService;

    @GetMapping("/ranking")
    @Operation(summary = "Ranking", description = "Ordem por XP. O item do usuário logado vem com isYou true e a sequência atual.")
    public ResponseEntity<List<RankingItemResponse>> ranking(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(communityApiService.ranking(user));
    }

    @GetMapping("/feed")
    @Operation(summary = "Feed do jardim")
    public ResponseEntity<List<FeedPostResponse>> feed(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(communityApiService.feed(user));
    }

    @PostMapping("/feed")
    @Operation(summary = "Publica no feed")
    public ResponseEntity<FeedPostResponse> publish(@Parameter(hidden = true) @AuthenticationPrincipal User user,
                                                    @RequestBody @Valid CreateFeedRequest request) {
        return ResponseEntity.ok(communityApiService.publish(user, request));
    }

    @PostMapping("/feed/{id}/like")
    @Operation(summary = "Curte ou descurte")
    public ResponseEntity<LikeResponse> like(@Parameter(hidden = true) @AuthenticationPrincipal User user,
                                             @Parameter(example = "8") @PathVariable Long id) {
        return ResponseEntity.ok(communityApiService.toggleLike(user, id));
    }

    @GetMapping("/feed/{id}/comments")
    @Operation(summary = "Comentários da publicação")
    public ResponseEntity<List<FeedCommentResponse>> comments(@Parameter(hidden = true) @AuthenticationPrincipal User user,
                                                              @Parameter(example = "8") @PathVariable Long id) {
        return ResponseEntity.ok(communityApiService.comments(user, id));
    }

    @PostMapping("/feed/{id}/comments")
    @Operation(summary = "Comenta uma publicação")
    public ResponseEntity<FeedCommentResponse> comment(@Parameter(hidden = true) @AuthenticationPrincipal User user,
                                                       @Parameter(example = "8") @PathVariable Long id,
                                                       @RequestBody @Valid CreateCommentRequest request) {
        return ResponseEntity.ok(communityApiService.comment(user, id, request));
    }

    @GetMapping("/garden")
    @Operation(summary = "Meu jardim compartilhado")
    @ApiResponse(responseCode = "200", description = "Jardim atual, com código de convite")
    @ApiResponse(responseCode = "204", description = "O usuário ainda não entrou em um jardim")
    public ResponseEntity<CommunityGardenResponse> garden(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return gardenService.current(user)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/garden")
    @Operation(summary = "Abre o meu jardim", description = "Cria o jardim se ainda não existir e devolve o código para os amigos entrarem.")
    public ResponseEntity<CommunityGardenResponse> openGarden(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gardenService.openMine(user));
    }

    @PostMapping("/garden/join")
    @Operation(summary = "Entra em um jardim pelo código")
    public ResponseEntity<CommunityGardenResponse> joinGarden(@Parameter(hidden = true) @AuthenticationPrincipal User user,
                                                              @RequestBody(required = false) @Valid JoinGardenRequest request) {
        return ResponseEntity.ok(gardenService.join(user, request == null ? null : request.code()));
    }

    @GetMapping("/garden/impact")
    @Operation(summary = "Impacto somado do jardim", description = "CO2, reciclagem, transporte, água e quantidade de ações de quem está no mesmo jardim.")
    public ResponseEntity<GardenImpactResponse> gardenImpact(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(communityApiService.gardenImpact(user));
    }

    @DeleteMapping("/garden")
    @Operation(summary = "Sai do jardim")
    @ApiResponse(responseCode = "204", description = "Saída concluída")
    public ResponseEntity<Void> leaveGarden(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        gardenService.leave(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/nudges")
    @Operation(summary = "Lembra um amigo da sequência", description = "Há um limite diário de lembretes. A pessoa recebe uma notificação.")
    public ResponseEntity<NudgeResponse> nudge(@Parameter(hidden = true) @AuthenticationPrincipal User user, @RequestBody @Valid NudgeRequest request) {
        return ResponseEntity.ok(communityApiService.nudge(user, request == null ? null : request.userId()));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Notificações ainda não lidas")
    public ResponseEntity<List<NotificationResponse>> notifications(@Parameter(hidden = true) @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(communityApiService.notifications(user));
    }

    @PostMapping("/notifications/read")
    @Operation(summary = "Marca notificações como lidas")
    @ApiResponse(responseCode = "204", description = "Ids informados foram removidos da lista")
    public ResponseEntity<Void> readNotifications(@Parameter(hidden = true) @AuthenticationPrincipal User user,
                                                  @RequestBody @Valid ReadNotificationsRequest request) {
        communityApiService.markNotificationsRead(user, request == null ? List.of() : request.ids());
        return ResponseEntity.noContent().build();
    }
}
