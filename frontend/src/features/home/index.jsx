import { useEffect, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';

import api from '../../services/api';

import '../../styles/home.css';
import { SideBar } from '../../components/sidebar';

function Home() {
    const navigate = useNavigate();

    const [assets, setAssets] = useState([]);
    const [apiError, setApiError] = useState(false);

    const [vulnGradient, setVulnGradient] = useState(
        'conic-gradient(#e5e7eb 0% 100%)'
    );

    const getOSCategory = (operatingSystem) => {
        const os = (operatingSystem ?? '').toLowerCase();

        if (os.includes('windows')) return 'windows';
        if (os.includes('mac') || os.includes('darwin')) return 'mac';

        if (
            /linux|ubuntu|debian|centos|red hat|redhat|fedora|alpine/.test(os)
        ) {
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

        assets.forEach((asset) => {
            const category = getOSCategory(asset?.operatingSystem);
            stats[category]++;
        });

        return {
            ...stats,
            percentages: {
                windows: stats.total
                    ? (stats.windows / stats.total) * 100
                    : 0,
                linux: stats.total
                    ? (stats.linux / stats.total) * 100
                    : 0,
                mac: stats.total
                    ? (stats.mac / stats.total) * 100
                    : 0,
                unknown: stats.total
                    ? (stats.unknown / stats.total) * 100
                    : 0
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

        assets.forEach((asset) => {
            const findings = asset?.findings ?? [];

            findings.forEach((finding) => {
                stats.total++;

                if (
                    finding?.status === 'RESOLVED' ||
                    finding?.status === 'VALIDATED'
                ) {
                    stats.resolved++;
                } else {
                    switch (finding?.severity) {
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

                        default:
                            break;
                    }
                }
            });
        });

        return stats;
    }, [assets]);

    useEffect(() => {
        const loadAssets = async () => {
            try {
                const response = await api.get('/asset');

                const data = Array.isArray(response.data)
                    ? response.data
                    : [];

                setAssets(data);
                setApiError(false);
            } catch (error) {
                console.error('Erro ao buscar ativos:', error);

                setAssets([]);
                setApiError(true);
            }
        };

        loadAssets();
    }, []);

    useEffect(() => {
        const values = [
            vulnStats.low,
            vulnStats.medium,
            vulnStats.high,
            vulnStats.critical,
            vulnStats.resolved
        ];

        const total = values.reduce(
            (sum, value) => sum + Number(value || 0),
            0
        );

        if (total === 0) {
            setVulnGradient(
                'conic-gradient(#e5e7eb 0% 100%)'
            );
            return;
        }

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

            current += (value / total) * 100;

            return `${colors[index]} ${start}% ${current}%`;
        });

        setVulnGradient(
            `conic-gradient(${segments.join(', ')})`
        );
    }, [vulnStats]);

    return (
        <main>
            <section className="home">
                <SideBar />

                <article className="overview">
                    <h3>Visão geral</h3>

                    {apiError && (
                        <div className="warning-message">
                            ⚠️ Não foi possível conectar ao
                            servidor. Os dados exibidos podem estar
                            indisponíveis ou desatualizados.
                        </div>
                    )}

                    <div className="overview_content">
                        <article className="overview_stats">
                            <section className="home_cards section assets_card">
                                <p>Ativos monitorados</p>

                                <h3>{assetStats.total}</h3>

                                <div className="assets_progress">
                                    <span
                                        className="assets_progress_win"
                                        style={{
                                            width: `${assetStats.percentages.windows}%`
                                        }}
                                    />

                                    <span
                                        className="assets_progress_lin"
                                        style={{
                                            width: `${assetStats.percentages.linux}%`
                                        }}
                                    />

                                    <span
                                        className="assets_progress_mac"
                                        style={{
                                            width: `${assetStats.percentages.mac}%`
                                        }}
                                    />

                                    <span
                                        className="assets_progress_unk"
                                        style={{
                                            width: `${assetStats.percentages.unknown}%`
                                        }}
                                    />
                                </div>
                            </section>

                            <section className="home_cards section">
                                <h4>
                                    Distribuição por Sistema
                                    Operacional
                                </h4>

                                <ul>
                                    <span>
                                        <li className="win-bullet">
                                            Windows
                                        </li>
                                        <h4>
                                            {Math.round(
                                                assetStats.percentages
                                                    .windows
                                            )}
                                            %
                                        </h4>
                                    </span>

                                    <span>
                                        <li className="lin-bullet">
                                            Linux
                                        </li>
                                        <h4>
                                            {Math.round(
                                                assetStats.percentages
                                                    .linux
                                            )}
                                            %
                                        </h4>
                                    </span>

                                    <span>
                                        <li className="mac-bullet">
                                            macOS
                                        </li>
                                        <h4>
                                            {Math.round(
                                                assetStats.percentages
                                                    .mac
                                            )}
                                            %
                                        </h4>
                                    </span>

                                    <span>
                                        <li className="gray-bullet">
                                            Desconhecido
                                        </li>
                                        <h4>
                                            {Math.round(
                                                assetStats.percentages
                                                    .unknown
                                            )}
                                            %
                                        </h4>
                                    </span>
                                </ul>
                            </section>
                        </article>

                        <section className="home_cards pizza section">
                            <p>
                                Gravidade das vulnerabilidades
                                encontradas
                            </p>

                            <div className="vulnerability_chart">
                                <div
                                    className="vulnerability_donut"
                                    style={{
                                        background:
                                            vulnGradient
                                    }}
                                >
                                    <div className="donut_center">
                                        <strong>
                                            {vulnStats.total}
                                        </strong>
                                        <p>
                                            Vulnerabilidades
                                        </p>
                                    </div>
                                </div>

                                <ul className="vulnerability_legend">
                                    <li>
                                        <span className="legend_color low" />
                                        Baixa{' '}
                                        <strong>
                                            {vulnStats.low}
                                        </strong>
                                    </li>

                                    <li>
                                        <span className="legend_color medium" />
                                        Média{' '}
                                        <strong>
                                            {vulnStats.medium}
                                        </strong>
                                    </li>

                                    <li>
                                        <span className="legend_color high" />
                                        Alta{' '}
                                        <strong>
                                            {vulnStats.high}
                                        </strong>
                                    </li>

                                    <li>
                                        <span className="legend_color critical" />
                                        Crítica{' '}
                                        <strong>
                                            {vulnStats.critical}
                                        </strong>
                                    </li>

                                    <li>
                                        <span className="legend_color resolved" />
                                        Resolvidas{' '}
                                        <strong>
                                            {vulnStats.resolved}
                                        </strong>
                                    </li>
                                </ul>
                            </div>
                        </section>
                    </div>

                    <section className="actions">
                        <button
                            onClick={() =>
                                navigate('novo-scan')
                            }
                            className="btn primary"
                        >
                            Nova varredura
                        </button>

                        <button
                            onClick={() =>
                                navigate('hosts')
                            }
                            className="btn secondary"
                        >
                            Visualizar ativos
                        </button>
                    </section>
                </article>
            </section>
        </main>
    );
}

export default Home;