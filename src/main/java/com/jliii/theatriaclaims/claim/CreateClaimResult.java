package com.jliii.theatriaclaims.claim;

import org.jetbrains.annotations.Nullable;

public class CreateClaimResult {
    //whether the creation succeeded (it would fail if the new claim overlapped another existing claim)
    public boolean succeeded;

    //when succeeded, this is a reference to the new claim
    //when failed, this is a reference to the pre-existing, conflicting claim
    public @Nullable Claim claim;
}
