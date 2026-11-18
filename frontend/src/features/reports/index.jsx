import { useCallback, useEffect, useMemo, useState } from 'react';
import { downloadReportPdf, fetchReportBundle, fetchReportFilterOptions } from '../../services/reportService';

import { getApiErrorMessage } from '../../services/apiError';

import { SideBar } from "../../components/sidebar";

import '../../styles/reports.css';

const SEVERITY_LABELS = {
  CRITICAL: 'Crítica',
  HIGH: 'Alta',
  MEDIUM: 'Média',
  LOW: 'Baixa',
  INFO: 'Informativa',
};

const SEVERITY_COLORS = {
  CRITICAL: '#ef4444',
  HIGH: '#f97316',
  MEDIUM: '#eab308',
  LOW: '#22c55e',
  INFO: '#3b82f6',
};

const TYPE_LABELS = {
  VULNERABILITY: 'Vulnerabilidade',
  POLICY_VIOLATION: 'Violação de política',
};

const TYPE_COLORS = {
  VULNERABILITY: '#3b82f6',
  POLICY_VIOLATION: '#8b5cf6',
};

const STATUS_LABELS = {
  OPEN: 'Aberto',
  IN_PROGRESS: 'Em andamento',
  FAILED_VALIDATION: 'Reteste falhou',
  RESOLVED: 'Resolvido',
  JUSTIFIED: 'Justificado',
  VALIDATED: 'Validado',
};

function defaultDateRange() {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - 29);
  return {
    from: start.toISOString().slice(0, 10),
    to: end.toISOString().slice(0, 10),
  };
}

function formatDateBr(isoDate) {
  if (!isoDate) return '-';
  const parts = String(isoDate).slice(0, 10).split('-');
  if (parts.length !== 3) return isoDate;
  return `${parts[2]}/${parts[1]}/${parts[0]}`;
}

function formatShortDate(isoDate) {
  if (!isoDate) return '';
  const parts = String(isoDate).slice(0, 10).split('-');
  return parts.length === 3 ? `${parts[2]}/${parts[1]}` : isoDate;
}

function DonutSvg({ items, total, centerLabel = 'findings' }) {
  const radius = 44;
  const strokeWidth = 14;
  const circumference = 2 * Math.PI * radius;

  const positive = items.filter((i) => i.value > 0);
  let cumulative = 0;

  return (
    <svg width="132" height="132" viewBox="0 0 120 120" aria-label="Gráfico de distribuição">
      <circle
        cx="60"
        cy="60"
        r={radius}
        fill="none"
        stroke="#262f40"
        strokeWidth={strokeWidth}
      />
      {total > 0 &&
        positive.map((item) => {
          const fraction = item.value / total;
          const dash = fraction * circumference;
          const offset = -cumulative * circumference;
          cumulative += fraction;

          return (
            <circle
              key={item.key}
              cx="60"
              cy="60"
              r={radius}
              fill="none"
              stroke={item.color}
              strokeWidth={strokeWidth}
              strokeDasharray={`${dash} ${circumference}`}
              strokeDashoffset={offset}
              transform="rotate(-90 60 60)"
            />
          );
        })}
      <text x="60" y="57" textAnchor="middle" fill="#ffffff" fontSize="18" fontWeight="700">
        {total}
      </text>
      <text x="60" y="72" textAnchor="middle" fill="#9ca3af" fontSize="9.5">
        {centerLabel}
      </text>
    </svg>
  );
}

