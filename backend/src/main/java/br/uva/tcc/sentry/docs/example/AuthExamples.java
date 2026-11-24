package br.uva.tcc.sentry.docs.example;

/** Exemplos dos endpoints de autenticação e de usuários. */
public final class AuthExamples {

    public static final String LOGIN_REQUEST = """
            {
              "email": "admin@sentry.com",
              "password": "12345678"
            }
            """;

    public static final String LOGIN_RESPONSE = """
            {
              "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJzZW50cnkiLCJzdWIiOiIxIn0.assinatura",
              "tokenType": "Bearer",
              "expiresIn": 7200,
              "user": {
                "id": 1,
                "name": "Administrador",
                "email": "admin@sentry.com",
                "role": "ADM",
                "active": true,
                "createdAt": "2026-11-01T09:00:00",
                "lastLoginAt": "2026-11-22T14:25:10"
              }
            }
            """;

    public static final String USER = """
            {
              "id": 2,
              "name": "Maria Souza",
              "email": "maria@sentry.com",
              "role": "DEV",
              "active": true,
              "createdAt": "2026-11-10T10:15:00",
              "lastLoginAt": null
            }
            """;

    public static final String USER_LIST = """
            [
              {
                "id": 1,
                "name": "Administrador",
                "email": "admin@sentry.com",
                "role": "ADM",
                "active": true,
                "createdAt": "2026-11-01T09:00:00",
                "lastLoginAt": "2026-11-22T14:25:10"
              },
              {
                "id": 2,
                "name": "Maria Souza",
                "email": "maria@sentry.com",
                "role": "DEV",
                "active": true,
                "createdAt": "2026-11-10T10:15:00",
                "lastLoginAt": null
              }
            ]
            """;

    public static final String CHANGE_PASSWORD_REQUEST = """
            {
              "currentPassword": "12345678",
              "newPassword": "NovaSenha@2026"
            }
            """;

    public static final String CREATE_USER_REQUEST = """
            {
              "name": "Maria Souza",
              "email": "maria@sentry.com",
              "password": "Senha@2026",
              "role": "DEV"
            }
            """;

    public static final String UPDATE_USER_REQUEST = """
            {
              "name": "Maria Souza",
              "email": "maria@sentry.com",
              "role": "USER",
              "active": true
            }
            """;

    public static final String RESET_PASSWORD_REQUEST = """
            {
              "newPassword": "SenhaTemporaria@2026"
            }
            """;

    private AuthExamples() {
    }
}
