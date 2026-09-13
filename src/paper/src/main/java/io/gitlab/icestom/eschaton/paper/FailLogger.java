package io.gitlab.icestom.eschaton.paper;

import io.gitlab.icestom.eschaton.core.BruteForceReverseSolver;
import io.gitlab.icestom.eschaton.kinematics.D0;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.logging.Level;

public final class FailLogger implements AutoCloseable {

    private static final String FILE_NAME = "fails.log";

    private final JavaPlugin plugin;
    private final Path logFile;
    private final Object lock = new Object();
    private BufferedWriter writer;

    public FailLogger(JavaPlugin plugin) {
        this.plugin = plugin;
        Path dataFolder = plugin.getDataFolder().toPath();
        try {
            Files.createDirectories(dataFolder);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create plugin data folder", e);
        }
        this.logFile = dataFolder.resolve(FILE_NAME);
        this.writer = openWriter();
    }

    private BufferedWriter openWriter() {
        try {
            return Files.newBufferedWriter(
                    logFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open fail log for writing", e);
        }
    }

    public void log(String uuid, long gameTime, D0 now, BruteForceReverseSolver.Result result) {
        String line = String.format(
                Locale.ROOT,
                "%s %d %d N[%.10f %.10f %.10f, w=%.10f] E %s %.20f [%.10f %.10f %.10f]",
                uuid,
                System.currentTimeMillis(),
                gameTime,
                now.x(),
                now.y(),
                now.z(),
                now.yaw(),
                result.input(),
                result.error(),
                result.ex(),
                result.ey(),
                result.ez()
        );

        synchronized (lock) {
            try {
                writer.write(line);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to write to fail log, attempting to reopen", e);
                reopenAndRetry(line);
            }
        }
    }

    private void reopenAndRetry(String line) {
        try {
            writer.close();
        } catch (IOException ignored) {
        }
        try {
            writer = openWriter();
            writer.write(line);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Fail log is unwritable, dropping entry", e);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            try {
                writer.close();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Error closing fail log", e);
            }
        }
    }
}