function EvolutionLineChart({ points }) {
  if (!points || points.length === 0) {
    return <div className="reports-empty">Sem dados suficientes para exibir a evolução diária.</div>;
  }

  const hasAnyEvent = points.some((p) => p.detected > 0 || p.resolved > 0 || p.pending > 0);
  if (!hasAnyEvent) {
    return <div className="reports-empty">Sem dados suficientes: nenhuma movimentação registrada no período.</div>;
  }

  const width = 760;
  const height = 220;
  const padLeft = 38;
  const padRight = 18;
  const padTop = 18;
  const padBottom = 30;
  const plotW = width - padLeft - padRight;
  const plotH = height - padTop - padBottom;

  const maxVal = Math.max(
    4,
    ...points.map((p) => Math.max(p.detected || 0, p.resolved || 0, p.pending || 0))
  );

  const xPos = (idx) =>
    points.length === 1 ? padLeft + plotW / 2 : padLeft + (plotW * idx) / (points.length - 1);
  const yPos = (val) => padTop + plotH - (val / maxVal) * plotH;

  const makePath = (getter) =>
    points.map((p, idx) => `${idx === 0 ? 'M' : 'L'} ${xPos(idx).toFixed(1)} ${yPos(getter(p)).toFixed(1)}`).join(' ');

  const step = Math.max(1, Math.ceil(points.length / 8));

  return (
    <div>
      <div className="reports-legend" style={{ marginBottom: '0.6rem' }}>
        <span className="reports-legend-item">
          <span className="reports-legend-dot" style={{ background: '#3b82f6' }} />
          Detectados no dia
        </span>
        <span className="reports-legend-item">
          <span className="reports-legend-dot" style={{ background: '#22c55e' }} />
          Concluídos (validados)
        </span>
        <span className="reports-legend-item">
          <span className="reports-legend-dot" style={{ background: '#f97316' }} />
          Pendentes ao fim do dia
        </span>
      </div>

      <svg viewBox={`0 0 ${width} ${height}`} style={{ width: '100%', height: 'auto', display: 'block' }}>
        {[0, 0.25, 0.5, 0.75, 1].map((t) => {
          const y = padTop + plotH * (1 - t);
          const label = Math.round(maxVal * t);
          return (
            <g key={t}>
              <line x1={padLeft} y1={y} x2={width - padRight} y2={y} stroke="#242e42" strokeWidth="1" />
              <text x={padLeft - 6} y={y + 3} textAnchor="end" fill="#9ca3af" fontSize="10">
                {label}
              </text>
            </g>
          );
        })}

        <path d={makePath((p) => p.pending || 0)} fill="none" stroke="#f97316" strokeWidth="2.2" />
        <path d={makePath((p) => p.detected || 0)} fill="none" stroke="#3b82f6" strokeWidth="2.2" />
        <path d={makePath((p) => p.resolved || 0)} fill="none" stroke="#22c55e" strokeWidth="2.2" />

        {points.map((p, idx) =>
          idx % step === 0 || idx === points.length - 1 ? (
            <text
              key={p.date}
              x={xPos(idx)}
              y={height - 8}
              textAnchor="middle"
              fill="#9ca3af"
              fontSize="10"
            >
              {formatShortDate(p.date)}
            </text>
          ) : null
        )}
      </svg>
    </div>
  );
}

