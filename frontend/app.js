'use strict';

const TOKEN_KEY = 'billing.jwt';
const $ = (sel, root = document) => root.querySelector(sel);

// ---------- token helpers ----------
const getToken = () => localStorage.getItem(TOKEN_KEY);
const setToken = (t) => localStorage.setItem(TOKEN_KEY, t);
const clearToken = () => localStorage.removeItem(TOKEN_KEY);

function tokenClaims() {
  const t = getToken();
  if (!t) return {};
  try { return JSON.parse(atob(t.split('.')[1])); } catch { return {}; }
}
const tokenSubject = () => tokenClaims().sub || null;
const isAdmin = () => (tokenClaims().roles || []).includes('ROLE_ADMIN');

// ---------- api ----------
async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth && getToken()) headers.Authorization = `Bearer ${getToken()}`;

  const res = await fetch(path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  const text = await res.text();
  const data = text ? safeJson(text) : null;

  if (res.status === 401 && auth) { logout(); throw new ApiError(401, 'Sessão expirada. Entre novamente.'); }
  if (!res.ok) throw new ApiError(res.status, (data && (data.error || data.message)) || `HTTP ${res.status}`, data);
  return data;
}
const safeJson = (s) => { try { return JSON.parse(s); } catch { return null; } };
class ApiError extends Error { constructor(status, msg, data) { super(msg); this.status = status; this.data = data; } }

