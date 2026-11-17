describe('Sentry - Autenticação e Controle de Acesso (RBAC)', () => {
  it('deve redirecionar usuário não autenticado para /login', () => {
    cy.visit('/');
    cy.url().should('include', '/login');
    cy.contains('h1', 'Entrar').should('be.visible');
  });

  it('deve validar campos obrigatórios no formulário de login', () => {
    cy.visit('/login');
    cy.get('button[type="submit"]').click();
    cy.get('[role="alert"]').should('contain.text', 'Informe o e-mail e a senha.');
  });

  it('deve realizar login via formulário como ADM e acessar o Dashboard', () => {
    cy.mockSentryApi('ADM');
    cy.visit('/login');
    cy.get('#login-email').clear().type('admin@sentry.local');
    cy.get('#login-password').clear().type('Admin@123');
    cy.get('button[type="submit"]').click();
    cy.wait('@loginRequest');
    cy.url().should('eq', `${Cypress.config('baseUrl')}/`);
    cy.get('nav').should('be.visible');
  });

  it('deve bloquear acesso de perfil USER à rota administrativa /usuarios (Tela Forbidden)', () => {
    cy.loginWithRole('USER');
    cy.visit('/usuarios');
    cy.contains('h1', 'permissão para').should('be.visible');
  });

  it('deve exibir tela 404 para rotas inexistentes', () => {
    cy.loginWithRole('ADM');
    cy.visit('/rota-inexistente-tcc');
    cy.contains('h1', 'não existe').should('be.visible');
  });
});
