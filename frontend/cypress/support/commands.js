// Gera um JWT sintético com expiração futura (necessário para session.js / isTokenExpired)
function createFakeJwt(role = 'ADM', email = 'admin@sentry.local') {
  const header = { alg: 'HS256', typ: 'JWT' };
  const payload = {
    sub: email,
    role,
    exp: Math.floor(Date.now() / 1000) + 3600 * 4, // válido por 4 horas
  };

  const encode = (obj) =>
    btoa(JSON.stringify(obj))
      .replace(/=/g, '')
      .replace(/\+/g, '-')
      .replace(/\//g, '_');

  return `${encode(header)}.${encode(payload)}.fake-signature-cypress`;
}

const MOCK_IDS = {
  assetId: '11111111-1111-1111-1111-111111111111',
  groupId: '22222222-2222-2222-2222-222222222222',
  scanId: '33333333-3333-3333-3333-333333333333',
  findingId: '44444444-4444-4444-4444-444444444444',
  policyId: '55555555-5555-5555-5555-555555555555',
  ruleId: '66666666-6666-6666-6666-666666666666',
};

Cypress.Commands.add('mockSentryApi', (role = 'ADM') => {
  const userMap = {
    ADM: {
      id: 1,
      name: 'Administrador TCC',
      email: 'admin@sentry.local',
      role: 'ADM',
      active: true,
      createdAt: '2026-08-01T10:00:00',
      lastLoginAt: '2026-11-17T14:00:00',
    },
    DEV: {
      id: 2,
      name: 'Desenvolvedor SecOps',
      email: 'dev@sentry.local',
      role: 'DEV',
      active: true,
      createdAt: '2026-08-02T10:00:00',
      lastLoginAt: '2026-11-17T13:30:00',
    },
    USER: {
      id: 3,
      name: 'Auditor Leitura',
      email: 'user@sentry.local',
      role: 'USER',
      active: true,
      createdAt: '2026-08-03T10:00:00',
      lastLoginAt: '2026-11-17T12:00:00',
    },
  };

  const currentUser = userMap[role] || userMap.ADM;
  const fakeToken = createFakeJwt(currentUser.role, currentUser.email);

  // Healthcheck (usado na tela /novo-scan)
  cy.intercept('GET', '**/actuator/health', {
    statusCode: 200,
    body: { status: 'UP' },
  }).as('healthCheck');

  // Autenticação
  cy.intercept('POST', '**/auth/login', {
    statusCode: 200,
    body: {
      accessToken: fakeToken,
      tokenType: 'Bearer',
      expiresIn: 14400,
      user: currentUser,
    },
  }).as('loginRequest');

  cy.intercept('GET', '**/auth/me', {
    statusCode: 200,
    body: currentUser,
  }).as('getMe');

  cy.intercept('PUT', '**/auth/me/password', {
    statusCode: 204,
    body: {},
  }).as('changePassword');

  // Usuários (/users)
  cy.intercept('GET', '**/users', {
    statusCode: 200,
    body: [userMap.ADM, userMap.DEV, userMap.USER],
  }).as('getUsers');

  // Ativos (/asset)
  const mockAssets = [
    {
      id: MOCK_IDS.assetId,
      ipAddress: '192.168.10.15',
      hostname: 'lab-target-01.local',
      macAddress: '02:42:AC:11:00:02',
      operatingSystem: 'Ubuntu Linux 22.04',
      status: 'ACTIVE',
      firstSeenAt: '2026-11-10T09:00:00',
      lastSeenAt: '2026-11-17T14:30:00',
      servicesCount: 2,
      findingsCount: 1,
      services: [
        {
          id: '77777777-7777-7777-7777-777777777777',
          port: 22,
          protocol: 'tcp',
          serviceName: 'ssh',
          product: 'OpenSSH',
          version: '8.9p1',
        },
        {
          id: '88888888-8888-8888-8888-888888888888',
          port: 80,
          protocol: 'tcp',
          serviceName: 'http',
          product: 'nginx',
          version: '1.18.0',
        },
      ],
      findings: [
        {
          id: MOCK_IDS.findingId,
          code: 'POL-SSH-001',
          type: 'POLICY_VIOLATION',
          severity: 'HIGH',
          status: 'OPEN',
          title: 'Serviço SSH exposto na rede',
        },
      ],
    },
  ];

  cy.intercept('GET', '**/asset', {
    statusCode: 200,
    body: mockAssets,
  }).as('getAssets');

  // Scans e Grupos (/scanGroup, /scan)
  const mockGroupSummary = {
    id: MOCK_IDS.groupId,
    status: 'COMPLETED',
    createdAt: '2026-11-17T14:00:00',
    startedAt: '2026-11-17T14:00:02',
    finishedAt: '2026-11-17T14:00:25',
    durationSeconds: 23,
    errorMessage: null,
    totalScans: 1,
    scansByStatus: { COMPLETED: 1 },
    targets: ['192.168.10.15'],
    assetsCount: 1,
    findingsCount: 1,
  };

  cy.intercept('GET', '**/scanGroup', {
    statusCode: 200,
    body: [mockGroupSummary],
  }).as('listScanGroups');

  cy.intercept('GET', `**/scanGroup/id/${MOCK_IDS.groupId}`, {
    statusCode: 200,
    body: {
      group: mockGroupSummary,
      scans: [
        {
          id: MOCK_IDS.scanId,
          target: '192.168.10.15',
          status: 'COMPLETED',
          createdAt: '2026-11-17T14:00:00',
          startedAt: '2026-11-17T14:00:02',
          finishedAt: '2026-11-17T14:00:25',
          durationSeconds: 23,
          assetsCount: 1,
          findingsCount: 1,
        },
      ],
    },
  }).as('getScanGroupDetail');

  cy.intercept('GET', `**/scan/id/${MOCK_IDS.scanId}`, {
    statusCode: 200,
    body: {
      id: MOCK_IDS.scanId,
      groupId: MOCK_IDS.groupId,
      target: '192.168.10.15',
      command: 'nmap -sV -O -oX - 192.168.10.15',
      status: 'COMPLETED',
      errorMessage: null,
      warnings: [],
      createdAt: '2026-11-17T14:00:00',
      startedAt: '2026-11-17T14:00:02',
      finishedAt: '2026-11-17T14:00:25',
      durationSeconds: 23,
      assets: [
        {
          id: MOCK_IDS.assetId,
          ipAddress: '192.168.10.15',
          hostname: 'lab-target-01.local',
          operatingSystem: 'Ubuntu Linux 22.04',
          status: 'ACTIVE',
          discoveredAt: '2026-11-17T14:00:15',
          openPorts: [
            { port: 22, protocol: 'tcp', service: 'ssh', product: 'OpenSSH', version: '8.9p1' },
          ],
        },
      ],
      findings: [
        {
          id: MOCK_IDS.findingId,
          type: 'POLICY_VIOLATION',
          code: 'POL-SSH-001',
          title: 'Serviço SSH exposto na rede',
          severity: 'HIGH',
          status: 'OPEN',
          assetId: MOCK_IDS.assetId,
          assetIp: '192.168.10.15',
          port: 22,
          protocol: 'tcp',
        },
      ],
    },
  }).as('getScanDetail');

  cy.intercept('POST', '**/scan/exec', {
    statusCode: 202,
    body: {
      message: 'Varredura enfileirada no RabbitMQ com sucesso.',
      status: 'ACCEPTED',
      groupId: MOCK_IDS.groupId,
    },
  }).as('execScan');

  // Board / Findings (/findings)
  cy.intercept('GET', '**/findings', {
    statusCode: 200,
    body: [
      {
        id: MOCK_IDS.findingId,
        code: 'POL-SSH-001',
        type: 'POLICY_VIOLATION',
        severity: 'HIGH',
        status: 'OPEN',
        title: 'Serviço SSH exposto na rede',
        description: 'Porta 22 acessível sem restrição de rede.',
        detectedAt: '2026-11-17T14:00:20',
        resolvedAt: null,
        lastSeenAt: '2026-11-17T14:00:20',
        validatedAt: null,
        reopenCount: 0,
        evidence: '22/tcp open ssh OpenSSH 8.9p1',
        justification: null,
        assigneeId: 2,
        asset: {
          id: MOCK_IDS.assetId,
          ipAddress: '192.168.10.15',
          hostname: 'lab-target-01.local',
        },
        service: {
          id: '77777777-7777-7777-7777-777777777777',
          port: 22,
          protocol: 'tcp',
          serviceName: 'ssh',
          product: 'OpenSSH',
          version: '8.9p1',
        },
      },
    ],
  }).as('getFindings');

  // Políticas (/policy e /policyRule)
  const mockPolicy = {
    id: MOCK_IDS.policyId,
    name: 'Política Base de Segurança',
    description: 'Regras padrão de exposição de serviços e certificados.',
    status: 'ACTIVE',
    createdAt: '2026-10-01T10:00:00',
  };

  cy.intercept('GET', '**/policy', {
    statusCode: 200,
    body: [mockPolicy],
  }).as('listPolicies');

  cy.intercept('GET', `**/policy/${MOCK_IDS.policyId}`, {
    statusCode: 200,
    body: mockPolicy,
  }).as('getPolicyDetail');

  cy.intercept('GET', '**/policyRule', {
    statusCode: 200,
    body: [
      {
        id: MOCK_IDS.ruleId,
        policyId: MOCK_IDS.policyId,
        name: 'Bloquear SSH Externo',
        type: 'EXPOSED_SSH',
        severity: 'HIGH',
        enabled: true,
        parameters: '{}',
      },
    ],
  }).as('listPolicyRules');

  return cy.wrap({ currentUser, fakeToken });
});

Cypress.Commands.add('loginWithRole', (role = 'ADM') => {
  cy.mockSentryApi(role).then(({ currentUser, fakeToken }) => {
    // Injeta a sessão válida antes do carregamento da página para evitar recarregamentos extras
    cy.visit('/', {
      onBeforeLoad(win) {
        win.localStorage.setItem('token', fakeToken);
        win.localStorage.setItem('user', JSON.stringify(currentUser));
      },
    });
    cy.wait('@getMe');
  });
});

export { MOCK_IDS };
