package br.com.fiap.soulie.controller;

import br.com.fiap.soulie.api.dto.CreateFeedRequest;
import br.com.fiap.soulie.api.dto.FeedPostResponse;
import br.com.fiap.soulie.api.dto.LikeResponse;
import br.com.fiap.soulie.api.dto.RankingItemResponse;
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
}