// ---------- toasts ----------
function toast(message, variant = 'primary') {
  const el = document.createElement('div');
  el.className = `toast align-items-center text-bg-${variant} border-0 show`;
  el.role = 'alert';
  el.innerHTML = `<div class="d-flex"><div class="toast-body">${escapeHtml(message)}</div>
    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>`;
  $('#toasts').append(el);
  new bootstrap.Toast(el, { delay: 4000 }).show();
  el.addEventListener('hidden.bs.toast', () => el.remove());
}
const escapeHtml = (s) => String(s).replace(/[&<>"']/g, (c) => (
  { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

// ---------- views ----------
function showAuth() {
  $('#auth-view').classList.remove('d-none');
  $('#app-view').classList.add('d-none');
}
function showApp() {
  $('#auth-view').classList.add('d-none');
  $('#app-view').classList.remove('d-none');
  $('#whoami').textContent = tokenSubject() || '';
  $('#btn-new-plan').classList.toggle('d-none', !isAdmin());   // admin-only UI
  loadBalance();
  loadPlans();
  loadMySubs();
}
function logout() { clearToken(); showAuth(); }

// ---------- balance ----------
const money = (v) => Number(v).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

async function loadBalance() {
  try {
    const me = await api('/user/me');
    $('#balance').textContent = money(me.balance);
  } catch { $('#balance').textContent = '—'; }
}

async function deposit() {
  const raw = prompt('Quanto deseja adicionar de saldo? (R$)');
  if (raw == null) return;
  const amount = Number(String(raw).replace(',', '.'));
  if (!(amount > 0)) { toast('Valor inválido.', 'warning'); return; }
  try {
    const me = await api('/user/me/deposit', { method: 'POST', body: { amount } });
    $('#balance').textContent = money(me.balance);
    toast('Saldo adicionado.', 'success');
  } catch (e) {
    const detail = e.data && Array.isArray(e.data.fieldsError) && e.data.fieldsError.length
      ? e.data.fieldsError.map((x) => x.message).join(' · ') : e.message;
    toast(detail, 'danger');
  }
}

async function loadPlans() {
  const body = $('#plans-body');
  body.innerHTML = `<tr><td colspan="5" class="text-center text-secondary py-4">Carregando…</td></tr>`;
  try {
    const plans = await api('/plan');
    if (!plans.length) {
      body.innerHTML = `<tr><td colspan="5" class="text-center text-secondary py-4">Nenhum plano cadastrado.</td></tr>`;
      return;
    }
    body.innerHTML = plans.map((p) => `
      <tr>
        <td class="text-secondary">${p.id}</td>
        <td class="fw-semibold">${escapeHtml(p.name)}</td>
        <td class="text-end">${money(p.price)}</td>
        <td>${p.active
          ? '<span class="badge text-bg-success">ativo</span>'
          : '<span class="badge text-bg-secondary">inativo</span>'}</td>
        <td class="text-end">
          <button class="btn btn-sm btn-primary" data-subscribe="${p.id}" ${p.active ? '' : 'disabled'}>Assinar</button>
        </td>
      </tr>`).join('');
  } catch (e) {
    body.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-4">${escapeHtml(e.message)}</td></tr>`;
  }
}

async function subscribe(planId) {
  try {
    await api('/subscription', { method: 'POST', body: { planId: Number(planId) } });
    toast('Assinatura criada com sucesso.', 'success');
    loadBalance();
    loadMySubs();
  } catch (e) {
    if (e.status === 402) toast('Saldo insuficiente. Adicione saldo antes de assinar.', 'warning');
    else toast(e.message, e.status === 409 ? 'warning' : 'danger');
  }
}

// ---------- my subscriptions ----------
// Backend sends "2026-09-03" / "2026-10-02T23:59:59". `new Date("2026-09-03")`
// is parsed as UTC midnight and shifts a day back in negative-offset zones, so
// build the date from its calendar parts and render it as local.
const fmtDate = (s) => {
  if (!s) return '—';
  const [y, mo, d] = s.split('T')[0].split('-').map(Number);
  return new Date(y, mo - 1, d).toLocaleDateString('pt-BR');
};
const statusBadge = (s) => ({
  ACTIVED: 'text-bg-success', CANCELED: 'text-bg-secondary', PENDING_PAYMENT: 'text-bg-warning',
}[s] || 'text-bg-light');

async function loadMySubs() {
  const body = $('#subs-body');
  body.innerHTML = `<tr><td colspan="8" class="text-center text-secondary py-4">Carregando…</td></tr>`;
  try {
    const subs = await api('/subscription/me');
    if (!subs.length) {
      body.innerHTML = `<tr><td colspan="8" class="text-center text-secondary py-4">Você ainda não tem assinaturas.</td></tr>`;
      return;
    }
    body.innerHTML = subs.map((s) => `
      <tr>
        <td class="text-secondary">${s.id}</td>
        <td class="fw-semibold">${escapeHtml(s.planName)}</td>
        <td class="text-end">${money(s.amount)}</td>
        <td>${fmtDate(s.startDate)}</td>
        <td>${fmtDate(s.endDate)}</td>
        <td>${s.canceledAt ? fmtDate(s.canceledAt) : '—'}</td>
        <td><span class="badge ${statusBadge(s.status)}">${s.status.toLowerCase()}</span></td>
        <td class="text-end">
          ${s.status === 'ACTIVED'
            ? `<button class="btn btn-sm btn-outline-danger" data-cancel="${s.id}">Cancelar</button>` : ''}
        </td>
      </tr>`).join('');
  } catch (e) {
    body.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">${escapeHtml(e.message)}</td></tr>`;
  }
}

async function cancelSub(id) {
  if (!confirm('Cancelar esta assinatura?')) return;
  try {
    await api(`/subscription/${id}/cancel`, { method: 'PATCH' });
    toast('Assinatura cancelada.', 'success');
    loadMySubs();
  } catch (e) {
    toast(e.message, e.status === 409 ? 'warning' : 'danger');
  }
}

// ---------- wire up ----------
$('#form-login').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const f = new FormData(ev.target);
  try {
    const r = await api('/auth/login', { auth: false, method: 'POST', body: { email: f.get('email'), password: f.get('password') } });
    setToken(r.token);
    showApp();
  } catch (e) { toast(e.message, 'danger'); }
});

$('#form-register').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const f = new FormData(ev.target);
  try {
    await api('/auth/register', { auth: false, method: 'POST', body: { name: f.get('name'), email: f.get('email'), password: f.get('password') } });
    toast('Conta criada. Faça login.', 'success');
    const email = f.get('email');
    ev.target.reset();
    $('[data-bs-target="#form-login"]').click();          // let Bootstrap handle the tab switch
    $('#form-login [name="email"]').value = email;         // pre-fill so the user only types the password
  } catch (e) {
    const detail = e.data && Array.isArray(e.data.fieldsError) && e.data.fieldsError.length
      ? e.data.fieldsError.map((x) => x.message).join(' · ') : e.message;
    toast(detail, e.status === 409 ? 'warning' : 'danger');
  }
});

$('#form-plan').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const f = new FormData(ev.target);
  try {
    await api('/plan', { method: 'POST', body: {
      namePlan: f.get('namePlan'), description: f.get('description'),
      price: Number(f.get('price')), billingCycle: f.get('billingCycle'),
    }});
    toast('Plano criado.', 'success');
    ev.target.reset();
    loadPlans();
  } catch (e) {
    toast(e.status === 403 ? 'Apenas administradores podem criar planos.' : e.message,
      e.status === 403 ? 'warning' : 'danger');
  }
});

$('#btn-logout').addEventListener('click', logout);
$('#btn-deposit').addEventListener('click', deposit);
$('#subs-body').addEventListener('click', (ev) => {
  const btn = ev.target.closest('[data-cancel]');
  if (btn) cancelSub(btn.dataset.cancel);
});
$('#plans-body').addEventListener('click', (ev) => {
  const btn = ev.target.closest('[data-subscribe]');
  if (btn) subscribe(btn.dataset.subscribe);
});

// ---------- boot ----------
getToken() ? showApp() : showAuth();
