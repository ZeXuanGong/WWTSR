package com.wwtsr.platform;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Makes sure only one copy of the app runs: the first copy holds a lock on a file, which the
 * system releases by itself when that copy ends, however it ends.
 */
public final class SingleInstance implements AutoCloseable {

    private final FileChannel channel;
    private final FileLock lock;

    private SingleInstance(FileChannel channel, FileLock lock) {
        this.channel = channel;
        this.lock = lock;
    }

    /**
     * Tries to become the one running copy.
     *
     * @return the hold on the lock, or empty when another copy already has it
     * @throws IOException when the lock file can't be used at all
     */
    public static Optional<SingleInstance> tryAcquire(Path lockFile) throws IOException {
        Path dir = lockFile.toAbsolutePath().getParent();
        if (dir != null) {
            Files.createDirectories(dir);
        }
        FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            FileLock lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                return Optional.empty();
            }
            return Optional.of(new SingleInstance(channel, lock));
        } catch (OverlappingFileLockException e) {
            // Held by this same program (only happens in tests).
            channel.close();
            return Optional.empty();
        } catch (IOException e) {
            channel.close();
            throw e;
        }
    }

    @Override
    public void close() {
        try {
            lock.release();
        } catch (IOException e) {
            // The system releases it when the program ends.
        }
        try {
            channel.close();
        } catch (IOException e) {
            // Nothing more to do.
        }
    }
}
