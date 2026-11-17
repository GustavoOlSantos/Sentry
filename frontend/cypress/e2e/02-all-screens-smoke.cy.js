import { MOCK_IDS } from '../support/commands';

describe('Sentry - Navegação E2E por Todas as Telas da Aplicação', () => {
  beforeEach(() => {
    cy.loginWithRole('ADM');
  });

  it('1. Home (Dashboard de Ativos e Vulnerabilidades)', () => {
    cy.visit('/');
    cy.wait('@getAssets');
    cy.get('main').should('be.visible');
  });

  it('2. Novo Scan (Submissão de Alvo para Fila RabbitMQ)', () => {
    cy.visit('/novo-scan');
    cy.wait('@healthCheck');
    cy.contains('h2', 'Hosts a serem escaneados').should('be.visible');

    cy.get('input[placeholder*="192.168"]').first().type('192.168.10.15');
    cy.get('button[type="submit"]').click();
    cy.wait('@execScan');
    cy.contains('Varredura enfileirada no RabbitMQ com sucesso.').should('be.visible');
  });

  it('3. Varreduras: Listagem de Grupos, Detalhes do Grupo e Detalhes do Scan', () => {
    cy.visit('/scans');
    cy.wait('@listScanGroups');
    cy.contains('h2', 'Varreduras').should('be.visible');
    cy.contains('192.168.10.15').should('be.visible');

    // Detalhes do Grupo
    cy.visit(`/scans/grupo/${MOCK_IDS.groupId}`);
    cy.wait('@getScanGroupDetail');
    cy.get('main').should('be.visible');

    // Detalhes do Scan Individual
    cy.visit(`/scans/${MOCK_IDS.scanId}`);
    cy.wait('@getScanDetail');
    cy.contains('nmap -sV -O -oX - 192.168.10.15').should('be.visible');
  });

  it('4. Ativos / Hosts Descobertos', () => {
    cy.visit('/hosts');
    cy.wait('@getAssets');
    cy.contains('h2', 'Ativos').should('be.visible');
    cy.contains('192.168.10.15').should('be.visible');
  });

  it('5. Quadro Kanban de Findings', () => {
    cy.visit('/board');
    cy.wait(['@getFindings', '@getUsers']);
    cy.contains('h2', 'Findings').should('be.visible');
    cy.contains('Serviço SSH exposto na rede').should('be.visible');
  });

  it('6. Políticas de Segurança: Listagem, Nova Política e Detalhes', () => {
    cy.visit('/policies');
    cy.wait('@listPolicies');
    cy.contains('h2', 'Políticas').should('be.visible');
    cy.contains('Política Base de Segurança').should('be.visible');

    // Modal de Nova Política (/policies/nova)
    cy.visit('/policies/nova');
    cy.contains('Nova política').should('be.visible');

    // Detalhes da Política (/policies/:id)
    cy.visit(`/policies/${MOCK_IDS.policyId}`);
    cy.wait(['@getPolicyDetail', '@listPolicyRules']);
    cy.contains('h2', 'Política Base de Segurança').should('be.visible');
  });

  it('7. Gestão de Usuários e Meu Perfil', () => {
    cy.visit('/usuarios');
    cy.wait('@getUsers');
    cy.contains('h2', 'Usuários').should('be.visible');
    cy.contains('Administrador TCC').should('be.visible');

    cy.visit('/perfil');
    cy.contains('h2', 'Meu perfil').should('be.visible');
    cy.contains('admin@sentry.local').should('be.visible');
  });
});
