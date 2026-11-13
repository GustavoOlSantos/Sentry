export const STATUS_LABELS = {
    PENDING: "Pendente",
    RUNNING: "Em execução",
    COMPLETED: "Concluído",
    FAILED: "Falhou",
    NOT_FOUND: "Host não encontrado",
    CANCELLED: "Cancelado",
    PARTIAL: "Parcial",
    ACCEPTED: "Aceito",
};

/** Status em que a varredura ainda está andando (a tela atualiza sozinha). */
export const ACTIVE_STATUSES = ["PENDING", "RUNNING"];

export const isActive = (status) => ACTIVE_STATUSES.includes(status);

export const SEVERITY_LABELS = {
    CRITICAL: "Crítica",
    HIGH: "Alta",
    MEDIUM: "Média",
    LOW: "Baixa",
    INFO: "Info",
    NONE: "Nenhuma",
};

export const FINDING_STATUS_LABELS = {
    OPEN: "Aberto",
    IN_PROGRESS: "Em andamento",
    RESOLVED: "Resolvido",
    VALIDATED: "Validado",
    JUSTIFIED: "Justificado",
    FAILED_VALIDATION: "Reteste falhou",
};

export const FINDING_TYPE_LABELS = {
    VULNERABILITY: "Vulnerabilidade",
    POLICY_VIOLATION: "Política",
};

/** "a1b2c3d4" — os 8 primeiros caracteres bastam para identificar um grupo/scan na tela. */
export function shortId(id) {
    return id ? String(id).slice(0, 8) : "—";
}

/** 45 → "45s", 192 → "3m 12s", 3900 → "1h 05m". */
export function formatDuration(seconds) {
    if (seconds === null || seconds === undefined) return "—";

    const total = Math.max(0, Math.round(seconds));
    const hours = Math.floor(total / 3600);
    const minutes = Math.floor((total % 3600) / 60);
    const secs = total % 60;

    if (hours > 0) return `${hours}h ${String(minutes).padStart(2, "0")}m`;
    if (minutes > 0) return `${minutes}m ${String(secs).padStart(2, "0")}s`;
    return `${secs}s`;
}

/**
 * Duração a exibir: a que o servidor calculou, ou — com a varredura em andamento —
 * o tempo decorrido desde o início, usando `now` (ms) para o contador ao vivo.
 */
export function displayDuration({ durationSeconds, startedAt, status }, now) {
    if (durationSeconds !== null && durationSeconds !== undefined) return formatDuration(durationSeconds);
    if (isActive(status) && startedAt) return formatDuration((now - new Date(startedAt).getTime()) / 1000);
    return "—";
}

/** Quantos scans do grupo já terminaram, e o percentual. */
export function groupProgress(group) {
    const byStatus = group.scansByStatus ?? {};
    const total = group.totalScans ?? 0;
    const active = (byStatus.PENDING ?? 0) + (byStatus.RUNNING ?? 0);
    const done = total - active;

    return { done, total, percent: total === 0 ? 0 : Math.round((done / total) * 100) };
}

/** Lista de alvos resumida: primeiros `max` e quantos ficaram de fora. */
export function summarizeTargets(targets, max = 2) {
    const list = targets ?? [];
    return { shown: list.slice(0, max), extra: Math.max(0, list.length - max) };
}

/** "2 concluídos · 1 falhou" a partir do mapa status → quantidade. */
export function describeStatusCounts(scansByStatus) {
    const labels = {
        RUNNING: (n) => `${n} em execução`,
        PENDING: (n) => `${n} pendente${n > 1 ? "s" : ""}`,
        COMPLETED: (n) => `${n} concluído${n > 1 ? "s" : ""}`,
        FAILED: (n) => `${n} ${n > 1 ? "falharam" : "falhou"}`,
        NOT_FOUND: (n) => `${n} sem host`,
        CANCELLED: (n) => `${n} cancelado${n > 1 ? "s" : ""}`,
    };

    return Object.keys(labels)
        .filter((status) => (scansByStatus?.[status] ?? 0) > 0)
        .map((status) => labels[status](scansByStatus[status]))
        .join(" · ");
}
