package in.littlelist;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
class Api {
    private final WishlistService service;
    private final AdminAccess admin;
    private final ObjectProvider<InMemoryUserDetailsManager> local;
    private final ObjectProvider<ClientRegistrationRepository> google;

    Api(WishlistService service, ObjectProvider<InMemoryUserDetailsManager> local,
        ObjectProvider<ClientRegistrationRepository> google, AdminAccess admin) {
        this.admin = admin;
        this.service = service;
        this.local = local;
        this.google = google;
    }

    @GetMapping("/session")
    Map<String, Object> session(Authentication principal, CsrfToken csrf) {
        return Map.of("admin", admin.allowed(principal), "signedIn", principal != null, "csrf", csrf.getToken(),
            "local", local.getIfAvailable() != null, "google", google.getIfAvailable() != null);
    }

    @GetMapping("/identity")
    Map<String,String> identity(Authentication auth) { return Map.of("identity",owner(auth)); }

    @GetMapping("/wishlists")
    List<WishlistService.Header> lists(Authentication auth) {
        return service.lists(owner(auth));
    }

    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteAccount(Authentication auth, HttpServletRequest request) {
        service.deleteAccount(owner(auth));
        request.getSession().invalidate();
    }

    @PostMapping("/wishlists")
    @ResponseStatus(HttpStatus.CREATED)
    WishlistService.Wishlist create(Authentication auth, @Valid @RequestBody ListInput input) {
        return service.create(owner(auth), input);
    }

    @GetMapping("/wishlists/{id}")
    WishlistService.Wishlist list(Authentication auth, @PathVariable UUID id) {
        return service.get(owner(auth), id);
    }

    @PatchMapping("/wishlists/{id}")
    WishlistService.Wishlist update(Authentication auth, @PathVariable UUID id, @Valid @RequestBody ListInput input) {
        return service.update(owner(auth), id, input);
    }

    @DeleteMapping("/wishlists/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Authentication auth, @PathVariable UUID id) {
        service.delete(owner(auth), id);
    }

    @PutMapping("/wishlists/{id}/publication")
    WishlistService.Wishlist publish(Authentication auth, @PathVariable UUID id) {
        return service.publish(owner(auth), id, true);
    }

    @DeleteMapping("/wishlists/{id}/publication")
    WishlistService.Wishlist unpublish(Authentication auth, @PathVariable UUID id) {
        return service.publish(owner(auth), id, false);
    }

    @PostMapping("/wishlists/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    WishlistService.Wishlist addGift(Authentication auth, @PathVariable UUID id, @Valid @RequestBody GiftInput input) {
        return service.saveGift(owner(auth), id, null, input);
    }

    @PatchMapping("/wishlists/{id}/items/{giftId}")
    WishlistService.Wishlist editGift(Authentication auth, @PathVariable UUID id, @PathVariable UUID giftId,
                                       @Valid @RequestBody GiftInput input) {
        return service.saveGift(owner(auth), id, giftId, input);
    }

    @DeleteMapping("/wishlists/{id}/items/{giftId}")
    WishlistService.Wishlist removeGift(Authentication auth, @PathVariable UUID id, @PathVariable UUID giftId) {
        return service.removeGift(owner(auth), id, giftId);
    }

    @GetMapping("/shared/{shareId}")
    WishlistService.Shared shared(@PathVariable UUID shareId) {
        return service.shared(shareId);
    }

    @PostMapping("/shared/{shareId}/items/{giftId}/claim-requests")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void requestClaim(@PathVariable UUID shareId, @PathVariable UUID giftId,
                      @Valid @RequestBody ClaimRequest input) {
        service.sendRequest(service.requestClaim(shareId, giftId, input.email(),input.intent()==null?"reserve":input.intent(),input.name(),input.message(),input.store()));
    }

    @PostMapping("/claims/confirm")
    WishlistService.Claim confirm(@Valid @RequestBody ConfirmInput input) {
        return service.confirm(input.token(), input.managementToken());
    }

    @PostMapping("/claims/view")
    WishlistService.Claim claim(@Valid @RequestBody TokenInput input) {
        return service.claim(input.token());
    }

    @PostMapping("/claims/{action:purchase|release}")
    WishlistService.Claim record(@PathVariable String action, @Valid @RequestBody TokenInput input) {
        return service.record(input.token(), action.equals("purchase") ? "purchased" : "released");
    }

    static String owner(Authentication auth) {
        if (auth.getPrincipal() instanceof OidcUser user) {
            return user.getIssuer() + "|" + user.getSubject();
        }
        return "local|" + auth.getName();
    }

    record ListInput(@NotBlank @Size(max = 100) String title, @Size(max = 80) String occasion, @Size(max = 500) String greeting) {
        ListInput(String title,String occasion) { this(title,occasion,null); }
    }
    record GiftInput(@NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 40) String category,
                     @Size(max = 300) String note, @Size(max = 2048) String retailerUrl,
                     @Min(0) Integer pricePaise, @NotNull @Min(1) @Max(99) Integer quantity,
                     @Size(max=120) String variant, @Pattern(regexp="essential|nice-to-have") String priority,
                     @Pattern(regexp="manual|link|extension|template") String captureSource, UUID requestId, @Size(max=2048) String imageUrl) {
        GiftInput(String name,String category,String note,String retailerUrl,Integer pricePaise,Integer quantity,String variant,String priority,String captureSource,UUID requestId) { this(name,category,note,retailerUrl,pricePaise,quantity,variant,priority,captureSource,requestId,null); }
        GiftInput(String name,String category,String note,String retailerUrl,Integer pricePaise,Integer quantity) {
            this(name,category,note,retailerUrl,pricePaise,quantity,null,null,null,null);
        }
    }
    record ClaimRequest(@NotBlank @Email @Size(max = 254) String email, @Pattern(regexp="reserve|purchase") String intent, @Size(max=100) String name, @Size(max=500) String message, @Size(max=100) String store) {}
    record ConfirmInput(@NotBlank @Pattern(regexp = "[a-f0-9]{64}") String token,
                        @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String managementToken) {}
    record TokenInput(@NotBlank @Pattern(regexp = "[a-f0-9]{64}") String token) {}
}
