package br.uva.tcc.sentry.scan.scanner.nmap;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import br.uva.tcc.sentry.scan.exception.PermanentScanException;

@Component
public class NmapCommandBuilder {

    public List<String> build(String target, String outputFile) {
        
        if (target == null || target.isBlank()) {
            throw new PermanentScanException("Não é possível construir o comando Nmap sem um alvo válido." );
        }

        List<String> command = new ArrayList<>();

        command.add("nmap");
        command.add("-sV");
        command.add("-O");
        command.add("-oX");
        command.add(outputFile);
        command.add(target);

        return command;
    }
}