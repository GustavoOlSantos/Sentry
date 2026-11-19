
#!/bin/sh

echo "[SENTRY-LAB2] Configurando serviços..."

# --------------------------------------------------
# 1. Desabilita serviços que não pertencem ao perfil
# --------------------------------------------------

if [ -f /etc/inetd.conf ]; then
    sed -i -r '/^(telnet|shell|login|exec|tftp)[[:space:]]/s/^/#/' /etc/inetd.conf
fi

# --------------------------------------------------
# 2. Configura UnrealIRCd
# --------------------------------------------------

UNREAL_CONFIG="/etc/unreal/unrealircd.conf"

if [ -f "$UNREAL_CONFIG" ]; then
    # Desabilita a porta SSL 6697, preservando a 6667.
    sed -i '/^[[:space:]]*listen[[:space:]]\+\*:6697[[:space:]]*;/s/^/# SENTRY: /' "$UNREAL_CONFIG"
    echo "[OK] UnrealIRCd: porta 6697 desabilitada"
fi

# --------------------------------------------------
# 3. Inicia os serviços de rede
# --------------------------------------------------

echo "[SENTRY-LAB2] Iniciando xinetd..."

# INETD_COMPAT=Yes permite utilizar /etc/inetd.conf.
if service xinetd start; then
    echo "[OK] xinetd iniciado"
else
    echo "[ERRO] Falha ao iniciar xinetd"
fi

echo "[SENTRY-LAB2] Iniciando distcc..."

if service distcc start; then
    echo "[OK] distcc iniciado"
else
    echo "[ERRO] Falha ao iniciar distcc"
fi

# --------------------------------------------------
# 4. Inicia serviços configurados originalmente
#    no rc.local:
#    - Java RMI (1099)
#    - VNC (5900)
#    - UnrealIRCd (6667)
#    - Ruby DRb (8787)
# --------------------------------------------------

if [ -f /etc/rc.local ]; then
    echo "[SENTRY-LAB2] Executando rc.local..."
    sh /etc/rc.local
else
    echo "[AVISO] /etc/rc.local não encontrado"
fi

# --------------------------------------------------
# 5. Inicia Tomcat
#    - AJP (8009)
# --------------------------------------------------

echo "[SENTRY-LAB2] Iniciando Tomcat..."

if service tomcat5.5 start; then
    echo "[OK] Tomcat iniciado"
else
    echo "[ERRO] Falha ao iniciar Tomcat"
fi

# --------------------------------------------------
# 6. Finalização
# --------------------------------------------------

echo ""
echo "[SENTRY-LAB2] Configuração concluída."
echo "[SENTRY-LAB2] Serviços esperados:"
echo "  1099 - Java RMI"
echo "  1524 - Bindshell (inetd/xinetd)"
echo "  3632 - distccd"
echo "  5900 - VNC"
echo "  6667 - UnrealIRCd"
echo "  8009 - Tomcat AJP"
echo "  8787 - Ruby DRb"

tail -f /dev/null
