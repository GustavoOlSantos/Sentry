package br.uva.tcc.sentry.scan.scanner.nmap;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class NmapCommandBuilder {

    public List<String> build(String target, String outputFile) {
        List<String> command = new ArrayList<>();

        command.add("nmap");
        command.add("-sV");
        command.add("-oX");
        command.add(outputFile);
        command.add(target);

        return command;
    }
}