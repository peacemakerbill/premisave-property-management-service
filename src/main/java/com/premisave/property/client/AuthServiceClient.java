package com.premisave.property.client;

import com.premisave.property.dto.request.auth_service.SocialActionRequest;
import com.premisave.property.dto.response.*;
import com.premisave.property.dto.response.auth_service.ProfileViewResponse;
import com.premisave.property.dto.response.auth_service.ProfileViewStats;
import com.premisave.property.dto.response.auth_service.PublicProfileViewStats;
import com.premisave.property.dto.response.auth_service.ReviewDto;
import com.premisave.property.dto.response.auth_service.SocialActionResponse;
import com.premisave.property.dto.response.auth_service.UserInteractionDto;
import com.premisave.property.dto.response.auth_service.WhoIViewedResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(
    name = "auth-service",
    url = "${auth-service.url:http://localhost:8080}"
)
public interface AuthServiceClient {

    // ── Internal (API-key protected) ──────────────────────────────
    // Retained for other internal cross-service lookups (e.g. by userId/email
    // outside a user request context). NOT used for owner/tenant profile
    // sync anymore — see getMyProfile() below, which forwards the caller's
    // own JWT instead.

    @GetMapping("/internal/users/{userId}")
    UserDto getUserById(@PathVariable String userId,
                        @RequestHeader("X-API-Key") String apiKey);

    @GetMapping("/internal/users/email/{email}")
    UserDto getUserByEmail(@PathVariable String email,
                           @RequestHeader("X-API-Key") String apiKey);

    // ── Profile (user-JWT protected — forward Authorization header) ──

    /**
     * Fetch the profile of whoever the forwarded JWT belongs to.
     * Used by OwnerService/TenantService for quick-create, sync, and
     * sync-status flows instead of the internal API-key endpoint —
     * this way auth-service enforces its own auth on the request rather
     * than trusting a shared secret.
     */
    @GetMapping("/profile/me")
    UserDto getMyProfile(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/profile/user/{userId}")
    UserDto getUserPublicProfile(@PathVariable String userId,
                                  @RequestHeader("Authorization") String authHeader);

    /**
     * Raw Feign binding — do not call directly, auth-service returns 400
     * on blank/missing query. Use {@link #searchUsers(String, String)} instead.
     */
    @GetMapping("/profile/search")
    List<UserDto> searchUsersRaw(@RequestParam("query") String query,
                                  @RequestHeader("Authorization") String authHeader);

    /**
     * Search users by name, username, email, etc.
     * Returns an empty list for null/blank query instead of letting
     * auth-service's 400 surface as an unhandled FeignException.
     */
    default List<UserDto> searchUsers(String query, String authHeader) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return searchUsersRaw(query.trim(), authHeader);
    }

    @GetMapping("/profile/all")
    List<UserDto> getAllUsers(@RequestHeader("Authorization") String authHeader);

    // ── Profile Views (user-JWT protected) ──────────────────────────

    @PostMapping("/profile/views/{targetId}")
    ProfileViewResponse recordProfileView(@PathVariable String targetId,
                                           @RequestHeader("Authorization") String authHeader);

    @GetMapping("/profile/views/who-viewed-me")
    List<ProfileViewResponse> getWhoViewedMe(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/profile/views/who-i-viewed")
    List<WhoIViewedResponse> getWhoIViewed(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/profile/views/my-stats")
    ProfileViewStats getMyProfileViewStats(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/profile/views/stats")
    ProfileViewStats getMyStatsViaStatsEndpoint(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/profile/views/stats/{userId}")
    PublicProfileViewStats getUserViewStats(@PathVariable String userId,
                                             @RequestHeader("Authorization") String authHeader);

    // ── Social (user-JWT protected) ──────────────────────────────────

    @PostMapping("/social/like")
    SocialActionResponse likeUser(@RequestBody SocialActionRequest request,
                                   @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/social/unlike/{targetId}")
    SocialActionResponse unlikeUser(@PathVariable String targetId,
                                     @RequestHeader("Authorization") String authHeader);

    @PostMapping("/social/follow")
    SocialActionResponse followUser(@RequestBody SocialActionRequest request,
                                     @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/social/unfollow/{targetId}")
    SocialActionResponse unfollowUser(@PathVariable String targetId,
                                       @RequestHeader("Authorization") String authHeader);

    @PostMapping("/social/review")
    SocialActionResponse reviewUser(@RequestBody SocialActionRequest request,
                                     @RequestHeader("Authorization") String authHeader);

    @PutMapping("/social/review")
    SocialActionResponse editReview(@RequestBody SocialActionRequest request,
                                     @RequestHeader("Authorization") String authHeader);

    @DeleteMapping("/social/review/{reviewId}")
    SocialActionResponse deleteReview(@PathVariable String reviewId,
                                       @RequestHeader("Authorization") String authHeader);

    @GetMapping("/social/reviews/{targetId}")
    List<ReviewDto> getUserReviews(@PathVariable String targetId,
                                    @RequestHeader("Authorization") String authHeader);

    @GetMapping("/social/stats/{userId}")
    UserInteractionDto getUserStats(@PathVariable String userId,
                                     @RequestHeader("Authorization") String authHeader);

    @GetMapping("/social/my-likes")
    List<UserDto> getMyLikedUsers(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/social/my-following")
    List<UserDto> getMyFollowingUsers(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/social/my-likers")
    List<UserDto> getMyLikers(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/social/my-followers")
    List<UserDto> getMyFollowers(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/social/my-reviews")
    List<ReviewDto> getMyReviews(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/social/my-written-reviews")
    List<ReviewDto> getMyWrittenReviews(@RequestHeader("Authorization") String authHeader);

    @GetMapping("/social/like/status/{targetId}")
    Map<String, Boolean> getLikeStatus(@PathVariable String targetId,
                                        @RequestHeader("Authorization") String authHeader);

    @GetMapping("/social/follow/status/{targetId}")
    Map<String, Boolean> getFollowStatus(@PathVariable String targetId,
                                          @RequestHeader("Authorization") String authHeader);

    @GetMapping("/social/review/status/{targetId}")
    Map<String, Boolean> getReviewStatus(@PathVariable String targetId,
                                          @RequestHeader("Authorization") String authHeader);

    @GetMapping("/social/follow/mutual/{targetId}")
    Map<String, Boolean> getMutualFollowStatus(@PathVariable String targetId,
                                                @RequestHeader("Authorization") String authHeader);
}