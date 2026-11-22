import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import api from '../../services/api';

import '../../styles/home.css';
import { SideBar } from '../../components/sidebar';

function Home() {
    const navigate = useNavigate();

    const [assets, setAssets] = useState([]);
    const [apiError, setApiError] = useState(false);

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

    const getSeverityLabel = (severity) => {
        switch (severity) {
            case 'CRITICAL':
                return 'Crítica';
            case 'HIGH':
                return 'Alta';
            case 'MEDIUM':
                return 'Média';
            case 'LOW':
                return 'Baixa';
            default:
                return 'Não classificada';
        }
    };

    const getSeverityClass = (severity) => {
        switch (severity) {
            case 'CRITICAL':
                return 'critical';
            case 'HIGH':
                return 'high';
            case 'MEDIUM':
                return 'medium';
            case 'LOW':
                return 'low';
            default:
                return 'info';
        }
    };

    const isResolved = (finding) => {
        return (
            finding?.status === 'RESOLVED' ||
            finding?.status === 'VALIDATED'
        );
    };

    const assetStats = useMemo(() => {
        const stats = {
            total: assets.length,
            online: 0,
            offline: 0,
            unknownStatus: 0,
            windows: 0,
            linux: 0,
            mac: 0,
            unknown: 0
        };

        assets.forEach((asset) => {
            const category = getOSCategory(asset?.operatingSystem);
            stats[category]++;

            const status = String(
                asset?.status ??
                asset?.state ??
                asset?.availability ??
                ''
            ).toUpperCase();

            if (
                status === 'ONLINE' ||
                status === 'UP' ||
                status === 'ACTIVE'
            ) {
                stats.online++;
            } else if (
                status === 'OFFLINE' ||
                status === 'DOWN' ||
                status === 'INACTIVE'
            ) {
                stats.offline++;
            } else {
                stats.unknownStatus++;
            }
        });

        const total = stats.total || 1;

        return {
            ...stats,
            percentages: {
                windows: (stats.windows / total) * 100,
                linux: (stats.linux / total) * 100,
                mac: (stats.mac / total) * 100,
                unknown: (stats.unknown / total) * 100
            }
        };
    }, [assets]);

    const findingStats = useMemo(() => {
        const stats = {
            total: 0,
            active: 0,
            resolved: 0,
            low: 0,
            medium: 0,
            high: 0,
            critical: 0
        };

        assets.forEach((asset) => {
            const findings = asset?.findings ?? [];

            findings.forEach((finding) => {
                stats.total++;

                if (isResolved(finding)) {
                    stats.resolved++;
                    return;
                }

                stats.active++;

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
            });
        });

        return stats;
    }, [assets]);

    const priorityFindings = useMemo(() => {
        const severityWeight = {
            CRITICAL: 4,
            HIGH: 3,
            MEDIUM: 2,
            LOW: 1
        };

        const findings = [];

        assets.forEach((asset) => {
            const assetFindings = asset?.findings ?? [];

            assetFindings.forEach((finding) => {
                if (isResolved(finding)) {
                    return;
                }

                findings.push({
                    ...finding,
                    asset
                });
            });
        });

        return findings
            .sort((a, b) => {
                const severityA = severityWeight[a?.severity] ?? 0;
                const severityB = severityWeight[b?.severity] ?? 0;

                return severityB - severityA;
            })
            .slice(0, 5);
    }, [assets]);

    const osDistribution = [
        {
            key: 'windows',
            label: 'Windows',
            value: assetStats.windows,
            percentage: assetStats.percentages.windows
        },
        {
            key: 'linux',
            label: 'Linux',
            value: assetStats.linux,
            percentage: assetStats.percentages.linux
        },
        {
            key: 'mac',
            label: 'macOS',
            value: assetStats.mac,
            percentage: assetStats.percentages.mac
        },
        {
            key: 'unknown',
            label: 'Desconhecido',
            value: assetStats.unknown,
            percentage: assetStats.percentages.unknown
        }
    ];

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

    useEffect(() => {
        loadAssets();
    }, []);

    return (
        <main>
            <section className="home">
                <SideBar />

                <article className="overview">
                    <header className="dashboard_header">
                        <div>
                            <h3>Visão geral</h3>
                            <p>
                                Acompanhe o estado atual dos ativos e dos
                                principais achados de segurança.
                            </p>
                        </div>

                        <button
                            className="btn primary dashboard_scan_btn"
                            onClick={() => navigate('novo-scan')}
                        >
                            Nova varredura
                        </button>
                    </header>

                    {apiError && (
                        <div className="warning-message">
                            ⚠️ Não foi possível conectar ao servidor. Os dados
                            exibidos podem estar indisponíveis ou desatualizados.
                        </div>
                    )}

                    <section className="dashboard_metrics">
                        <article className="dashboard_metric">
                            <div className="metric_icon asset_icon">
                                <i className="fa-solid fa-server" />
                            </div>

                            <div className="metric_content">
                                <span>Ativos</span>
                                <strong>{assetStats.total}</strong>
                                <small>ativos monitorados</small>
                            </div>
                        </article>

                        <article className="dashboard_metric">
                            <div className="metric_icon online_icon">
                                <i className="fa-solid fa-circle-check" />
                            </div>

                            <div className="metric_content">
                                <span>Online</span>
                                <strong>{assetStats.online}</strong>
                                <small>
                                    {assetStats.total
                                        ? `${Math.round(
                                              (assetStats.online /
                                                  assetStats.total) *
                                                  100
                                          )}% dos ativos`
                                        : 'nenhum ativo identificado'}
                                </small>
                            </div>
                        </article>

                        <article className="dashboard_metric">
                            <div className="metric_icon finding_icon">
                                <i className="fa-solid fa-triangle-exclamation" />
                            </div>

                            <div className="metric_content">
                                <span>Findings ativos</span>
                                <strong>{findingStats.active}</strong>
                                <small>
                                    {findingStats.resolved} resolvidos
                                </small>
                            </div>
                        </article>

                        <article className="dashboard_metric">
                            <div className="metric_icon critical_icon">
                                <i className="fa-solid fa-shield-halved" />
                            </div>

                            <div className="metric_content">
                                <span>Críticos</span>
                                <strong>{findingStats.critical}</strong>
                                <small>requerem atenção</small>
                            </div>
                        </article>
                    </section>

                    <section className="dashboard_grid">
                        <article className="dashboard_card security_card">
                            <div className="card_header">
                                <div>
                                    <h4>Panorama de segurança</h4>
                                    <p>
                                        Distribuição dos findings ativos por
                                        severidade.
                                    </p>
                                </div>

                                <button
                                    className="card_link"
                                    onClick={() => navigate('/board')}
                                >
                                    Ver findings
                                </button>
                            </div>

                            <div className="severity_overview">
                                <div className="severity_total">
                                    <strong>{findingStats.active}</strong>
                                    <span>Findings ativos</span>
                                </div>

                                <div className="severity_bars">
                                    <div className="severity_row">
                                        <div className="severity_label">
                                            <span className="severity_dot critical" />
                                            <span>Crítica</span>
                                            <strong>
                                                {findingStats.critical}
                                            </strong>
                                        </div>

                                        <div className="severity_bar">
                                            <span
                                                className="critical"
                                                style={{
                                                    width: `${
                                                        findingStats.active
                                                            ? (findingStats.critical /
                                                                  findingStats.active) *
                                                              100
                                                            : 0
                                                    }%`
                                                }}
                                            />
                                        </div>
                                    </div>

                                    <div className="severity_row">
                                        <div className="severity_label">
                                            <span className="severity_dot high" />
                                            <span>Alta</span>
                                            <strong>
                                                {findingStats.high}
                                            </strong>
                                        </div>

                                        <div className="severity_bar">
                                            <span
                                                className="high"
                                                style={{
                                                    width: `${
                                                        findingStats.active
                                                            ? (findingStats.high /
                                                                  findingStats.active) *
                                                              100
                                                            : 0
                                                    }%`
                                                }}
                                            />
                                        </div>
                                    </div>

                                    <div className="severity_row">
                                        <div className="severity_label">
                                            <span className="severity_dot medium" />
                                            <span>Média</span>
                                            <strong>
                                                {findingStats.medium}
                                            </strong>
                                        </div>

                                        <div className="severity_bar">
                                            <span
                                                className="medium"
                                                style={{
                                                    width: `${
                                                        findingStats.active
                                                            ? (findingStats.medium /
                                                                  findingStats.active) *
                                                              100
                                                            : 0
                                                    }%`
                                                }}
                                            />
                                        </div>
                                    </div>

                                    <div className="severity_row">
                                        <div className="severity_label">
                                            <span className="severity_dot low" />
                                            <span>Baixa</span>
                                            <strong>
                                                {findingStats.low}
                                            </strong>
                                        </div>

                                        <div className="severity_bar">
                                            <span
                                                className="low"
                                                style={{
                                                    width: `${
                                                        findingStats.active
                                                            ? (findingStats.low /
                                                                  findingStats.active) *
                                                              100
                                                            : 0
                                                    }%`
                                                }}
                                            />
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div className="resolved_summary">
                                <span>
                                    <span className="resolved_dot" />
                                    Findings resolvidos
                                </span>

                                <strong>{findingStats.resolved}</strong>
                            </div>
                        </article>

                        <article className="dashboard_card">
                            <div className="card_header">
                                <div>
                                    <h4>Ativos por sistema</h4>
                                    <p>
                                        Distribuição dos sistemas operacionais
                                        identificados.
                                    </p>
                                </div>
                            </div>

                            <div className="os_distribution">
                                <div className="os_progress">
                                    {osDistribution.map((item) => (
                                        <span
                                            key={item.key}
                                            className={`os_segment ${item.key}`}
                                            style={{
                                                width: `${item.percentage}%`
                                            }}
                                        />
                                    ))}
                                </div>

                                <div className="os_list">
                                    {osDistribution.map((item) => (
                                        <div
                                            className="os_item"
                                            key={item.key}
                                        >
                                            <div>
                                                <span
                                                    className={`os_dot ${item.key}`}
                                                />
                                                <span>{item.label}</span>
                                            </div>

                                            <strong>
                                                {item.value}
                                            </strong>

                                            <small>
                                                {Math.round(item.percentage)}%
                                            </small>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </article>
                    </section>

                    <section className="dashboard_bottom_grid">
                        <article className="dashboard_card priority_card">
                            <div className="card_header">
                                <div>
                                    <h4>Findings prioritários</h4>
                                    <p>
                                        Os achados que merecem atenção
                                        primeiro.
                                    </p>
                                </div>

                                <button
                                    className="card_link"
                                    onClick={() => navigate('/board')}
                                >
                                    Ver todos
                                </button>
                            </div>

                            {priorityFindings.length > 0 ? (
                                <div className="priority_list">
                                    {priorityFindings.map((finding, index) => {
                                        const severityClass =
                                            getSeverityClass(
                                                finding?.severity
                                            );

                                        const assetName =
                                            finding?.asset?.hostname ||
                                            finding?.asset?.ip ||
                                            finding?.asset?.address ||
                                            'Ativo não identificado';

                                        return (
                                            <div
                                                className="priority_item"
                                                key={
                                                    finding?.id ??
                                                    `${assetName}-${index}`
                                                }
                                            >
                                                <div
                                                    className={`priority_indicator ${severityClass}`}
                                                />

                                                <div className="priority_info">
                                                    <strong>
                                                        {finding?.title ||
                                                            finding?.name ||
                                                            'Finding sem título'}
                                                    </strong>

                                                    <span>
                                                        {assetName}
                                                    </span>
                                                </div>

                                                <span
                                                    className={`severity_badge ${severityClass}`}
                                                >
                                                    {getSeverityLabel(
                                                        finding?.severity
                                                    )}
                                                </span>
                                            </div>
                                        );
                                    })}
                                </div>
                            ) : (
                                <div className="empty_state">
                                    <div className="empty_icon">
                                        <i className="fa-solid fa-shield-check" />
                                    </div>

                                    <strong>
                                        Nenhum finding ativo
                                    </strong>

                                    <p>
                                        Não há achados pendentes para exibir
                                        no momento.
                                    </p>
                                </div>
                            )}
                        </article>

                        <article className="dashboard_card activity_card">
                            <div className="card_header">
                                <div>
                                    <h4>Acesso rápido</h4>
                                    <p>
                                        Ações mais utilizadas no Sentry.
                                    </p>
                                </div>
                            </div>

                            <div className="quick_actions">
                                <button
                                    className="quick_action"
                                    onClick={() => navigate('novo-scan')}
                                >
                                    <span className="quick_action_icon primary">
                                        <i className="fa-solid fa-magnifying-glass-plus" />
                                    </span>

                                    <span>
                                        <strong>Nova varredura</strong>
                                        <small>
                                            Iniciar uma nova análise
                                        </small>
                                    </span>

                                    <i className="fa-solid fa-chevron-right" />
                                </button>

                                <button
                                    className="quick_action"
                                    onClick={() => navigate('hosts')}
                                >
                                    <span className="quick_action_icon secondary">
                                        <i className="fa-solid fa-server" />
                                    </span>

                                    <span>
                                        <strong>Visualizar ativos</strong>
                                        <small>
                                            Consultar hosts monitorados
                                        </small>
                                    </span>

                                    <i className="fa-solid fa-chevron-right" />
                                </button>

                                <button
                                    className="quick_action"
                                    onClick={() => navigate('/board')}
                                >
                                    <span className="quick_action_icon warning">
                                        <i className="fa-solid fa-bug" />
                                    </span>

                                    <span>
                                        <strong>Consultar findings</strong>
                                        <small>
                                            Analisar problemas encontrados
                                        </small>
                                    </span>

                                    <i className="fa-solid fa-chevron-right" />
                                </button>
                            </div>
                        </article>
                    </section>
                </article>
            </section>
        </main>
    );
}

export default Home;