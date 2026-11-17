#!/bin/sh

# Desabilita serviços que não pertencem ao perfil 2
sed -i -E '/^(telnet|shell|login|exec|tftp)[[:space:]]/s/^/#/' /etc/inetd.conf

service openbsd-inetd start
service distcc start

# Serviços iniciados originalmente pelo rc.local
sh /etc/rc.local

# Tomcat para o conector AJP
service tomcat5.5 start

tail -f /dev/null