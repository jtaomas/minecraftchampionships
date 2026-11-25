package com.mcm.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A vote request from the tournament core to the Decision Dome provider. The request carries the
 * information the provider cannot know from the game ids alone: the teams in their pods, the event
 * index, whether power-ups are allowed this round, whether this is the audience vote, and how many
 * wedges the dial currently holds.
 *
 * <p>All ids are lower-case game ids. The map is player UUID -> tournament team name.</p>
 */
public record McmVoteRequest(List<String> gameIds,
                             Map<UUID, String> teams,
                             int eventIndex,
                             boolean allowPowerUps,
                             boolean audienceVote,
                             int dialCapacity) {

    public McmVoteRequest {
        gameIds = gameIds == null ? List.of() : List.copyOf(gameIds);
        teams = teams == null ? Map.of() : Map.copyOf(teams);
        if (dialCapacity <= 0) dialCapacity = 8;
    }

    /** Minimal request for older callers/tests: no team pods, no power-ups, 8 wedges. */
    public static McmVoteRequest simple(List<String> gameIds) {
        return new McmVoteRequest(gameIds, Map.of(), 0, false, false, 8);
    }
}
