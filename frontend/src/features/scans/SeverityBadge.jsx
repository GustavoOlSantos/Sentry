import { SEVERITY_LABELS } from "./scan-utils";

export default function SeverityBadge({ severity }) {
    return (
        <span className={`severity-badge severity-badge--${String(severity).toLowerCase()}`}>
            {SEVERITY_LABELS[severity] ?? severity}
        </span>
    );
}
