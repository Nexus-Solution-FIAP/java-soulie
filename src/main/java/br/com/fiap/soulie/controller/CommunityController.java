package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.CommunityGardenResponse;
import br.com.fiap.soulie.api.dto.CreateCommentRequest;
import br.com.fiap.soulie.api.dto.CreateFeedRequest;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/community", "/v1/community"})
@SecurityRequirement(name = "bearer-key")
public class CommunityController {

    @Autowired
    private CommunityApiService communityApiService;

    @Autowired
    private GardenService gardenService;

    @GetMapping("/ranking")
    public ResponseEntity<List<RankingItemResponse>> ranking(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(communityApiService.ranking(user));
    }

    @GetMapping("/feed")
    public ResponseEntity<List<FeedPostResponse>> feed(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(communityApiService.feed(user));
    }

    @PostMapping("/feed")
    public ResponseEntity<FeedPostResponse> publish(@AuthenticationPrincipal User user,
                                                    @RequestBody CreateFeedRequest request) {
        return ResponseEntity.ok(communityApiService.publish(user, request));
    }

    @PostMapping("/feed/{id}/like")
    public ResponseEntity<LikeResponse> like(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(communityApiService.toggleLike(user, id));
    }

    @GetMapping("/feed/{id}/comments")
    public ResponseEntity<List<FeedCommentResponse>> comments(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(communityApiService.comments(user, id));
    }

    @PostMapping("/feed/{id}/comments")
    public ResponseEntity<FeedCommentResponse> comment(@AuthenticationPrincipal User user,
                                                       @PathVariable Long id,
                                                       @RequestBody CreateCommentRequest request) {
        return ResponseEntity.ok(communityApiService.comment(user, id, request));
    }

    @GetMapping("/garden")
    public ResponseEntity<CommunityGardenResponse> garden(@AuthenticationPrincipal User user) {
        return gardenService.findMine(user)
                .map(garden -> ResponseEntity.ok(new CommunityGardenResponse(
                        garden.getId(),
                        garden.getName(),
                        garden.getInviteCode(),
                        (int) garden.getUsers().stream().filter(member -> member.getActive() == null || member.getActive()).count()
                )))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/garden")
    public ResponseEntity<CommunityGardenResponse> openGarden(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(gardenService.openMine(user));
    }

    @PostMapping("/garden/join")
    public ResponseEntity<CommunityGardenResponse> joinGarden(@AuthenticationPrincipal User user,
                                                              @RequestBody JoinGardenRequest request) {
        return ResponseEntity.ok(gardenService.join(user, request == null ? null : request.code()));
    }

    @DeleteMapping("/garden")
    public ResponseEntity<Void> leaveGarden(@AuthenticationPrincipal User user) {
        gardenService.leave(user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/nudges")
    public ResponseEntity<NudgeResponse> nudge(@AuthenticationPrincipal User user, @RequestBody NudgeRequest request) {
        return ResponseEntity.ok(communityApiService.nudge(user, request == null ? null : request.userId()));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> notifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(communityApiService.notifications(user));
    }

    @PostMapping("/notifications/read")
    public ResponseEntity<Void> readNotifications(@AuthenticationPrincipal User user,
                                                  @RequestBody ReadNotificationsRequest request) {
        communityApiService.markNotificationsRead(user, request == null ? List.of() : request.ids());
        return ResponseEntity.noContent().build();
    }
}
