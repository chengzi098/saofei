import { readFileSync } from 'node:fs'
import { Client } from './deploy-tools/node_modules/ssh2/lib/index.js'

const host = process.env.DEPLOY_HOST
const username = process.env.DEPLOY_USER
const password = process.env.DEPLOY_PASSWORD
const archivePath = process.env.DEPLOY_ZIP
const remoteArchive = process.env.DEPLOY_REMOTE_ZIP || '/tmp/garment-update.zip'

if (!host || !username || !password || !archivePath) {
  throw new Error('Missing deployment environment variables')
}

const conn = new Client()

function exec(command) {
  return new Promise((resolve, reject) => {
    conn.exec(command, (error, stream) => {
      if (error) return reject(error)
      let stdout = ''
      let stderr = ''
      stream.on('data', (chunk) => { stdout += chunk })
      stream.stderr.on('data', (chunk) => { stderr += chunk })
      stream.on('close', (code) => {
        if (stdout.trim()) process.stdout.write(stdout)
        if (stderr.trim()) process.stderr.write(stderr)
        code === 0 ? resolve() : reject(new Error(`Remote command failed: ${code}`))
      })
    })
  })
}

function upload() {
  const data = readFileSync(archivePath)
  return new Promise((resolve, reject) => {
    conn.sftp((error, sftp) => {
      if (error) return reject(error)
      const stream = sftp.createWriteStream(remoteArchive, { mode: 0o600 })
      stream.on('error', reject)
      stream.on('close', () => {
        sftp.end()
        resolve()
      })
      stream.end(data)
    })
  })
}

conn.on('ready', async () => {
  try {
    await exec('hostname; date; systemctl is-active nginx; systemctl is-active garment')
    await upload()
    await exec(`
set -e
WORK_DIR=/tmp/garment-update-new
BACKEND_DIR=/opt/garment/backend
LIVE_FRONTEND=/var/www/garment
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"
unzip -q -o "${remoteArchive}" -d "$WORK_DIR"
test -f "$WORK_DIR/garment-server.jar"
test -f "$WORK_DIR/dist/index.html"

sudo cp "$BACKEND_DIR/garment-server.jar" "$BACKEND_DIR/garment-server.jar.previous"
sudo rm -rf /var/www/garment.previous
sudo cp -a "$LIVE_FRONTEND" /var/www/garment.previous

sudo cp "$WORK_DIR/garment-server.jar" "$BACKEND_DIR/garment-server.jar"
sudo rm -rf "$LIVE_FRONTEND"
sudo cp -a "$WORK_DIR/dist" "$LIVE_FRONTEND"
sudo chown -R www-data:www-data "$LIVE_FRONTEND"
sudo systemctl restart garment

for attempt in $(seq 1 30); do
  if systemctl is-active --quiet garment && curl -sS -o /dev/null http://127.0.0.1:8080/api/auth/login; then
    break
  fi
  sleep 1
done

if ! systemctl is-active --quiet garment; then
  sudo cp "$BACKEND_DIR/garment-server.jar.previous" "$BACKEND_DIR/garment-server.jar"
  sudo rm -rf "$LIVE_FRONTEND"
  sudo mv /var/www/garment.previous "$LIVE_FRONTEND"
  sudo systemctl restart garment
  exit 1
fi

sudo nginx -t
sudo systemctl reload nginx
echo FULL_DEPLOY_OK
systemctl is-active nginx
systemctl is-active garment
`)
    conn.end()
  } catch (error) {
    console.error(error.message)
    conn.end()
    process.exitCode = 1
  }
})

conn.on('error', (error) => {
  console.error(error.message)
  process.exitCode = 1
})

conn.connect({
  host,
  port: 22,
  username,
  password,
  readyTimeout: 20000
})
