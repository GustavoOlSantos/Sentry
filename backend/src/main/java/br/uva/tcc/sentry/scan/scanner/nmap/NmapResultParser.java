package br.uva.tcc.sentry.scan.scanner.nmap;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import br.uva.tcc.sentry.finding.domain.Host;
import br.uva.tcc.sentry.finding.domain.PortInfo;

@Component
public class NmapResultParser {

    private final CpeFormatConverter cpeFormatConverter;

    public NmapResultParser(CpeFormatConverter cpeFormatConverter) {
        this.cpeFormatConverter = cpeFormatConverter;
    }

    public List<Host> parse(String xml) {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // desabilita resolução de entidades externas (boa prática de segurança
            // ao parsear XML — evita ataques tipo XXE mesmo sendo saída do próprio nmap)

            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();

            List<Host> hosts = new ArrayList<>();
            NodeList hostNodes = doc.getElementsByTagName("host");

            for (int h = 0; h < hostNodes.getLength(); h++) {
                Element hostEl = (Element) hostNodes.item(h);
                Host host = parseHost(hostEl);
                if (host != null) {
                    hosts.add(host);
                }
            }
            return hosts;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Nmap XML result", e);
        }
    }

    private Host parseHost(Element hostEl) {
        String ip = null;
        NodeList addresses = hostEl.getElementsByTagName("address");
        for (int i = 0; i < addresses.getLength(); i++) {
            Element addr = (Element) addresses.item(i);
            if ("ipv4".equals(addr.getAttribute("addrtype")) || "ipv6".equals(addr.getAttribute("addrtype"))) {
                ip = addr.getAttribute("addr");
                break;
            }
        }
        if (ip == null) {
            return null; // host sem endereço não é útil pra gente
        }

        Host host = new Host(ip);

        NodeList hostnameNodes = hostEl.getElementsByTagName("hostname");
        if (hostnameNodes.getLength() > 0) {
            host.setHostname(((Element) hostnameNodes.item(0)).getAttribute("name"));
        }

        NodeList osMatchNodes = hostEl.getElementsByTagName("osmatch");
        if (osMatchNodes.getLength() > 0) {
            host.setOs(((Element) osMatchNodes.item(0)).getAttribute("name"));
        }

        NodeList portNodes = hostEl.getElementsByTagName("port");
        for (int p = 0; p < portNodes.getLength(); p++) {
            Element portEl = (Element) portNodes.item(p);
            PortInfo portInfo = parsePort(portEl);
            if (portInfo != null) {
                host.addPort(portInfo);
            }
        }

        return host;
    }

    private PortInfo parsePort(Element portEl) {
        Node stateNode = firstChildNamed(portEl, "state");
        String state = stateNode != null ? ((Element) stateNode).getAttribute("state") : "unknown";
        if (!"open".equals(state)) {
            return null; // já filtramos com --open no runner, isso é defesa extra
        }

        int port = Integer.parseInt(portEl.getAttribute("portid"));
        String protocol = portEl.getAttribute("protocol");

        String service = null, product = null, version = null, cpe23 = null;
        Node serviceNode = firstChildNamed(portEl, "service");
        if (serviceNode != null) {
            Element serviceEl = (Element) serviceNode;
            service = emptyToNull(serviceEl.getAttribute("name"));
            product = emptyToNull(serviceEl.getAttribute("product"));
            version = emptyToNull(serviceEl.getAttribute("version"));

            NodeList cpeNodes = serviceEl.getElementsByTagName("cpe");
            if (cpeNodes.getLength() > 0) {
                String rawCpe = cpeNodes.item(0).getTextContent();
                cpe23 = cpeFormatConverter.toCpe23(rawCpe);
            }
        }

        return new PortInfo(port, protocol, state, service, product, version, cpe23);
    }

    private Node firstChildNamed(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                return child;
            }
        }
        return null;
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}