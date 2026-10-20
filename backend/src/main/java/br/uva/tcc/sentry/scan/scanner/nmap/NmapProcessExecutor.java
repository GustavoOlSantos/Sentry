package br.uva.tcc.sentry.scan.scanner.nmap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.scan.exception.PermanentScanException;

@Component
public class NmapProcessExecutor {

    public String execute(List<String> command, Path outputFile) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("Falha na execução do Nmap: " + error);
            }

            return Files.readString(outputFile, StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new PermanentScanException("Não foi possível iniciar o Nmap ou acessar sua saída. Verifique a instalação e as permissões.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A execução do Nmap foi interrompida", e);
        }
    }
}