import { useState } from "react";
import { Navigate, useLocation } from "react-router-dom";

import "../../styles/auth.css";

import LOGO from "../../assets/full-logo.png";
import { getApiErrorMessage } from "../../services/apiError";
import { useAuth } from "./useAuth";

function describeLoginError(error) {
    if (error?.response?.status === 401) {
        return "E-mail ou senha inválidos.";
    }

    return getApiErrorMessage(error, "Não foi possível entrar. Tente novamente.");
}

export function Login() {
    const { user, login, sessionExpired, notice } = useAuth();
    const location = useLocation();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState("");

    // volta para a página que o usuário tentou abrir antes de ser mandado para o login
    const redirectTo = location.state?.from?.pathname ?? "/";

    if (user) {
        return <Navigate to={redirectTo} replace />;
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setError("");

        if (!email.trim() || !password) {
            setError("Informe o e-mail e a senha.");
            return;
        }

        setSubmitting(true);

        try {
            await login(email.trim(), password);
            // com o usuário definido, o <Navigate> acima redireciona
        } catch (err) {
            setError(describeLoginError(err));
            setPassword("");
            setSubmitting(false);
        }
    }

    return (
        <main className="login-page">
            <section className="login-card">
                <img src={LOGO} className="login-logo" alt="Sentry" />

                <h1 className="login-title">Entrar</h1>
                <p className="login-subtitle">Gestão de Vulnerabilidades e Compliance</p>

                {sessionExpired && !error && (
                    <p className="auth-alert auth-alert--info" role="status">
                        <i className="fa-solid fa-clock" />
                        Sua sessão expirou. Entre novamente para continuar.
                    </p>
                )}

                {notice && !error && (
                    <p className="auth-alert auth-alert--success" role="status">
                        <i className="fa-solid fa-circle-check" />
                        {notice}
                    </p>
                )}

                {error && (
                    <p className="auth-alert auth-alert--error" role="alert">
                        <i className="fa-solid fa-circle-exclamation" />
                        {error}
                    </p>
                )}

                <form onSubmit={handleSubmit} noValidate>
                    <label className="auth-field" htmlFor="login-email">
                        <span>E-mail</span>
                        <input
                            id="login-email"
                            name="email"
                            type="email"
                            autoComplete="username"
                            autoFocus
                            placeholder="voce@empresa.com"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            disabled={submitting}
                        />
                    </label>

                    <label className="auth-field" htmlFor="login-password">
                        <span>Senha</span>

                        <div className="auth-password">
                            <input
                                id="login-password"
                                name="password"
                                type={showPassword ? "text" : "password"}
                                autoComplete="current-password"
                                placeholder="Sua senha"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                disabled={submitting}
                            />

                            <button
                                type="button"
                                className="auth-password-toggle"
                                onClick={() => setShowPassword((visible) => !visible)}
                                aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
                                title={showPassword ? "Ocultar senha" : "Mostrar senha"}
                            >
                                <i className={`fa-solid ${showPassword ? "fa-eye-slash" : "fa-eye"}`} />
                            </button>
                        </div>
                    </label>

                    <button type="submit" className="login-submit" disabled={submitting}>
                        {submitting ? (
                            <><i className="fa-solid fa-circle-notch fa-spin" /> Entrando...</>
                        ) : (
                            "Entrar"
                        )}
                    </button>
                </form>

                <p className="login-help">
                    Esqueceu a senha? Solicite a redefinição a um administrador.
                </p>
            </section>

            <p className="login-footer">Ambiente acadêmico · TCC @ 2026</p>
        </main>
    );
}

export default Login;
