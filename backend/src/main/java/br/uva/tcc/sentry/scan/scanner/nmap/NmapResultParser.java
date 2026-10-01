package br.uva.tcc.sentry.scan.scanner.nmap;

import java.io.StringReader;
import java.time.LocalDateTime;
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

import br.uva.tcc.sentry.Asset.domain.Asset;
import br.uva.tcc.sentry.Asset.domain.AssetStatus;
import br.uva.tcc.sentry.Asset.domain.Service;
import br.uva.tcc.sentry.scan.domain.DiscoveredHost;

@Component
public class NmapResultParser {

    private final CpeFormatConverter cpeFormatConverter;

    public NmapResultParser(CpeFormatConverter cpeFormatConverter) {
        this.cpeFormatConverter = cpeFormatConverter;
    }

    public List<DiscoveredHost> parse(String xml) {

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

            List<DiscoveredHost> hosts = new ArrayList<>();
            NodeList hostNodes = doc.getElementsByTagName("host");

            for (int h = 0; h < hostNodes.getLength(); h++) {
                Element hostEl = (Element) hostNodes.item(h);
                DiscoveredHost host = parseHost(hostEl);
                if (host != null) {
                    hosts.add(host);
                }
            }
            return hosts;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Nmap XML result", e);
        }
    }

    private DiscoveredHost parseHost(Element hostEl) {
        String ip = null;
        String mac = null;
        NodeList addresses = hostEl.getElementsByTagName("address");
        for (int i = 0; i < addresses.getLength(); i++) {
            Element addr = (Element) addresses.item(i);
            String type = addr.getAttribute("addrtype");
            if (ip == null && ("ipv4".equals(type) || "ipv6".equals(type))) {
                ip = addr.getAttribute("addr");
            } else if (mac == null && "mac".equals(type)) {
                mac = emptyToNull(addr.getAttribute("addr"));
            }
        }
        if (ip == null) {
            return null; // host sem endereço não é útil pra gente
        }

        Asset asset = new Asset();
        asset.setIpAddress(ip);
        asset.setMacAddress(mac);
        asset.setStatus(parseAssetStatus(hostEl));

        // o scan acabou de ver este asset; quem decide se é "primeira vez"
        // ou só atualização de lastSeenAt é a camada que persiste/reconcilia
        LocalDateTime now = LocalDateTime.now();
        asset.setFirstSeenAt(now);
        asset.setLastSeenAt(now);

        NodeList hostnameNodes = hostEl.getElementsByTagName("hostname");
        if (hostnameNodes.getLength() > 0) {
            asset.setHostname(emptyToNull(((Element) hostnameNodes.item(0)).getAttribute("name")));
        }

        NodeList osMatchNodes = hostEl.getElementsByTagName("osmatch");
        if (osMatchNodes.getLength() > 0) {
            asset.setOperatingSystem(emptyToNull(((Element) osMatchNodes.item(0)).getAttribute("name")));
        }

        List<Service> services = new ArrayList<>();
        NodeList portNodes = hostEl.getElementsByTagName("port");
        for (int p = 0; p < portNodes.getLength(); p++) {
            Element portEl = (Element) portNodes.item(p);
            Service service = parseService(portEl, asset);
            if (service != null) {
                services.add(service);
            }
        }

        return new DiscoveredHost(asset, services);
    }

    /**
     * O Nmap só diz se o host está "up" ou "down". HEALTHY/UNHEALTHY depende de
     * análise posterior (findings), então um host que respondeu fica UNKNOWN.
     */
    private AssetStatus parseAssetStatus(Element hostEl) {
        Node statusNode = firstChildNamed(hostEl, "status");
        if (statusNode != null && "down".equals(((Element) statusNode).getAttribute("state"))) {
            return AssetStatus.DOWN;
        }
        return AssetStatus.UP;
    }

    private Service parseService(Element portEl, Asset asset) {
        Node stateNode = firstChildNamed(portEl, "state");
        String state = stateNode != null ? ((Element) stateNode).getAttribute("state") : "unknown";
        if (!"open".equals(state)) {
            return null; // já filtramos com --open no runner, isso é defesa extra
        }

        int port = Integer.parseInt(portEl.getAttribute("portid"));
        String protocol = portEl.getAttribute("protocol");

        String serviceName = null, product = null, version = null, extraInfo = null, cpe23 = null;
        Node serviceNode = firstChildNamed(portEl, "service");
        if (serviceNode != null) {
            Element serviceEl = (Element) serviceNode;
            serviceName = emptyToNull(serviceEl.getAttribute("name"));
            product = emptyToNull(serviceEl.getAttribute("product"));
            version = emptyToNull(serviceEl.getAttribute("version"));
            extraInfo = emptyToNull(serviceEl.getAttribute("extrainfo"));

            NodeList cpeNodes = serviceEl.getElementsByTagName("cpe");
            if (cpeNodes.getLength() > 0) {
                String rawCpe = cpeNodes.item(0).getTextContent();
                cpe23 = cpeFormatConverter.toCpe23(rawCpe);
            }
        }

        Service service = new Service(port, protocol, state, serviceName, product, version, cpe23);
        service.setExtraInfo(extraInfo);
        service.setAsset(asset);
        return service;
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