export default function ReportsPage() {
  const initialRange = useMemo(() => defaultDateRange(), []);

  const [draftFilters, setDraftFilters] = useState({
    from: initialRange.from,
    to: initialRange.to,
    assetId: '',
    scanGroupId: '',
    type: '',
    severity: '',
    policyId: '',
  });

  const [appliedFilters, setAppliedFilters] = useState(draftFilters);
  const [options, setOptions] = useState({ assets: [], scanGroups: [], policies: [] });
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState('');

  useEffect(() => {
    fetchReportFilterOptions().then(setOptions);
  }, []);

  const loadReports = useCallback(async (filtersToLoad) => {
    setLoading(true);
    setError('');
    try {
      const bundle = await fetchReportBundle(filtersToLoad);
      setData(bundle);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Não foi possível carregar os dados de relatórios.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadReports(appliedFilters);
  }, [appliedFilters, loadReports]);

  const handleFieldChange = (field, value) => {
    setDraftFilters((prev) => ({ ...prev, [field]: value }));
  };

  const handleApplyFilters = (e) => {
    e.preventDefault();
    setExportError('');
    setAppliedFilters({ ...draftFilters });
  };

  const handleResetFilters = () => {
    const range = defaultDateRange();
    const reset = {
      from: range.from,
      to: range.to,
      assetId: '',
      scanGroupId: '',
      type: '',
      severity: '',
      policyId: '',
    };
    setDraftFilters(reset);
    setExportError('');
    setAppliedFilters(reset);
  };

  const handleExportPdf = async () => {
    setExporting(true);
    setExportError('');
    try {
      await downloadReportPdf(appliedFilters);
    } catch (err) {
      setExportError(getApiErrorMessage(err, 'Falha ao exportar o relatório em PDF.'));
    } finally {
      setExporting(false);
    }
  };

  const summary = data?.summary;
  const distribution = data?.distribution;
  const evolution = data?.evolution;
  const mttr = data?.mttr;
  const compliance = data?.compliance;
  const relevant = data?.relevant;

  const severityItems = (distribution?.bySeverity || []).map((row) => ({
    key: row.severity,
    label: SEVERITY_LABELS[row.severity] || row.severity,
    value: row.count,
    percentage: row.percentage,
    color: SEVERITY_COLORS[row.severity] || '#64748b',
  }));

  const typeItems = (distribution?.byType || []).map((row) => ({
    key: row.type,
    label: TYPE_LABELS[row.type] || row.type,
    value: row.count,
    percentage: row.percentage,
    color: TYPE_COLORS[row.type] || '#64748b',
  }));

  const maxMttrHours = Math.max(
    1,
    ...(mttr?.bySeverity || []).map((r) => r.averageHours || 0)
  );

  return (
    <main className="report-main">
      <SideBar />

      <div className="reports-page">
        <header className="reports-header">
          <div className="reports-title-group">
            <h1>Relatórios de Segurança</h1>
            <p className="reports-subtitle">
              Indicadores consolidados de vulnerabilidades, tempo de resposta (MTTR) e conformidade com políticas.
            </p>
            {summary?.scopeDescription && (
              <span className="reports-scope-badge">Escopo: {summary.scopeDescription}</span>
            )}
          </div>

          <button
            type="button"
            className="reports-pdf-btn"
            disabled={exporting || loading}
            onClick={handleExportPdf}
          >
            {exporting ? 'Gerando PDF...' : 'Exportar PDF'}
          </button>
        </header>

        {exportError && (
          <div className="reports-alert-error">
            <span>{exportError}</span>
          </div>
        )}

        {/* Barra de Filtros */}
        <form className="reports-filters-card" onSubmit={handleApplyFilters}>
          <div className="reports-filters-grid">
            <div className="reports-field">
              <label htmlFor="rep-from">Data inicial</label>
              <input
                id="rep-from"
                type="date"
                value={draftFilters.from}
                onChange={(e) => handleFieldChange('from', e.target.value)}
              />
            </div>

            <div className="reports-field">
              <label htmlFor="rep-to">Data final</label>
              <input
                id="rep-to"
                type="date"
                value={draftFilters.to}
                onChange={(e) => handleFieldChange('to', e.target.value)}
              />
            </div>

            <div className="reports-field">
              <label htmlFor="rep-asset">Ativo</label>
              <select
                id="rep-asset"
                value={draftFilters.assetId}
                onChange={(e) => handleFieldChange('assetId', e.target.value)}
              >
                <option value="">Todos os ativos</option>
                {options.assets.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.hostname ? `${a.ipAddress} (${a.hostname})` : a.ipAddress}
                  </option>
                ))}
              </select>
            </div>

            <div className="reports-field">
              <label htmlFor="rep-group">Grupo de scans</label>
              <select
                id="rep-group"
                value={draftFilters.scanGroupId}
                onChange={(e) => handleFieldChange('scanGroupId', e.target.value)}
              >
                <option value="">Todos os grupos</option>
                {options.scanGroups.map((g) => (
                  <option key={g.id} value={g.id}>
                    {g.target || String(g.id).slice(0, 8)}
                  </option>
                ))}
              </select>
            </div>

            <div className="reports-field">
              <label htmlFor="rep-type">Tipo</label>
              <select
                id="rep-type"
                value={draftFilters.type}
                onChange={(e) => handleFieldChange('type', e.target.value)}
              >
                <option value="">Todos os tipos</option>
                <option value="VULNERABILITY">Vulnerabilidade</option>
                <option value="POLICY_VIOLATION">Violação de política</option>
              </select>
            </div>

            <div className="reports-field">
              <label htmlFor="rep-severity">Severidade</label>
              <select
                id="rep-severity"
                value={draftFilters.severity}
                onChange={(e) => handleFieldChange('severity', e.target.value)}
              >
                <option value="">Todas as severidades</option>
                <option value="CRITICAL">Crítica</option>
                <option value="HIGH">Alta</option>
                <option value="MEDIUM">Média</option>
                <option value="LOW">Baixa</option>
                <option value="INFO">Informativa</option>
              </select>
            </div>

            <div className="reports-field">
              <label htmlFor="rep-policy">Política</label>
              <select
                id="rep-policy"
                value={draftFilters.policyId}
                onChange={(e) => handleFieldChange('policyId', e.target.value)}
              >
                <option value="">Todas as políticas</option>
                {options.policies.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="reports-filter-actions">
              <button type="submit" className="reports-apply-btn" disabled={loading}>
                Aplicar filtros
              </button>
              <button type="button" className="reports-clear-btn" onClick={handleResetFilters}>
                Limpar
              </button>
            </div>
          </div>
        </form>

        {loading && <div className="reports-loading">Carregando indicadores e gráficos do relatório...</div>}

        {!loading && error && (
          <div className="reports-alert-error">
            <span>{error}</span>
            <button type="button" className="reports-apply-btn" onClick={() => loadReports(appliedFilters)}>
              Tentar novamente
            </button>
          </div>
        )}

        {!loading && !error && summary && (
          <>
            {/* Cards de Indicadores */}
            <section className="reports-kpi-grid" aria-label="Indicadores principais">
              <div className="reports-kpi-card">
                <span className="reports-kpi-label">Total de findings</span>
                <span className="reports-kpi-value">{summary.totalFindings}</span>
                <span className="reports-kpi-note">
                  {summary.detectedInPeriod} no período · {summary.carriedOver} anteriores
                </span>
              </div>

              <div className="reports-kpi-card kpi-critical">
                <span className="reports-kpi-label">Críticos</span>
                <span className="reports-kpi-value">{summary.critical}</span>
                <span className="reports-kpi-note">{summary.pendingCritical} ainda pendentes</span>
              </div>

              <div className="reports-kpi-card kpi-open">
                <span className="reports-kpi-label">Abertos</span>
                <span className="reports-kpi-value">{summary.open}</span>
                <span className="reports-kpi-note">{summary.inProgress} em andamento</span>
              </div>

              <div className="reports-kpi-card kpi-mttr">
                <span className="reports-kpi-label">MTTR médio</span>
                <span className="reports-kpi-value">
                  {summary.mttr?.averageLabel || 'Sem dados'}
                </span>
                <span className="reports-kpi-note">
                  {summary.mttr?.completedFindings || 0} findings validados
                </span>
              </div>

              <div className="reports-kpi-card kpi-compliance">
                <span className="reports-kpi-label">Compliance</span>
                <span className="reports-kpi-value">
                  {summary.compliance?.percentage != null ? `${summary.compliance.percentage}%` : 'Sem dados'}
                </span>
                <span className="reports-kpi-note">
                  {summary.compliance?.evaluations || 0} avaliações de política
                </span>
              </div>
            </section>

            {/* Gráficos */}
            <section className="reports-charts-grid">
              <div className="reports-card span-12">
                <div className="reports-card-header">
                  <h2>Evolução diária de findings</h2>
                  <span className="reports-card-caption">
                    Detectados, concluídos e pendentes ao fim de cada dia
                  </span>
                </div>
                <EvolutionLineChart points={evolution?.points || []} />
              </div>

              <div className="reports-card span-6">
                <div className="reports-card-header">
                  <h2>Findings por severidade</h2>
                  <span className="reports-card-caption">Total: {distribution?.total || 0}</span>
                </div>
                {distribution?.total > 0 ? (
                  <div className="reports-donut-layout">
                    <DonutSvg items={severityItems} total={distribution.total} />
                    <div className="reports-donut-list">
                      {severityItems.map((item) => (
                        <div key={item.key} className="reports-donut-row">
                          <span className="reports-legend-item">
                            <span className="reports-legend-dot" style={{ background: item.color }} />
                            {item.label}
                          </span>
                          <strong>
                            {item.value} ({item.percentage}%)
                          </strong>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : (
                  <div className="reports-empty">Sem dados suficientes para o escopo selecionado.</div>
                )}
              </div>

              <div className="reports-card span-6">
                <div className="reports-card-header">
                  <h2>Findings por tipo</h2>
                  <span className="reports-card-caption">Vulnerabilidades vs. violações de política</span>
                </div>
                {distribution?.total > 0 ? (
                  <div className="reports-donut-layout">
                    <DonutSvg items={typeItems} total={distribution.total} />
                    <div className="reports-donut-list">
                      {typeItems.map((item) => (
                        <div key={item.key} className="reports-donut-row">
                          <span className="reports-legend-item">
                            <span className="reports-legend-dot" style={{ background: item.color }} />
                            {item.label}
                          </span>
                          <strong>
                            {item.value} ({item.percentage}%)
                          </strong>
                        </div>
                      ))}
                    </div>
                  </div>
                ) : (
                  <div className="reports-empty">Sem dados suficientes para o escopo selecionado.</div>
                )}
              </div>

              <div className="reports-card span-6">
                <div className="reports-card-header">
                  <h2>Compliance por política</h2>
                  <span className="reports-card-caption">Taxa de conformidade na última avaliação</span>
                </div>
                {compliance?.evaluationsAvailable && compliance.byPolicy?.length > 0 ? (
                  <div className="reports-hbar-list">
                    {compliance.byPolicy.map((pol) => {
                      const pct = pol.compliancePercentage ?? 0;
                      return (
                        <div key={pol.policyId} className="reports-hbar-row">
                          <div className="reports-hbar-meta">
                            <span>{pol.policyName}</span>
                            <strong>
                              {pol.compliancePercentage != null ? `${pol.compliancePercentage}%` : 'Sem dados'} (
                              {pol.compliant}/{pol.compliant + pol.nonCompliant})
                            </strong>
                          </div>
                          <div className="reports-hbar-track">
                            <div
                              className="reports-hbar-fill"
                              style={{
                                width: `${Math.min(100, Math.max(0, pct))}%`,
                                background: pct >= 80 ? '#22c55e' : pct >= 50 ? '#eab308' : '#ef4444',
                              }}
                            />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="reports-empty">
                    Sem dados suficientes: nenhuma avaliação de política registrada no período.
                  </div>
                )}
              </div>

              <div className="reports-card span-6">
                <div className="reports-card-header">
                  <h2>MTTR por severidade</h2>
                  <span className="reports-card-caption">Tempo médio entre detecção e validação</span>
                </div>
                {mttr?.completedFindings > 0 ? (
                  <div className="reports-hbar-list">
                    {(mttr.bySeverity || []).map((row) => {
                      const hours = row.averageHours || 0;
                      const widthPct = row.averageSeconds != null ? Math.max(6, (hours / maxMttrHours) * 100) : 0;
                      return (
                        <div key={row.severity} className="reports-hbar-row">
                          <div className="reports-hbar-meta">
                            <span>
                              {SEVERITY_LABELS[row.severity] || row.severity} ({row.completedCount})
                            </span>
                            <strong>{row.averageLabel || 'Sem dados'}</strong>
                          </div>
                          <div className="reports-hbar-track">
                            <div
                              className="reports-hbar-fill"
                              style={{
                                width: `${Math.min(100, widthPct)}%`,
                                background: SEVERITY_COLORS[row.severity] || '#3b82f6',
                              }}
                            />
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="reports-empty">
                    Sem dados suficientes: nenhum finding validado no período selecionado.
                  </div>
                )}
              </div>
            </section>

            {/* Tabelas */}
            <section className="reports-card">
              <div className="reports-card-header">
                <h2>Findings com maior severidade</h2>
                <span className="reports-card-caption">
                  Exibindo {relevant?.findings?.length || 0} de {relevant?.totalInScope || 0} no escopo
                </span>
              </div>
              {relevant?.findings?.length > 0 ? (
                <div className="reports-table-wrap">
                  <table className="reports-table">
                    <thead>
                      <tr>
                        <th>Título</th>
                        <th>Severidade</th>
                        <th>Tipo</th>
                        <th>Status</th>
                        <th>Ativo</th>
                        <th>Detecção</th>
                      </tr>
                    </thead>
                    <tbody>
                      {relevant.findings.map((f) => (
                        <tr key={f.id}>
                          <td>{f.title || f.code}</td>
                          <td>
                            <span className={`rep-badge rep-sev-${f.severity}`}>
                              {SEVERITY_LABELS[f.severity] || f.severity}
                            </span>
                          </td>
                          <td>{TYPE_LABELS[f.type] || f.type}</td>
                          <td>{STATUS_LABELS[f.status] || f.status}</td>
                          <td>
                            {f.assetIp}
                            {f.port ? `:${f.port}` : ''}
                          </td>
                          <td>{formatDateBr(f.detectedAt)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <div className="reports-empty">Sem dados suficientes: nenhum finding encontrado para os filtros aplicados.</div>
              )}
            </section>

            <section className="reports-charts-grid">
              <div className="reports-card span-6">
                <div className="reports-card-header">
                  <h2>Ativos com mais ocorrências</h2>
                  <span className="reports-card-caption">Top ativos ordenados por criticidade e volume</span>
                </div>
                {distribution?.topAssets?.length > 0 ? (
                  <div className="reports-table-wrap">
                    <table className="reports-table">
                      <thead>
                        <tr>
                          <th>Ativo</th>
                          <th className="reports-num">Total</th>
                          <th className="reports-num">Crít.</th>
                          <th className="reports-num">Alta</th>
                          <th className="reports-num">Pendentes</th>
                          <th className="reports-num">Validados</th>
                        </tr>
                      </thead>
                      <tbody>
                        {distribution.topAssets.map((a) => (
                          <tr key={a.assetId}>
                            <td>{a.hostname ? `${a.ipAddress} (${a.hostname})` : a.ipAddress}</td>
                            <td className="reports-num">{a.total}</td>
                            <td className="reports-num">{a.bySeverity?.CRITICAL || 0}</td>
                            <td className="reports-num">{a.bySeverity?.HIGH || 0}</td>
                            <td className="reports-num">{a.pending}</td>
                            <td className="reports-num">{a.validated}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="reports-empty">Nenhum ativo com ocorrências no período.</div>
                )}
              </div>

              <div className="reports-card span-6">
                <div className="reports-card-header">
                  <h2>Políticas com maior quantidade de violações</h2>
                  <span className="reports-card-caption">Regras não conformes na última avaliação</span>
                </div>
                {compliance?.topViolations?.length > 0 ? (
                  <div className="reports-table-wrap">
                    <table className="reports-table">
                      <thead>
                        <tr>
                          <th>Regra</th>
                          <th>Política</th>
                          <th>Severidade</th>
                          <th className="reports-num">Ativos afetados</th>
                        </tr>
                      </thead>
                      <tbody>
                        {compliance.topViolations.map((v) => (
                          <tr key={v.ruleId}>
                            <td>{v.ruleName}</td>
                            <td>{v.policyName}</td>
                            <td>
                              <span className={`rep-badge rep-sev-${v.severity}`}>
                                {SEVERITY_LABELS[v.severity] || v.severity}
                              </span>
                            </td>
                            <td className="reports-num">{v.affectedAssets}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <div className="reports-empty">Nenhuma violação de política registrada no período.</div>
                )}
              </div>
            </section>

            {summary.limitations?.length > 0 && (
              <section className="reports-card">
                <div className="reports-card-header">
                  <h2>Notas metodológicas</h2>
                </div>
                <ul className="reports-notes">
                  {summary.limitations.map((note, idx) => (
                    <li key={idx}>{note}</li>
                  ))}
                </ul>
              </section>
            )}
          </>
        )}
      </div>
    </main>
  );
}
