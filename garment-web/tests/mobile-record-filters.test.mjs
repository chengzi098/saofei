import assert from 'node:assert/strict'
import { buildMobileRecordParams, formatLocalDate } from '../src/utils/mobileRecordFilters.js'

const workerParams = buildMobileRecordParams({
  role: 'worker',
  userId: 12,
  activeDateTab: 'today',
  now: new Date(2026, 5, 18, 0, 30, 0),
})

assert.equal(workerParams.workerId, 12)
assert.equal(workerParams.startDate, '2026-06-18')
assert.equal(workerParams.endDate, '2026-06-18')

const supervisorParams = buildMobileRecordParams({
  role: 'supervisor',
  userId: 34,
  activeDateTab: 'today',
  now: new Date(2026, 5, 18, 0, 30, 0),
})

assert.equal(Object.hasOwn(supervisorParams, 'workerId'), false)
assert.equal(supervisorParams.startDate, '2026-06-18')
assert.equal(supervisorParams.endDate, '2026-06-18')

const returnedParams = buildMobileRecordParams({
  role: 'supervisor',
  userId: 34,
  activeStatus: 'returned',
  activeDateTab: 'custom',
  dateRange: ['2026-06-01', '2026-06-10'],
})

assert.equal(returnedParams.status, 3)
assert.equal(returnedParams.startDate, '2026-06-01')
assert.equal(returnedParams.endDate, '2026-06-10')

assert.equal(formatLocalDate(new Date(2026, 0, 2, 3, 4, 5)), '2026-01-02')

console.log('mobile record filters ok')
