#!/bin/sh

echo "[SENTRY-LAB1] Configurando serviços..."

# Desabilita serviços que não pertencem ao perfil 1
if [ -f /etc/inetd.conf ]; then
    sed -i -r '/^(shell|login|exec|ingreslock|tftp)[[:space:]]/s/^/#/' /etc/inetd.conf
fi

# Serviços que devem estar disponíveis no alvo
SERVICES="
apache2
ssh
openbsd-inetd
proftpd
samba
mysql
postgresql-8.3
tomcat5.5
"

for service_name in $SERVICES; do
    echo "[SENTRY-LAB1] Iniciando $service_name..."

    if service "$service_name" start; then
        echo "[OK] $service_name"
    else
        echo "[ERRO] Falha ao iniciar $service_name"
    fi
done

echo "[SENTRY-LAB1] Configuração concluída."

tail -f /dev/null
