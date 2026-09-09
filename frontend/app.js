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

// pulls the friendliest message out of an API error (field errors first)
const errText = (e) => (e.data && Array.isArray(e.data.fieldsError) && e.data.fieldsError.length)
  ? e.data.fieldsError.map((x) => x.message).join(' · ')
  : e.message;

// ---------- ui helpers ----------
const escapeHtml = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => (
  { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

const money = (v) => Number(v).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

// Backend sends "2026-09-03" / "2026-10-02T23:59:59". `new Date("2026-09-03")` is
// parsed as UTC midnight and shifts a day back in negative-offset zones, so build
// the date from its calendar parts and render it as local.
const fmtDate = (s) => {
  if (!s) return '—';
  const [y, mo, d] = s.split('T')[0].split('-').map(Number);
  return new Date(y, mo - 1, d).toLocaleDateString('pt-BR');
};

const CYCLE = { MONTHLY: 'mensal', QUARTERLY: 'trimestral', YEARLY: 'anual' };
const STATUS = {
  ACTIVED: { cls: 'p-ok', label: 'ativa' },
  CANCELED: { cls: 'p-off', label: 'cancelada' },
  PENDING_PAYMENT: { cls: 'p-warn', label: 'pendente' },
};

const TOAST_ICON = { success: 'check-circle-fill', danger: 'x-circle-fill', warning: 'exclamation-triangle-fill', primary: 'info-circle-fill' };

function toast(message, variant = 'primary') {
  const el = document.createElement('div');
  el.className = `toast align-items-center text-bg-${variant} border-0 show`;
  el.role = 'alert';
  el.innerHTML = `<div class="d-flex">
      <div class="toast-body d-flex align-items-center gap-2">
        <i class="bi bi-${TOAST_ICON[variant] || TOAST_ICON.primary}"></i>${escapeHtml(message)}
      </div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
    </div>`;
  $('#toasts').append(el);
  new bootstrap.Toast(el, { delay: 4200 }).show();
  el.addEventListener('hidden.bs.toast', () => el.remove());
}

const planSkeleton = () => `
  <div class="col-md-6 col-xl-4">
    <div class="surface plan">
      <div class="skel mb-2" style="width:45%"></div>
      <div class="skel mb-4" style="width:75%;height:9px"></div>
      <div class="skel mt-auto" style="width:55%;height:26px"></div>
      <div class="skel mt-3" style="height:36px"></div>
    </div>
  </div>`.repeat(3);

const rowSkeleton = (cols) =>
  `<tr>${`<td><div class="skel"></div></td>`.repeat(cols)}</tr>`.repeat(3);

const emptyRow = (cols, icon, text) =>
  `<tr><td colspan="${cols}"><div class="empty"><i class="bi bi-${icon}"></i>${escapeHtml(text)}</div></td></tr>`;

// ---------- views ----------
function showAuth() {
  $('#auth-view').classList.remove('d-none');
  $('#app-view').classList.add('d-none');
}

function showApp() {
  $('#auth-view').classList.add('d-none');
  $('#app-view').classList.remove('d-none');

  const email = tokenSubject() || '';
  const av = $('#avatar');
  av.textContent = (email[0] || '?').toUpperCase();
  av.title = email;

  $('#btn-new-plan').classList.toggle('d-none', !isAdmin());   // admin-only UI

  loadBalance();
  loadPlans();
  loadMySubs();
}

function logout() { clearToken(); showAuth(); }

// ---------- balance ----------
async function loadBalance() {
  try {
    const me = await api('/user/me');
    const v = money(me.balance);
    $('#balance').textContent = v;
    $('#stat-balance').textContent = v;
  } catch {
    $('#balance').textContent = '—';
    $('#stat-balance').textContent = '—';
  }
}

// ---------- plans ----------
async function loadPlans() {
  const grid = $('#plans-grid');
  grid.innerHTML = planSkeleton();
  try {
    const plans = await api('/plan');
    if (!plans.length) {
      grid.innerHTML = `<div class="col-12"><div class="surface"><div class="empty">
        <i class="bi bi-inbox"></i>Nenhum plano cadastrado ainda.</div></div></div>`;
      return;
    }
    grid.innerHTML = plans.map((p) => `
      <div class="col-md-6 col-xl-4">
        <div class="surface plan ${p.active ? '' : 'off'}">
          <div class="d-flex justify-content-between align-items-start gap-2 mb-2">
            <div class="fw-semibold fs-5">${escapeHtml(p.name)}</div>
            <span class="pill ${p.active ? 'p-ok' : 'p-off'}">
              <span class="dot"></span>${p.active ? 'ativo' : 'inativo'}</span>
          </div>
          <p class="text-muted small mb-4">${escapeHtml(p.description || '')}</p>
          <div class="mt-auto">
            <div class="d-flex align-items-baseline gap-2">
              <span class="price">${money(p.price)}</span>
              <span class="text-muted small">/ ${CYCLE[p.billingCycle] || ''}</span>
            </div>
            <button class="btn ${p.active ? 'btn-grad' : 'btn-ghost'} w-100 mt-3"
                    data-subscribe="${p.id}" ${p.active ? '' : 'disabled'}>
              ${p.active ? '<i class="bi bi-lightning-charge-fill me-1"></i>Assinar' : 'Indisponível'}
            </button>
          </div>
        </div>
      </div>`).join('');
  } catch (e) {
    grid.innerHTML = `<div class="col-12"><div class="surface"><div class="empty text-danger">
      <i class="bi bi-exclamation-octagon"></i>${escapeHtml(e.message)}</div></div></div>`;
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
    else toast(errText(e), e.status === 409 ? 'warning' : 'danger');
  }
}

// ---------- my subscriptions ----------
async function loadMySubs() {
  const body = $('#subs-body');
  body.innerHTML = rowSkeleton(8);
  try {
    const subs = await api('/subscription/me');

    $('#stat-active').textContent = subs.filter((s) => s.status === 'ACTIVED').length;
    $('#stat-spent').textContent = money(subs.reduce((t, s) => t + Number(s.amount || 0), 0));

    if (!subs.length) {
      body.innerHTML = emptyRow(8, 'journal-text', 'Você ainda não tem assinaturas. Escolha um plano acima.');
      return;
    }
    body.innerHTML = subs.map((s) => {
      const st = STATUS[s.status] || { cls: 'p-off', label: String(s.status).toLowerCase() };
      return `
      <tr>
        <td class="text-muted">#${s.id}</td>
        <td class="fw-semibold">${escapeHtml(s.planName)}</td>
        <td class="text-end fw-semibold">${money(s.amount)}</td>
        <td class="text-muted">${fmtDate(s.startDate)}</td>
        <td class="text-muted">${fmtDate(s.endDate)}</td>
        <td class="text-muted">${s.canceledAt ? fmtDate(s.canceledAt) : '—'}</td>
        <td><span class="pill ${st.cls}"><span class="dot"></span>${st.label}</span></td>
        <td class="text-end">
          ${s.status === 'ACTIVED'
            ? `<button class="btn btn-ghost btn-sm" data-cancel="${s.id}">Cancelar</button>` : ''}
        </td>
      </tr>`;
    }).join('');
  } catch (e) {
    body.innerHTML = `<tr><td colspan="8"><div class="empty text-danger">
      <i class="bi bi-exclamation-octagon"></i>${escapeHtml(e.message)}</div></td></tr>`;
  }
}

// ---------- deposit ----------
const depositModal = () => bootstrap.Modal.getOrCreateInstance($('#modal-deposit'));

$('#btn-deposit').addEventListener('click', () => {
  $('#form-deposit').reset();
  depositModal().show();
});

document.querySelectorAll('.quick-amt').forEach((b) =>
  b.addEventListener('click', () => { $('#form-deposit [name="amount"]').value = b.dataset.amt; }));

$('#form-deposit').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const amount = Number(new FormData(ev.target).get('amount'));
  if (!(amount > 0)) { toast('Informe um valor maior que zero.', 'warning'); return; }
  try {
    const me = await api('/user/me/deposit', { method: 'POST', body: { amount } });
    const v = money(me.balance);
    $('#balance').textContent = v;
    $('#stat-balance').textContent = v;
    depositModal().hide();
    toast(`Saldo atualizado para ${v}.`, 'success');
  } catch (e) { toast(errText(e), 'danger'); }
});

