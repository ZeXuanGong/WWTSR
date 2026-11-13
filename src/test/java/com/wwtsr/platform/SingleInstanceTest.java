package com.wwtsr.platform;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SingleInstanceTest {

    @TempDir
    Path dir;

    @Test
    void onlyOneCopyHoldsTheLockAtATime() throws IOException {
        Path lock = dir.resolve("nested").resolve("running.lock");
        Optional<SingleInstance> first = SingleInstance.tryAcquire(lock);
        assertTrue(first.isPresent());
        assertTrue(SingleInstance.tryAcquire(lock).isEmpty(), "a second copy is refused");
        first.get().close();
        Optional<SingleInstance> again = SingleInstance.tryAcquire(lock);
        assertTrue(again.isPresent(), "free again once the first copy is gone");
        again.get().close();
    }
}
