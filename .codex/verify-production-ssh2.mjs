import { Client } from './deploy-tools/node_modules/ssh2/lib/index.js'

const conn = new Client()

conn.on('ready', () => {
  conn.exec(`
set -e
echo "NGINX=$(systemctl is-active nginx)"
echo "GARMENT=$(systemctl is-active garment)"
echo "API_HTTP=$(curl -sS -o /tmp/garment-api-check.txt -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{}' http://127.0.0.1:8080/api/auth/login)"
echo "JAR_SHA256=$(sha256sum /opt/garment/backend/garment-server.jar | awk '{print $1}')"
echo "INDEX_SHA256=$(sha256sum /var/www/garment/index.html | awk '{print $1}')"
echo "BUNDLE_ASSET=$(grep -o 'assets/Bundle-[^"]*\\.js' /var/www/garment/assets/index-*.js | head -n 1 || true)"
tail -n 20 /opt/garment/logs/backend.log
`, (error, stream) => {
    if (error) throw error
    stream.on('data', (data) => process.stdout.write(data))
    stream.stderr.on('data', (data) => process.stderr.write(data))
    stream.on('close', (code) => {
      conn.end()
      process.exitCode = code
    })
  })
})

conn.on('error', (error) => {
  console.error(error.message)
  process.exitCode = 1
})

conn.connect({
  host: process.env.DEPLOY_HOST,
  port: 22,
  username: process.env.DEPLOY_USER,
  password: process.env.DEPLOY_PASSWORD,
  readyTimeout: 20000
})
