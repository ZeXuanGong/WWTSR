package com.wwtsr.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PetTest {

    private static Pet pet() {
        return new Pet(PetTimings.DEFAULT, 0, 0);
    }

    @Test
    void startsIdleWithEyesOpen() {
        Pet pet = pet();
        assertEquals(BodyPose.IDLE, pet.bodyPose());
        assertEquals(HeadPose.EYES_OPEN, pet.headPose());
        assertEquals(0, pet.tapCount());
    }

    @Test
    void tapsAlternateHandsStartingWithTheLeft() {
        Pet pet = pet();
        List<BodyPose> seen = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            pet.tap(i * 1_000L);
            seen.add(pet.bodyPose());
        }
        assertEquals(List.of(BodyPose.LEFT_DOWN, BodyPose.RIGHT_DOWN, BodyPose.LEFT_DOWN, BodyPose.RIGHT_DOWN), seen);
    }

    @Test
    void handComesBackUpAfterTheHoldTime() {
        Pet pet = pet();
        pet.tap(1_000);
        pet.tick(1_219);
        assertEquals(BodyPose.LEFT_DOWN, pet.bodyPose());
        pet.tick(1_220);
        assertEquals(BodyPose.IDLE, pet.bodyPose());
    }

    @Test
    void aSecondTapRestartsTheHoldTime() {
        Pet pet = pet();
        pet.tap(1_000);
        pet.tap(1_100);
        pet.tick(1_250);
        assertEquals(BodyPose.RIGHT_DOWN, pet.bodyPose(), "the first tap's release must not end the second");
        pet.tick(1_320);
        assertEquals(BodyPose.IDLE, pet.bodyPose());
    }

    @Test
    void everyTapCounts() {
        Pet pet = new Pet(PetTimings.DEFAULT, 0, 36_957);
        pet.tap(10);
        pet.tap(20);
        pet.tap(30);
        assertEquals(36_960, pet.tapCount());
    }

    @Test
    void blinksEveryFiveSecondsForAQuarterSecond() {
        Pet pet = pet();
        pet.tick(4_999);
        assertEquals(HeadPose.EYES_OPEN, pet.headPose());
        pet.tick(5_000);
        assertEquals(HeadPose.EYES_CLOSED, pet.headPose());
        pet.tick(5_249);
        assertEquals(HeadPose.EYES_CLOSED, pet.headPose());
        pet.tick(5_250);
        assertEquals(HeadPose.EYES_OPEN, pet.headPose());
        pet.tick(10_000);
        assertEquals(HeadPose.EYES_CLOSED, pet.headPose());
    }

    @Test
    void aBlinkThatWasDueLongAgoIsSkipped() {
        Pet pet = pet();
        pet.tick(60_000);
        assertEquals(HeadPose.EYES_OPEN, pet.headPose());
        pet.tick(65_000);
        assertEquals(HeadPose.EYES_CLOSED, pet.headPose());
    }

    @Test
    void blinkingDoesNotDisturbATap() {
        Pet pet = pet();
        pet.tap(4_900);
        pet.tick(5_000);
        assertEquals(BodyPose.LEFT_DOWN, pet.bodyPose());
        assertEquals(HeadPose.EYES_CLOSED, pet.headPose());
    }

    @Test
    void listenersHearOnlyVisibleChanges() {
        Pet pet = pet();
        int[] calls = {0};
        pet.addListener(p -> calls[0]++);
        pet.tick(100);
        pet.tick(200);
        assertEquals(0, calls[0], "nothing changed");
        pet.tap(300);
        assertEquals(1, calls[0]);
        pet.tick(400);
        assertEquals(1, calls[0], "hand still down");
        pet.tick(520);
        assertEquals(2, calls[0], "hand came up");
    }

    @Test
    void lettingGoBringsTheHandUpAtOnce() {
        Pet pet = pet();
        pet.strike(1_000);
        assertEquals(BodyPose.LEFT_DOWN, pet.bodyPose());
        assertEquals(0, pet.tapCount(), "a press alone isn't counted");
        pet.release();
        assertEquals(BodyPose.IDLE, pet.bodyPose(), "well before the 220 ms are over");
        pet.count();
        assertEquals(1, pet.tapCount());
        // The next press uses the other hand; one held down still comes up by itself.
        pet.strike(1_050);
        assertEquals(BodyPose.RIGHT_DOWN, pet.bodyPose());
        pet.tick(1_269);
        assertEquals(BodyPose.RIGHT_DOWN, pet.bodyPose());
        pet.tick(1_270);
        assertEquals(BodyPose.IDLE, pet.bodyPose());
        pet.release();
        assertEquals(BodyPose.IDLE, pet.bodyPose(), "letting go with no hand down changes nothing");
    }

    @Test
    void counterStopsAtTheLargestValue() {
        TapCounter counter = new TapCounter(Long.MAX_VALUE);
        counter.increment();
        assertEquals(Long.MAX_VALUE, counter.count());
    }

    @Test
    void badValuesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TapCounter(-1));
        assertThrows(IllegalArgumentException.class, () -> new PetTimings(0, 5_000, 250));
        assertThrows(IllegalArgumentException.class, () -> new PetTimings(220, 250, 250));
    }
}
