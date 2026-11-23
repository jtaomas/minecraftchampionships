package com.mcm.api;

import java.util.List;
import java.util.function.Consumer;

/**
 * Optional: replaces the core's fixed "next event in 10 seconds" with a vote (e.g. a Decision Dome).
 * The core calls {@link #runVote} between events during a championship; the provider must call
 * {@code onChosen} exactly once with one of the given ids (or null to let the core pick the next in order).
 */
public interface McmVoteProvider {
    void runVote(List<String> remainingGameIds, Consumer<String> onChosen);

    /** Abort a running vote (tournament stopped). Do not call onChosen afterwards. */
    void cancelVote();

    /**
     * Admin ran {@code /mcm skipwait} (or fast-test mode's 1 s limit was reached) while a vote is running:
     * end the vote now with the current result and call {@code onChosen} soon. Return true if handled; false
     * makes the core cancel the vote and continue in playlist order. Since core 3.2.0.
     */
    default boolean skipWait() { return false; }
}
