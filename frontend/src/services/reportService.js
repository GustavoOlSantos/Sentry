import api from './api';

function buildParams(filters = {}) {
  const params = {};
  if (filters.from) params.from = filters.from;
  if (filters.to) params.to = filters.to;
  if (filters.assetId) params.assetId = filters.assetId;
  if (filters.scanId) params.scanId = filters.scanId;
  if (filters.scanGroupId) params.scanGroupId = filters.scanGroupId;
  if (filters.severity) params.severity = filters.severity;
  if (filters.type) params.type = filters.type;
  if (filters.status) params.status = filters.status;
  if (filters.policyId) params.policyId = filters.policyId;
  return params;
}

function extractList(settledResult) {
  if (settledResult.status !== 'fulfilled') return [];
  const payload = settledResult.value?.data;
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.content)) return payload.content;
  return [];
}

export async function fetchReportBundle(filters = {}) {
  const params = buildParams(filters);

  const [summary, evolution, distribution, relevant, mttr, compliance] = await Promise.all([
    api.get('/reports/summary', { params }).then((r) => r.data),
    api.get('/reports/risk/evolution', { params: { ...params, groupBy: 'DAY' } }).then((r) => r.data),
    api.get('/reports/findings/distribution', { params }).then((r) => r.data),
    api.get('/reports/findings/relevant', { params: { ...params, limit: 15 } }).then((r) => r.data),
    api.get('/reports/mttr', { params }).then((r) => r.data),
    api.get('/reports/compliance', { params }).then((r) => r.data),
  ]);

  return { summary, evolution, distribution, relevant, mttr, compliance };
}

export async function downloadReportPdf(filters = {}) {
  const params = buildParams(filters);
  const response = await api.get('/reports/pdf', {
    params,
    responseType: 'blob',
  });

  const disposition = response.headers?.['content-disposition'] || '';
  const match = disposition.match(/filename="?([^"]+)"?/i);
  const filename = match ? match[1] : `sentry-report-${filters.from || 'inicio'}-a-${filters.to || 'fim'}.pdf`;

  const blob = new Blob([response.data], { type: 'application/pdf' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

export async function fetchReportFilterOptions() {
  const [assetsRes, groupsRes, policiesRes] = await Promise.allSettled([
    api.get('/asset'),
    api.get('/scanGroup'),
    api.get('/policy'),
  ]);

  return {
    assets: extractList(assetsRes),
    scanGroups: extractList(groupsRes),
    policies: extractList(policiesRes),
  };
}
