export function getInitials(name) {
    const parts = (name ?? "").trim().split(/\s+/).filter(Boolean);

    if (parts.length === 0) return "?";
    if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();

    return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

export function formatDate(value) {
    return value ? new Date(value).toLocaleDateString("pt-BR") : "—";
}

export function formatDateTime(value) {
    return value ? new Date(value).toLocaleString("pt-BR") : "—";
}

/** O backend limita a senha a 72 BYTES (limite do BCrypt) e exige 8+ caracteres. */
export function validatePassword(password) {
    if (!password) return "Informe a senha.";
    if (password.length < 8) return "A senha deve ter pelo menos 8 caracteres.";
    if (new TextEncoder().encode(password).length > 72) return "A senha é longa demais (máximo de 72 bytes).";
    return null;
}

export function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}