// ---------- cancel ----------
let pendingCancelId = null;
const cancelModal = () => bootstrap.Modal.getOrCreateInstance($('#modal-cancel'));

$('#confirm-cancel').addEventListener('click', async () => {
  const id = pendingCancelId;
  cancelModal().hide();
  if (!id) return;
  try {
    await api(`/subscription/${id}/cancel`, { method: 'PATCH' });
    toast('Assinatura cancelada.', 'success');
    loadMySubs();
  } catch (e) {
    toast(errText(e), e.status === 409 ? 'warning' : 'danger');
  } finally { pendingCancelId = null; }
});

// ---------- auth forms ----------
$('#form-login').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const f = new FormData(ev.target);
  try {
    const r = await api('/auth/login', { auth: false, method: 'POST', body: { email: f.get('email'), password: f.get('password') } });
    setToken(r.token);
    ev.target.reset();
    showApp();
  } catch (e) { toast(errText(e), 'danger'); }
});

$('#form-register').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const f = new FormData(ev.target);
  const email = f.get('email');
  try {
    await api('/auth/register', { auth: false, method: 'POST', body: { name: f.get('name'), email, password: f.get('password') } });
    toast('Conta criada. Faça login para continuar.', 'success');
    ev.target.reset();
    $('[data-bs-target="#form-login"]').click();      // let Bootstrap handle the tab switch
    $('#form-login [name="email"]').value = email;    // pre-fill so only the password is left
    $('#form-login [name="password"]').focus();
  } catch (e) {
    toast(errText(e), e.status === 409 ? 'warning' : 'danger');
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
    toast(e.status === 403 ? 'Apenas administradores podem criar planos.' : errText(e),
      e.status === 403 ? 'warning' : 'danger');
  }
});

// ---------- delegated clicks ----------
$('#btn-logout').addEventListener('click', logout);

$('#plans-grid').addEventListener('click', (ev) => {
  const btn = ev.target.closest('[data-subscribe]');
  if (btn) subscribe(btn.dataset.subscribe);
});

$('#subs-body').addEventListener('click', (ev) => {
  const btn = ev.target.closest('[data-cancel]');
  if (btn) { pendingCancelId = btn.dataset.cancel; cancelModal().show(); }
});

// ---------- boot ----------
getToken() ? showApp() : showAuth();
