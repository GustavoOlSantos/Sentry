package br.uva.tcc.sentry.scan.scanner.nmap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class NmapProcessExecutor {

    public String execute(List<String> command, Path outputFile) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("Nmap execution failed: " + error);
            }

            return Files.readString(outputFile, StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to execute Nmap", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Nmap execution was interrupted", e);
        }
    }
}