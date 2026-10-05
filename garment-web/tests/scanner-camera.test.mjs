import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'
import assert from 'node:assert/strict'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const scannerFiles = [
  'src/views/mobile/MobileScan.vue',
  'src/views/mobile/MobileSupervisorScanBundle.vue'
]

for (const relativePath of scannerFiles) {
  const content = readFileSync(resolve(root, relativePath), 'utf8')
  const videoBlock = content.match(/<video[\s\S]*?<\/video>/)?.[0] ?? ''

  assert(
    !videoBlock.includes('v-if='),
    `${relativePath} must keep the video element mounted before the camera stream is attached`
  )

  assert(
    videoBlock.includes('v-show="cameraStatus === \'ready\'"'),
    `${relativePath} must only hide the mounted video until the stream is ready`
  )

  assert(
    content.includes('capture="environment"'),
    `${relativePath} must hint mobile browsers to open the rear camera for photo fallback`
  )

  assert(
    content.includes('await video.play()'),
    `${relativePath} must explicitly start video playback after attaching the camera stream`
  )
}
