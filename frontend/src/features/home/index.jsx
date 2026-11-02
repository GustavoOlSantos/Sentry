import {useEffect, useState, useMemo} from 'react';
import  {useNavigate} from 'react-router-dom';

import api from '../../services/api';

import '../../styles/home.css';
import { SideBar } from '../../components/sidebar';

function Home() {
    const navigate = useNavigate();

    const [assets, setAssets] = useState([]);

    const [vuln_low, setVulLow] = useState(0);
    const [vuln_med, setVulMed] = useState(0);
    const [vuln_high, setVulHi] = useState(0);
    const [vuln_crit, setVulCrut] = useState(0);
    const [vuln_resolv, setVulRes] = useState(0);

    const [vuln_total_chart, setVulnTotalChart] = useState(0);
    const [vuln_gradient, setVulnGradient] = useState('conic-gradient(transparent 0% 100%)');

    

    const getOSCategory = (operatingSystem) => {
        const os = (operatingSystem ?? '').toLowerCase();

        if (os.includes('windows')) return 'windows';
        if (os.includes('mac') || os.includes('darwin')) return 'mac';

        if (/linux|ubuntu|debian|centos|red hat|redhat|fedora|alpine/.test(os)) {
            return 'linux';
        }

        return 'unknown';
    };

    const assetStats = useMemo(() => {
        const stats = {
            total: assets.length,
            windows: 0,
            linux: 0,
            mac: 0,
            unknown: 0
        };

        assets.forEach(asset => {
            const category = getOSCategory(asset.operatingSystem);
            stats[category]++;
        });

        return {
            ...stats,
            percentages: {
                windows: stats.total ? (stats.windows / stats.total) * 100 : 0,
                linux: stats.total ? (stats.linux / stats.total) * 100 : 0,
                mac: stats.total ? (stats.mac / stats.total) * 100 : 0,
                unknown: stats.total ? (stats.unknown / stats.total) * 100 : 0
            }
        };
    }, [assets]);

    const vulnStats = useMemo(() => {
        const stats = {
            total: 0,
            low: 0,
            medium: 0,
            high: 0,
            critical: 0,
            resolved: 0
        };

        assets.forEach(asset => {
            const findings = asset.findings ?? [];

            findings.forEach(finding => {
                stats.total++;

                if (finding.status === 'RESOLVED' || finding.status === 'VALIDATED') {
                    stats.resolved++;
                }
                else{
                    switch (finding.severity) {
                    case 'LOW':
                        stats.low++;
                        break;
                    case 'MEDIUM':
                        stats.medium++;
                        break;
                    case 'HIGH':
                        stats.high++;
                        break;
                    case 'CRITICAL':
                        stats.critical++;
                        break;
                }
                }
            });
        });

        return stats;
    }, [assets]);

   useEffect(() => {
        api.get('/asset')
        .then(response => {
            const data = response.data;

            setAssets(data);

            if (data.length === 0) {
                console.warn("Nenhum ativo encontrado");
            }
        })
        .catch(error => {
            console.error('Error fetching assets:', error);
        });
    }, []);

    useEffect(() => {
        const values = [
            vulnStats.low,
            vulnStats.medium,
            vulnStats.high,
            vulnStats.critical,
            vulnStats.resolved
        ].map(value => Math.max(0, Number(value) || 0));

        const total = values.reduce((sum, value) => sum + value, 0);

        const colors = [
            'var(--severity-low)',
            'var(--severity-medium)',
            'var(--severity-high)',
            'var(--severity-critical)',
            'var(--severity-info)'
        ];

        let current = 0;

        const segments = values.map((value, index) => {
            const start = current;
            current += total > 0 ? (value / total) * 100 : 0;

            return `${colors[index]} ${start}% ${current}%`;
        });

        setVulnTotalChart(total);
        setVulnGradient(`conic-gradient(${segments.join(', ')})`);
    }, [vulnStats.low, vulnStats.medium, vulnStats.high, vulnStats.critical, vulnStats.resolved]);

  return (
    <main>
        <section className="home">
            
            <SideBar />

            <article className="overview">
                <h3>Visão geral</h3>

                <div className="overview_content">
                    <article className="overview_stats">
                        <section className="home_cards section assets_card">
                            <p>Ativos monitorados</p>

                            <h3>{assetStats.total}</h3>

                            <div className="assets_progress">
                                <span
                                    className="assets_progress_win"
                                    style={{ width: `${assetStats.percentages.windows}%` }}
                                />

                                <span
                                    className="assets_progress_lin"
                                    style={{ width: `${assetStats.percentages.linux}%` }}
                                />

                                <span
                                    className="assets_progress_mac"
                                    style={{ width: `${assetStats.percentages.mac}%` }}
                                />

                                <span
                                    className="assets_progress_unk"
                                    style={{ width: `${assetStats.percentages.unknown}%` }}
                                />
                            </div>
                        </section>

                        <section className="home_cards section">
                            <h4>Distribuição por Sistema Operacional</h4>

                            <ul>
                                <span>
                                    <li className="win-bullet">Windows</li>
                                    <h4>{Math.round(assetStats.percentages.windows)}%</h4>
                                </span>

                                 <span>
                                    <li className="lin-bullet">Linux</li>
                                    <h4>{Math.round(assetStats.percentages.linux)}%</h4>
                                </span>

                                 <span>
                                    <li className="mac-bullet">macOS</li>
                                    <h4>{Math.round(assetStats.percentages.mac)}%</h4>
                                </span>

                                 <span>
                                    <li className="gray-bullet">Desconhecido</li>
                                    <h4>{Math.round(assetStats.percentages.unknown)}%</h4>
                                </span>
                            </ul>
                        </section>
                    </article>

                    <section className="home_cards pizza section">
                        <p>Gravidade das vulnerabilidades encontradas</p>

                        <div className="vulnerability_chart">
                            <div className="vulnerability_donut" style={{ background: vuln_gradient }} >
                                <div className="donut_center">
                                    <strong>{vulnStats.total}</strong>
                                    <p>Vulnerabilidades</p>
                                </div>
                            </div>

                            <ul className="vulnerability_legend">
                                <li>
                                    <span className="legend_color low" />
                                    Baixa <strong>{vulnStats.low}</strong>
                                </li>

                                <li>
                                    <span className="legend_color medium" />
                                    Média <strong>{vulnStats.medium}</strong>
                                </li>

                                <li>
                                    <span className="legend_color high" />
                                    Alta <strong>{vulnStats.high}</strong>
                                </li>
                                
                                <li>
                                    <span className="legend_color critical" />
                                    Crítica <strong>{vulnStats.critical}</strong>
                                </li>

                                <li>
                                    <span className="legend_color resolved" />
                                    Resolvidas <strong>{vulnStats.resolved}</strong>
                                </li>
                            </ul>
                        </div>
                    </section>
                </div>
                
                <section className='actions'>
                    <button onClick={() => navigate('novo-scan')}className='btn primary'>Nova varredura</button>
                    <button onClick={() => navigate('hosts')} className='btn secondary'>Visualizar ativos</button>
                </section>
            </article>
      </section>
    </main>
  );
}

export default Home;