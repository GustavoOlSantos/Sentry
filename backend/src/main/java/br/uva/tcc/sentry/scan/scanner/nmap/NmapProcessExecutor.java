package br.uva.tcc.sentry.scan.scanner.nmap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.scan.exception.PermanentScanException;
import br.uva.tcc.sentry.scan.exception.TransientScanException;

@Component
public class NmapProcessExecutor {

    private static final Logger log = LoggerFactory.getLogger(NmapProcessExecutor.class);

    // quanto da saída do processo a gente guarda só pra compor mensagens de
    // erro/log — não precisamos do nmap inteiro, só do suficiente pra diagnosticar
    private static final int MAX_OUTPUT_FOR_LOG = 8_000;

    @Value("${nmap.timeout-seconds:120}")
    private long timeoutSeconds;

    public String execute(List<String> command, Path outputFile) {
        long start = System.currentTimeMillis();
        log.info("Iniciando execução do Nmap (timeout={}s): {}", timeoutSeconds, String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        // junta stdout e stderr num stream só, pra gente ter só uma coisa pra drenar
        processBuilder.redirectErrorStream(true);

        Process process = null;
        ExecutorService drainerExecutor = Executors.newSingleThreadExecutor();

        try {
            process = processBuilder.start();
            final Process runningProcess = process;

            // lê o stdout/stderr combinados numa thread separada, continuamente,
            // enquanto o processo roda. Se a gente só fosse ler isso depois do
            // waitFor(), um scan com saída grande (muitas portas, -O, etc.)
            // podia encher o buffer do pipe e travar o processo pra sempre
            // esperando alguém esvaziar o outro lado.
            Future<String> outputFuture = drainerExecutor.submit(() -> readStream(runningProcess.getInputStream()));

            boolean finishedInTime = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finishedInTime) {
                throw handleTimeout(process, command, outputFuture);
            }

            int exitCode = process.exitValue();
            long elapsedMs = System.currentTimeMillis() - start;
            String capturedOutput = safeGet(outputFuture);

            if (exitCode != 0) {
                log.error("Nmap terminou com código {} após {}ms. Saída: {}", exitCode, elapsedMs, truncate(capturedOutput));
                throw new PermanentScanException(
                        "Falha na execução do Nmap (código de saída " + exitCode + "): " + truncate(capturedOutput));
            }

            if (!Files.exists(outputFile) || Files.size(outputFile) == 0) {
                log.error("Nmap terminou com código 0 mas não gerou XML de saída. Saída do processo: {}", truncate(capturedOutput));
                throw new PermanentScanException(
                        "O Nmap terminou sem erro, mas não produziu o arquivo XML de saída esperado.");
            }

            log.info("Nmap concluído com sucesso em {}ms.", elapsedMs);
            return Files.readString(outputFile, StandardCharsets.UTF_8);

        } catch (IOException e) {
            log.error("Não foi possível iniciar o Nmap ou acessar sua saída.", e);
            throw new PermanentScanException(
                    "Não foi possível iniciar o Nmap ou acessar sua saída. Verifique a instalação e as permissões.", e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread interrompida durante a execução do Nmap, encerrando o processo.");
            killProcess(process);
            throw new TransientScanException("A execução do Nmap foi interrompida.", e);

        } finally {
            drainerExecutor.shutdownNow();
        }
    }

    /**
     * Mata o processo que estourou o timeout e já lança a exceção certa
     * (TransientScanException — faz sentido tentar de novo mais tarde,
     * diferente de um PermanentScanException de comando inválido).
     */
    private TransientScanException handleTimeout(Process process, List<String> command, Future<String> outputFuture) {
        log.warn("Nmap excedeu o timeout de {}s, encerrando o processo (pid {}). Comando: {}",
                timeoutSeconds, process.pid(), String.join(" ", command));

        killProcess(process);
        String capturedOutput = safeGet(outputFuture);

        return new TransientScanException(
                "O Nmap excedeu o tempo limite de " + timeoutSeconds + "s e foi encerrado. Saída parcial: "
                        + truncate(capturedOutput));
    }

    /**
     * Encerra o processo de forma correta: pede educadamente (destroy),
     * espera um pouco, e só força (destroyForcibly) se ele ignorar.
     */
    private void killProcess(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }

        process.destroy();
        try {
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                log.warn("Processo do Nmap (pid {}) não encerrou após destroy(), chamando destroyForcibly().", process.pid());
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private String readStream(InputStream inputStream) {
        try {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // normal quando o processo é destruído no meio da leitura (o pipe fecha)
            return "";
        }
    }

    private String safeGet(Future<String> future) {
        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "(não foi possível capturar a saída do processo)";
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > MAX_OUTPUT_FOR_LOG
                ? text.substring(0, MAX_OUTPUT_FOR_LOG) + "... (truncado)"
                : text;
    }
}
