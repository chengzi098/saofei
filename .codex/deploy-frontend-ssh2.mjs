import { readFileSync } from 'node:fs'
import { Client } from './deploy-tools/node_modules/ssh2/lib/index.js'

const host = process.env.DEPLOY_HOST
const username = process.env.DEPLOY_USER
const password = process.env.DEPLOY_PASSWORD
const archivePath = process.env.DEPLOY_ZIP
const remoteArchive = process.env.DEPLOY_REMOTE_ZIP || '/tmp/garment-frontend.zip'

if (!host || !username || !password || !archivePath) {
  throw new Error('Missing DEPLOY_HOST, DEPLOY_USER, DEPLOY_PASSWORD, or DEPLOY_ZIP')
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
        if (code === 0) resolve()
        else reject(new Error(`Remote command failed with exit code ${code}`))
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
    await exec(
      'hostname; date; ' +
      'test -d /var/www/garment && echo FRONTEND_DIR_OK; ' +
      'systemctl is-active nginx; systemctl is-active garment'
    )
    await upload()
    await exec(`
set -e
WORK_DIR=/tmp/garment-frontend-new
LIVE_DIR=/var/www/garment
PREVIOUS_DIR=/var/www/garment.previous
rm -rf "$WORK_DIR"
mkdir -p "$WORK_DIR"
unzip -q -o "${remoteArchive}" -d "$WORK_DIR"
test -f "$WORK_DIR/index.html"
sudo rm -rf "$PREVIOUS_DIR"
if [ -d "$LIVE_DIR" ]; then sudo mv "$LIVE_DIR" "$PREVIOUS_DIR"; fi
sudo mv "$WORK_DIR" "$LIVE_DIR"
sudo chown -R www-data:www-data "$LIVE_DIR"
if sudo nginx -t; then
  sudo systemctl reload nginx
else
  sudo rm -rf "$LIVE_DIR"
  if [ -d "$PREVIOUS_DIR" ]; then sudo mv "$PREVIOUS_DIR" "$LIVE_DIR"; fi
  exit 1
fi
echo FRONTEND_DEPLOY_OK
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
