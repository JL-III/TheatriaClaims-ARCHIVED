package com.jliii.theatriaclaims.events;

import com.jliii.theatriaclaims.claim.Claim;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * An {@link Event} involving a {@link Claim}.
 */
public abstract class ClaimEvent extends Event
{

    private final @NotNull Claim claim;

    /**
     * Construct a new {@code ClaimEvent}.
     *
     * @param claim the {@link Claim} involved
     */
    protected ClaimEvent(@NotNull Claim claim)
    {
        this.claim = claim;
    }

    /**
     * Get the {@link Claim} involved.
     *
     * @return the {@code Claim}
     */
    public final @NotNull Claim getClaim()
    {
        return this.claim;
    }

}
