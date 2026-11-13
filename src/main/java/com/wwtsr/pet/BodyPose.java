package com.wwtsr.pet;

/** The body layer's picture: both hands up, or one hand down on a tap. */
public enum BodyPose {
    IDLE,
    LEFT_DOWN,
    RIGHT_DOWN;

    public static BodyPose forHand(Hand hand) {
        return hand == Hand.LEFT ? LEFT_DOWN : RIGHT_DOWN;
    }
}
