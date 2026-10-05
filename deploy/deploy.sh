#!/bin/bash
set -euo pipefail

APP_NAME="garment"
APP_DIR="/opt/garment"
: "${DB_PASSWORD:?请先设置 DB_PASSWORD 环境变量}"
: "${JWT_SECRET:?请先设置 JWT_SECRET 环境变量}"
SERVER_PORT="8080"
JAVA_BIN="/usr/lib/jvm/java-21-openjdk-amd64/bin/java"
DOMAIN="www.fjsaofei.cn"
CERT_DOMAIN="fjsaofei.cn"

echo "=============================="
echo "  Garment deploy start"
echo "=============================="

echo "[1/6] Create directories..."
mkdir -p "$APP_DIR"/{backend,frontend,logs}

echo "[2/6] Initialize database..."
mysql -u root -p"$DB_PASSWORD" < "$APP_DIR/backend/schema.sql"
if [ -f "$APP_DIR/backend/migration.sql" ]; then
    mysql -u root -p"$DB_PASSWORD" < "$APP_DIR/backend/migration.sql"
fi
echo "Database ready"

echo "[3/6] Configure backend service..."
cat > /etc/systemd/system/$APP_NAME.service <<EOF
[Unit]
Description=Garment Management System
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=$APP_DIR/backend
ExecStart=$JAVA_BIN -jar garment-server.jar --spring.profiles.active=prod
Environment=DB_USERNAME=root
Environment=DB_PASSWORD=$DB_PASSWORD
Environment=JWT_SECRET=$JWT_SECRET
Environment=JAVA_OPTS=-Xms512m -Xmx1024m
Restart=always
RestartSec=10
StandardOutput=append:$APP_DIR/logs/backend.log
StandardError=append:$APP_DIR/logs/backend.log

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable "$APP_NAME"
systemctl restart "$APP_NAME"
echo "Backend service started"

echo "[4/6] Deploy frontend..."
rm -rf /var/www/garment
mkdir -p /var/www/garment
cp -a "$APP_DIR/backend/dist/." /var/www/garment/

echo "[5/6] Configure Nginx..."
cat > /etc/nginx/sites-available/$APP_NAME <<EOF
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name fjsaofei.cn www.fjsaofei.cn _;

    root /var/www/garment;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:$SERVER_PORT/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    client_max_body_size 10m;
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
}
EOF

CERT_DIR="/etc/letsencrypt/live/$CERT_DOMAIN"
if [ ! -f "$CERT_DIR/fullchain.pem" ] && [ -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
    CERT_DIR="/etc/letsencrypt/live/$DOMAIN"
fi

if [ -f "$CERT_DIR/fullchain.pem" ] && [ -f "$CERT_DIR/privkey.pem" ]; then
cat > /etc/nginx/sites-available/$APP_NAME <<EOF
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    server_name fjsaofei.cn www.fjsaofei.cn _;

    location ^~ /.well-known/acme-challenge/ {
        default_type text/plain;
        root /var/www/certbot;
    }

    location / {
        return 301 https://$DOMAIN\$request_uri;
    }
}

server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name fjsaofei.cn www.fjsaofei.cn;

    ssl_certificate $CERT_DIR/fullchain.pem;
    ssl_certificate_key $CERT_DIR/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 1d;

    root /var/www/garment;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:$SERVER_PORT/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    client_max_body_size 10m;
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
}
EOF
fi

ln -sf /etc/nginx/sites-available/$APP_NAME /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl restart nginx
echo "Nginx ready"

echo "[6/6] Configure firewall..."
if command -v ufw > /dev/null 2>&1; then
    ufw allow 22/tcp
    ufw allow 80/tcp
    ufw allow 443/tcp
    ufw allow "$SERVER_PORT"/tcp
    ufw --force enable
fi

echo "=============================="
echo "Deploy complete"
echo "Frontend: https://$DOMAIN"
echo "Backend: http://43.130.31.244:$SERVER_PORT"
echo "Admin: admin（初始密码由 ADMIN_INIT_PASSWORD 或启动日志提供）"
echo "=============================="
