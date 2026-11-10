import { ROLE_LABELS } from "./roles";

export default function RoleBadge({ role }) {
    return (
        <span className={`role-badge role-badge--${String(role).toLowerCase()}`}>
            {ROLE_LABELS[role] ?? role}
        </span>
    );
}
