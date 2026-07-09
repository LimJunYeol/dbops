<script setup>
import { ref, onMounted } from 'vue'

const instances = ref([])
const selected = ref(null)      // 상세 보기 대상
const detail = ref(null)        // { instance, recentChecks }
const metrics = ref([])
const tasks = ref([])
const message = ref('')

// 계정/권한 폼
const accountForm = ref({ username: '', password: '' })
const grantForm = ref({ username: '', database: '', privilege: 'READ_ONLY' })

async function loadInstances() {
  instances.value = await (await fetch('/api/instances')).json()
}

async function select(inst) {
  selected.value = inst
  message.value = ''
  const id = inst.id
  detail.value = await (await fetch(`/api/instances/${id}`)).json()
  metrics.value = await (await fetch(`/api/instances/${id}/metrics`)).json()
  tasks.value = await (await fetch(`/api/instances/${id}/tasks`)).json()
}

async function checkNow() {
  await fetch(`/api/instances/${selected.value.id}/health`, { method: 'POST' })
  await loadInstances()
  await select(instances.value.find(i => i.id === selected.value.id))
}

async function post(path, body) {
  const res = await fetch(`/api/instances/${selected.value.id}/${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  const data = await res.json()
  message.value = res.ok
    ? `작업 #${data.id} ${data.status}: ${data.detail}`
    : `오류: ${data.message ?? res.status}`
  tasks.value = await (await fetch(`/api/instances/${selected.value.id}/tasks`)).json()
}

const createAccount = () => post('accounts', accountForm.value)
const grant = () => post('grants', grantForm.value)

onMounted(loadInstances)
</script>

<template>
  <main>
    <h1>DBOps 콘솔</h1>

    <!-- 1. 인스턴스 목록 -->
    <section>
      <h2>인스턴스 <button @click="loadInstances">새로고침</button></h2>
      <table>
        <thead><tr><th>ID</th><th>이름</th><th>주소</th><th>상태</th></tr></thead>
        <tbody>
          <tr v-for="i in instances" :key="i.id"
              :class="{ active: selected?.id === i.id }" @click="select(i)">
            <td>{{ i.id }}</td>
            <td>{{ i.name }}</td>
            <td>{{ i.host }}:{{ i.port }}</td>
            <td><span :class="['badge', i.status]">{{ i.status }}</span></td>
          </tr>
        </tbody>
      </table>
    </section>

    <template v-if="selected">
      <!-- 2. 상세: 헬스체크 이력 + 지표 -->
      <section>
        <h2>{{ selected.name }} 상세 <button @click="checkNow">즉시 점검</button></h2>
        <div class="grid">
          <div>
            <h3>헬스체크 이력</h3>
            <table>
              <thead><tr><th>시각</th><th>결과</th><th>응답(ms)</th><th>커넥션</th></tr></thead>
              <tbody>
                <tr v-for="(c, idx) in detail?.recentChecks" :key="idx">
                  <td>{{ c.checkedAt?.replace('T', ' ').slice(0, 19) }}</td>
                  <td>{{ c.reachable ? 'OK' : 'FAIL' }}</td>
                  <td>{{ c.responseMs ?? '-' }}</td>
                  <td>{{ c.connectionCnt ?? '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div>
            <h3>수집 지표 (에이전트)</h3>
            <table>
              <thead><tr><th>시각</th><th>커넥션</th><th>슬로우쿼리</th><th>Uptime(s)</th></tr></thead>
              <tbody>
                <tr v-for="m in metrics" :key="m.id">
                  <td>{{ m.collectedAt?.replace('T', ' ').slice(0, 19) }}</td>
                  <td>{{ m.threadsConnected }}</td>
                  <td>{{ m.slowQueries }}</td>
                  <td>{{ m.uptimeSec }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- 3. 계정/권한 작업 -->
      <section>
        <h2>계정 / 권한 작업</h2>
        <div class="grid">
          <div>
            <h3>계정 생성</h3>
            <input v-model="accountForm.username" placeholder="계정명" />
            <input v-model="accountForm.password" type="password" placeholder="비밀번호 (8자+)" />
            <button @click="createAccount">생성</button>
          </div>
          <div>
            <h3>권한 부여</h3>
            <input v-model="grantForm.username" placeholder="계정명" />
            <input v-model="grantForm.database" placeholder="데이터베이스명" />
            <select v-model="grantForm.privilege">
              <option>READ_ONLY</option>
              <option>READ_WRITE</option>
            </select>
            <button @click="grant">부여</button>
          </div>
        </div>
        <p v-if="message" class="msg">{{ message }}</p>

        <h3>작업 이력</h3>
        <table>
          <thead><tr><th>ID</th><th>유형</th><th>상태</th><th>내용</th><th>오류</th></tr></thead>
          <tbody>
            <tr v-for="t in tasks" :key="t.id">
              <td>{{ t.id }}</td>
              <td>{{ t.type }}</td>
              <td><span :class="['badge', t.status]">{{ t.status }}</span></td>
              <td>{{ t.detail }}</td>
              <td class="err">{{ t.errorMessage ?? '' }}</td>
            </tr>
          </tbody>
        </table>
      </section>
    </template>
  </main>
</template>

<style>
body { font-family: system-ui, sans-serif; margin: 0; background: #f5f6f8; }
main { max-width: 1000px; margin: 0 auto; padding: 24px; }
section { background: #fff; border-radius: 8px; padding: 16px 20px; margin-bottom: 16px; }
table { width: 100%; border-collapse: collapse; font-size: 14px; }
th, td { text-align: left; padding: 6px 8px; border-bottom: 1px solid #eee; }
tbody tr { cursor: pointer; }
tbody tr.active { background: #eef4ff; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.badge { padding: 2px 8px; border-radius: 10px; font-size: 12px; }
.badge.ACTIVE, .badge.SUCCESS { background: #d9f2e3; color: #14713d; }
.badge.UNREACHABLE, .badge.FAILED { background: #fde2e2; color: #b42318; }
.badge.PENDING { background: #fff4d6; color: #92600a; }
input, select { display: block; margin: 6px 0; padding: 6px 8px; width: 220px; }
.msg { background: #eef4ff; padding: 8px 12px; border-radius: 6px; font-size: 14px; }
.err { color: #b42318; font-size: 12px; }
</style>