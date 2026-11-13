package com.wwtsr.pet;

/** Both hands up, waiting for a tap. */
public final class IdleState implements BodyState {

    public static final IdleState INSTANCE = new IdleState();

    private IdleState() {
    }

    @Override
    public BodyPose pose() {
        return BodyPose.IDLE;
    }

    @Override
    public BodyState onTap(Hand hand, long nowMillis, PetTimings timings) {
        return new TappingState(hand, nowMillis + timings.tapHoldMillis());
    }

    @Override
    public BodyState onTick(long nowMillis) {
        return this;
    }
}
