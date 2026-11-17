#!/bin/sh

# Desabilita serviços que não pertencem ao perfil 1
sed -i -E '/^(shell|login|exec|ingreslock|tftp)[[:space:]]/s/^/#/' /etc/inetd.conf

service apache2 start
service ssh start
service openbsd-inetd start
service proftpd start
service samba start
service mysql start
service postgresql-8.3 start
service tomcat5.5 start

tail -f /dev/null