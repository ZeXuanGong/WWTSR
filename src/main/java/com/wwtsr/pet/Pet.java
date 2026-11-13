package com.wwtsr.pet;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The pet itself: its body state, its blinking, which hand taps next and the tap count.
 * It knows nothing about windows or pictures; callers hand it the time, so it can be tested
 * without waiting.
 */
public final class Pet {

    private final PetTimings timings;
    private final Blinker blinker;
    private final TapCounter counter;
    private final List<PetListener> listeners = new CopyOnWriteArrayList<>();
    private BodyState body = IdleState.INSTANCE;
    private Hand nextHand = Hand.LEFT;

    public Pet(PetTimings timings, long startMillis, long startCount) {
        this.timings = Objects.requireNonNull(timings);
        this.blinker = new Blinker(timings, startMillis);
        this.counter = new TapCounter(startCount);
    }

    public BodyPose bodyPose() {
        return body.pose();
    }

    public HeadPose headPose() {
        return blinker.pose();
    }

    public long tapCount() {
        return counter.count();
    }

    public void addListener(PetListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void removeListener(PetListener listener) {
        listeners.remove(listener);
    }

    /** A whole tap at {@code nowMillis}: the next hand goes down and the count goes up. */
    public void tap(long nowMillis) {
        body = body.onTap(nextHand, nowMillis, timings);
        nextHand = nextHand.other();
        counter.increment();
        notifyListeners();
    }

    /**
     * Something was pressed at {@code nowMillis}: the next hand goes down. The count is left
     * alone, because a key is counted when it is let go ({@link #count()}).
     */
    public void strike(long nowMillis) {
        body = body.onTap(nextHand, nowMillis, timings);
        nextHand = nextHand.other();
        notifyListeners();
    }

    /** One more tap is counted, without moving a hand. */
    public void count() {
        counter.increment();
        notifyListeners();
    }

    /** What was pressed was let go: the hand comes back up at once. */
    public void release() {
        BodyState next = body.onRelease();
        boolean changed = next.pose() != body.pose();
        body = next;
        if (changed) {
            notifyListeners();
        }
    }

    /** Uses up {@code amount} of the taps counted so far (opening a chest costs taps). */
    public void spend(long amount) {
        counter.spend(amount);
        notifyListeners();
    }

    /** Moves time on to {@code nowMillis}: hands come back up and the eyes blink. */
    public void tick(long nowMillis) {
        BodyState next = body.onTick(nowMillis);
        boolean changed = next.pose() != body.pose();
        body = next;
        changed |= blinker.tick(nowMillis);
        if (changed) {
            notifyListeners();
        }
    }

    private void notifyListeners() {
        for (PetListener listener : listeners) {
            listener.petChanged(this);
        }
    }
}
