package com.wwtsr.pet;

import java.util.Objects;

/** One hand is down; it goes back up at {@code releaseAtMillis} unless another tap comes first. */
public final class TappingState implements BodyState {

    private final Hand hand;
    private final long releaseAtMillis;

    TappingState(Hand hand, long releaseAtMillis) {
        this.hand = Objects.requireNonNull(hand);
        this.releaseAtMillis = releaseAtMillis;
    }

    public Hand hand() {
        return hand;
    }

    @Override
    public BodyPose pose() {
        return BodyPose.forHand(hand);
    }

    /** A new tap replaces this one and starts its own hold time. */
    @Override
    public BodyState onTap(Hand nextHand, long nowMillis, PetTimings timings) {
        return new TappingState(nextHand, nowMillis + timings.tapHoldMillis());
    }

    @Override
    public BodyState onTick(long nowMillis) {
        return nowMillis >= releaseAtMillis ? IdleState.INSTANCE : this;
    }
}
