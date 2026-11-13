package com.wwtsr.pet;

/**
 * What the pet's body is doing (state pattern). Each state answers events with the state that
 * follows, so {@link Pet} never switches on the kind of state.
 */
public sealed interface BodyState permits IdleState, TappingState {

    BodyPose pose();

    /** A tap with {@code hand} at {@code nowMillis}. */
    BodyState onTap(Hand hand, long nowMillis, PetTimings timings);

    /** The key or button was let go: the hand comes up at once, without waiting out its time. */
    default BodyState onRelease() {
        return IdleState.INSTANCE;
    }

    /** Time has moved on to {@code nowMillis}. */
    BodyState onTick(long nowMillis);
}
