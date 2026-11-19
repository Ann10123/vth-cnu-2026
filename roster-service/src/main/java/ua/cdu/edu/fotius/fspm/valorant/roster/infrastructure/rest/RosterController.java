package ua.cdu.edu.fotius.fspm.valorant.roster.infrastructure.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.annotation.Error;
import io.micronaut.security.annotation.Secured;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.validation.Validated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ua.cdu.edu.fotius.fspm.valorant.roster.application.JwtTokenService;
import ua.cdu.edu.fotius.fspm.valorant.roster.application.RosterApplicationService;
import ua.cdu.edu.fotius.fspm.valorant.roster.application.RosterNotFoundException;
import ua.cdu.edu.fotius.fspm.valorant.roster.domain.exception.DomainException;
import ua.cdu.edu.fotius.fspm.valorant.roster.domain.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Validated
@Controller("/api/v1")
public class RosterController {

    private static final Set<String> ALLOWED_MAPS = Set.of(
        "Ascent", "Bind", "Haven", "Split", "Icebox", "Breeze", "Fracture", "Pearl", "Lotus", "Sunset", "Abyss"
    );

    private final RosterApplicationService rosterService;
    private final JwtTokenService tokenService;

    @Inject
    public RosterController(RosterApplicationService rosterService,
                            JwtTokenService tokenService) {
        this.rosterService = rosterService;
        this.tokenService = tokenService;
    }

    @Serdeable
    public record LoginRequest(
        @NotBlank(message = "playerTag is required")
        @Size(min = 3, max = 50, message = "playerTag must be between 3 and 50 characters")
        String playerTag,

        @NotBlank(message = "pin is required")
        @Pattern(regexp = "^\\d{4,6}$", message = "pin must contain 4 to 6 digits")
        String pin
    ) {}

    @Serdeable public record LoginResponse(String accessToken, String tokenType, int expiresIn) {}
    @Serdeable public record LinkDto(String href, String method) {}
    @Serdeable public record MemberDto(int slot, String playerId, String playerTag, String agentKey, String roleType) {}
    @Serdeable public record RosterResponseDto(String id, String captainId, String mapName, String status,
        @JsonInclude(JsonInclude.Include.ALWAYS) List<MemberDto> members, Map<String, LinkDto> _links) {}

    @Serdeable
    public record CreateRosterRequest(
        @NotBlank(message = "mapName is required")
        @Size(max = 30, message = "mapName must not exceed 30 characters")
        String mapName,

        @NotBlank(message = "creatorPlayerId is required")
        @Size(max = 36, message = "creatorPlayerId must not exceed 36 characters")
        String creatorPlayerId
    ) {}

    @Serdeable
    public record AddAgentRequest(
        @Nullable
        @Min(value = 1, message = "slot must be between 1 and 5")
        @Max(value = 5, message = "slot must be between 1 and 5")
        Integer slot,

        @NotBlank(message = "playerId is required")
        @Size(max = 36, message = "playerId must not exceed 36 characters")
        String playerId,

        @NotBlank(message = "agentKey is required")
        @Pattern(regexp = "^[a-zA-Z0-9_-]{2,30}$", message = "agentKey must be 2-30 alphanumeric characters")
        String agentKey
    ) {}

    @Serdeable
    public record TransferCaptainRequest(
        @NotBlank(message = "newCaptainId is required")
        @Size(max = 36, message = "newCaptainId must not exceed 36 characters")
        String newCaptainId
    ) {}

    @Serdeable public record ErrorDto(String code, String message) {}

    @Post("/auth/login")
    public HttpResponse<?> login(@Valid @Body LoginRequest req) {
        // Validate player exists in the database (uses existing players table)
        try {
            rosterService.findPlayerIdByTag(req.playerTag());
        } catch (IllegalArgumentException e) {
            return HttpResponse.badRequest().body(new ErrorDto("INVALID_CREDENTIALS", "Player not found"));
        }

        // Validate PIN (mock: always "1234").
        // In real life: it should be compared with one in DB or verified with external Identity Provider.
        if (!"1234".equals(req.pin())) {
            return HttpResponse.badRequest().body(new ErrorDto("INVALID_CREDENTIALS", "Invalid PIN"));
        }

        // Issue stateless mock token — nothing stored in the database
        String token = tokenService.createToken(req.playerTag());
        return HttpResponse.ok(new LoginResponse(token, "Bearer", 3600));
    }

    @Secured("isAuthenticated")
    @Post("/rosters")
    public HttpResponse<?> createRoster(@Valid @Body CreateRosterRequest req) {
        if (ALLOWED_MAPS.stream().noneMatch(m -> m.equalsIgnoreCase(req.mapName().trim()))) {
            return HttpResponse.badRequest(new ErrorDto(
                "UNSUPPORTED_MAP",
                "Map '" + req.mapName() + "' is not in the active competitive map pool"
            ));
        }

        PlayerId creatorId = new PlayerId(req.creatorPlayerId());
        Roster created = rosterService.createRoster(req.mapName(), creatorId);
        return HttpResponse.created(toHalResponse(created, creatorId));
    }

