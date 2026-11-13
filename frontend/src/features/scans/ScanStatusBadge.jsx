import { STATUS_LABELS } from "./scan-utils";

/** Selo de status de grupo/scan; o ponto pulsa enquanto está em execução. */
export default function ScanStatusBadge({ status }) {
    const modifier = String(status).toLowerCase().replace("_", "-");

    return (
        <span className={`scan-badge scan-badge--${modifier}`}>
            <span className="scan-badge__dot" aria-hidden="true" />
            {STATUS_LABELS[status] ?? status}
        </span>
    );
}
