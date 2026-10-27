import { useCallback, useState, useMemo, useEffect } from "react";
import { useNavigate } from "react-router-dom";

import '../../styles/hosts.css';

import api from "../../services/api";
import { SideBar } from "../../components/sidebar";

export function Hosts(){

    const navigate = useNavigate();
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('todos');
    const [osFilter, setOsFilter] = useState('todos');

    const [hosts, setHosts] = useState([]);
    const [loadingHosts, setLoadingHosts] = useState(false);
    const [errorHosts, setErrorHosts] = useState(null);

    const onlineHosts = hosts.filter( host => host.status?.toUpperCase() === 'UP').length;
    const offlineHosts = hosts.filter(host => host.status?.toUpperCase() === 'DOWN').length;
    
    const fetchHosts = useCallback(async () => {
        try {
            setLoadingHosts(true);
            setErrorHosts(null);

            const res = await api.get('asset');

            setHosts(res.data);

            if (res.data.length === 0) {
                console.warn("Nenhum host encontrado");
            }
        } catch (err) {
            console.error("Falha ao obter listagem de Assets", err);
            setErrorHosts("Não foi possível carregar os hosts.");
        } finally {
            setLoadingHosts(false);
        }
    }, []);

    useEffect(() => {
        fetchHosts();
    }, [fetchHosts]);

    const getOSCategory = (operatingSystem) => {
        const os = (operatingSystem ?? '').toLowerCase();

        if (os.includes('windows')) return 'windows';

        if (/linux|ubuntu|debian|centos|red hat|redhat|fedora|alpine/.test(os)) {
            return 'linux';
        }

        if (os.includes('mac') || os.includes('darwin')) return 'mac';

        return 'unknown';
    };

    const filteredHosts = useMemo(() => {
        const search = searchTerm.trim().toLowerCase();

        return (hosts ?? []).filter((host) => {
            const matchesSearch =
                (host.ipAddress ?? '').toLowerCase().includes(search) ||
                (host.hostname ?? '').toLowerCase().includes(search);

            const matchesStatus =
                statusFilter === 'todos' ||
                host.status?.toLowerCase() === statusFilter;

            const matchesOS =
                osFilter === 'todos' ||
                getOSCategory(host.operatingSystem) === osFilter;

            return matchesSearch && matchesStatus && matchesOS;
        });
    }, [hosts, searchTerm, statusFilter, osFilter]);

    return(
        <main className="hosts-container">
            <SideBar></SideBar>

            <section className="hosts-content">
                <h2>Ativos</h2>
                <p>Inventário de máquinas identificadas nas varreduras.</p>

                <section className="overview-hosts">

                    <div className="overview-cards">
                        <i className="fa-solid fa-server"></i>

                        <div>
                            <h4>{hosts.length}</h4>
                            <p>Ativos</p>
                        </div>
                    </div>

                    <div className="overview-cards">
                        <span className="status online"></span>
                        <div>
                            <h4>{onlineHosts}</h4>
                            <p>Online</p>
                        </div>
                    </div>

                    <div className="overview-cards">
                        <span className="status offline"></span>
                        <div >
                            <h4>{offlineHosts}</h4>
                            <p>Offline</p>
                        </div>
                    </div>

                </section>

                <section className="filter-table">
                    <div className="filter-search">
                        <i className="fa-solid fa-magnifying-glass"></i>

                        <input
                            id="ip-name"
                            name="ip-name"
                            type="text"
                            placeholder="Buscar por IP ou hostname"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>

                    <div className="filter-select">
                        <select
                            id="status"
                            name="status"
                            value={statusFilter}
                            onChange={(e) => setStatusFilter(e.target.value)}
                        >
                            <option value="todos">Status: Todos</option>
                            <option value="up">Online</option>
                            <option value="down">Offline</option>
                        </select>

                        <i className="fa-solid fa-chevron-down" aria-hidden="true"></i>
                    </div>

                    <div className="filter-select">
                        <select
                            id="sistemaOperacional"
                            name="sistemaOperacional"
                            value={osFilter}
                            onChange={(e) => setOsFilter(e.target.value)}
                        >
                            <option value="todos">Sistema: Todos</option>
                            <option value="windows">Windows</option>
                            <option value="linux">Linux</option>
                            <option value="mac">MacOS</option>
                            <option value="unknown">Desconhecido</option>
                        </select>

                        <i className="fa-solid fa-chevron-down" aria-hidden="true"></i>
                    </div>

                    <div>
                        <button className="btn-update" onClick={fetchHosts} disabled={loadingHosts} type="button" >
                            <i className={`fa-solid fa-rotate ${loadingHosts ? 'fa-spin' : ''}`}></i>
                            {loadingHosts ? 'Atualizando...' : 'Atualizar'}
                        </button>
                    </div>
                </section>

                <table>
                    <thead>
                        <tr>
                            <th>Ativo</th>
                            <th>Sistema Operacional</th>
                            <th>Serviços</th>
                            <th>Última Descoberta</th>
                            <th>Findings</th>
                            <th>Status</th>
                        </tr>
                    </thead>

                    <tbody>
                        {filteredHosts.map(host => (
                            <tr key={host.id ?? host.ipAddress} className="clickable">
                                <td>
                                    <span className="row">
                                        <i className="fa-solid fa-server big"></i>

                                        <span className="col">
                                            <h4>{host.ipAddress}</h4>
                                            <p>{host.hostname}</p>
                                        </span>
                                    </span>
                                </td>

                                <td className="os">
                                    {
                                        host.operatingSystem?.toLowerCase().includes("windows") ? (<i className="fa-brands fa-windows" style={{ color: "#4d92fa" }} />) : 
                                        host.operatingSystem?.toLowerCase().includes("linux") ? (<i className="fa-brands fa-linux" />) : 
                                        host.operatingSystem?.toLowerCase().includes("mac") ? (<i className="fa-brands fa-apple" style={{ color: "#ffffff" }} />) : 
                                        (<i className="fa-solid fa-desktop" />)
                                    }

                                    &nbsp;
                                    {normalizeOS(host.operatingSystem)}
                                </td>

                                <td>
                                    <span className="row">
                                        <i className="fa-solid fa-database"></i>
                                        {host.services.length} serviços
                                    </span>
                                </td>

                                <td>
                                    <span className="row">
                                        <i className="fa-regular fa-calendar"></i>{ new Date(host.lastSeenAt).toLocaleDateString("pt-BR")}
                                    </span>
                                </td>

                                <td></td>

                                <td>
                                    <div className="row space">
                                        <span className="hosts-status">
                                            <span className={`status ${host.status !== 'UP' ? 'offline' : 'online'}`}></span>
                                            {host.status === 'UP' ? 'Online' : 'Offline'}
                                        </span>
                                        
                                        <i className="fa-solid fa-chevron-right access-icon"></i>
                                    </div>    
                                </td>
                            </tr>
                        ))} 
                    </tbody>
                </table>

                {errorHosts && (
                    <p className="message error-message">{errorHosts}</p>
                )}

                {loadingHosts && hosts.length === 0 && (
                    <p className="message">Carregando host encontrados...</p>
                )}
            </section>
        </main>
    )
}

function normalizeOS(os) {
    if (!os) return "Desconhecido";

    if (/windows/i.test(os)) {
        const match = os.match(/Windows\s+(\d+)/i);
        return match ? `Windows ${match[1]}` : "Windows";
    }

    if (/mac\s?os|macos|darwin/i.test(os)) {
        const match = os.match(/(?:Mac\s?OS|macOS)\s+([\d.]+)/i);
        return match ? `Mac OS ${match[1]}` : "Mac OS";
    }

    if (/ubuntu/i.test(os)) {
        return "Linux Ubuntu";
    }

    if (/linux/i.test(os)) {
        return "Linux";
    }

    return os;
}

export default Hosts;