    @Secured("isAuthenticated")
    @Get("/rosters/{id}")
    public HttpResponse<RosterResponseDto> getRoster(
            @NotBlank String id,
            @Header(value = "X-Player-Id", defaultValue = "p-001") @NotBlank String currentUserId) {
        Roster roster = rosterService.getRoster(new RosterId(id));
        return HttpResponse.ok(toHalResponse(roster, new PlayerId(currentUserId)));
    }

    @Secured("isAuthenticated")
    @Post("/rosters/{id}/agents")
    public HttpResponse<Void> addAgent(@NotBlank String id, @Valid @Body AddAgentRequest req) {
        rosterService.addAgent(new RosterId(id), req.slot(), new PlayerId(req.playerId()), new AgentKey(req.agentKey()));
        return HttpResponse.ok();
    }

    @Secured("isAuthenticated")
    @Delete("/rosters/{id}/agents/{agentKey}")
    public HttpResponse<Void> removeAgent(@NotBlank String id, @NotBlank String agentKey) {
        rosterService.removeAgent(new RosterId(id), new AgentKey(agentKey));
        return HttpResponse.noContent();
    }

    @Secured("isAuthenticated")
    @Put("/rosters/{id}/captain")
    public HttpResponse<?> transferCaptain(
            @NotBlank String id,
            @Header("X-Player-Id") @NotBlank String currentUserId,
            @Valid @Body TransferCaptainRequest req) {
        if (currentUserId.trim().equals(req.newCaptainId().trim())) {
            return HttpResponse.badRequest(new ErrorDto(
                "SELF_CAPTAIN_TRANSFER",
                "newCaptainId must differ from the current captain ID"
            ));
        }
        rosterService.transferCaptain(new RosterId(id), new PlayerId(currentUserId), new PlayerId(req.newCaptainId()));
        return HttpResponse.ok();
    }

    @Secured("isAuthenticated")
    @Post("/rosters/{id}/lock")
    public HttpResponse<Void> lockIn(@NotBlank String id, @Header("X-Player-Id") @NotBlank String currentUserId) {
        rosterService.lockIn(new RosterId(id), new PlayerId(currentUserId));
        return HttpResponse.noContent();
    }

    @Secured("isAuthenticated")
    @Post("/rosters/{id}/unlock")
    public HttpResponse<Void> unlock(@NotBlank String id, @Header("X-Player-Id") @NotBlank String currentUserId) {
        rosterService.unlock(new RosterId(id), new PlayerId(currentUserId));
        return HttpResponse.noContent();
    }

    @Secured("isAuthenticated")
    @Post("/rosters/{id}/archive")
    public HttpResponse<Void> archive(@NotBlank String id) {
        rosterService.archive(new RosterId(id));
        return HttpResponse.noContent();
    }

    @Error(exception = RosterNotFoundException.class)
    public HttpResponse<ErrorDto> handleNotFound(HttpRequest<?> request, RosterNotFoundException ex) {
        return HttpResponse.notFound(new ErrorDto("ROSTER_NOT_FOUND", ex.getMessage()));
    }

    @Error(exception = DomainException.class)
    public HttpResponse<ErrorDto> handleDomainError(HttpRequest<?> request, DomainException ex) {
        HttpStatus status = ex.getCode().startsWith("FORBIDDEN") ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST;
        return HttpResponse.status(status).body(new ErrorDto(ex.getCode(), ex.getMessage()));
    }

    private RosterResponseDto toHalResponse(Roster roster, PlayerId viewerId) {
        String id = roster.getId().value();
        List<MemberDto> memberDtos = roster.getMembers().stream()
            .map(m -> new MemberDto(m.getSlot(), m.getPlayerId().value(), m.getPlayerTag(), m.getAgentKey().value(), m.getRoleType().name()))
            .toList();

        Map<String, LinkDto> links = new HashMap<>();
        links.put("self", new LinkDto("/api/v1/rosters/" + id, "GET"));

        boolean isCaptain = roster.isCaptain(viewerId);
        if (roster.getStatus() == RosterStatus.DRAFT) {
            links.put("add_agent", new LinkDto("/api/v1/rosters/" + id + "/agents", "POST"));
            if (isCaptain) {
                links.put("transfer_captain", new LinkDto("/api/v1/rosters/" + id + "/captain", "PUT"));
                if (memberDtos.size() >= Roster.MIN_MEMBERS_TO_LOCK) {
                    links.put("lock_in", new LinkDto("/api/v1/rosters/" + id + "/lock", "POST"));
                }
            }
            
            // Add remove_agent links for each member
            for (RosterMember member : roster.getMembers()) {
                String removeLink = "/api/v1/rosters/" + id + "/agents/" + member.getAgentKey().value();
                links.put("remove_agent_" + member.getAgentKey().value(), 
                    new LinkDto(removeLink, "DELETE"));
            }
        } else if (roster.getStatus() == RosterStatus.LOCKED) {
            if (isCaptain) {
                links.put("unlock", new LinkDto("/api/v1/rosters/" + id + "/unlock", "POST"));
            }
            links.put("submit_match", new LinkDto("http://localhost:8081/api/v1/matches/submit", "POST"));
        }

        return new RosterResponseDto(
            id,
            roster.getCaptainId().value(),
            roster.getMapName(),
            roster.getStatus().name(),
            memberDtos,
            links
        );
    }
}
