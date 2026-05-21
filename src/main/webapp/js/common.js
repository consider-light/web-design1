/* ===== 公共工具函数 & UI 组件 ===== */

// ---------- 当前用户 ----------
function getCurrentUser() {
  try {
    const u = localStorage.getItem('currentUser');
    return u ? JSON.parse(u) : null;
  } catch { return null; }
}

function setCurrentUser(user) {
  localStorage.setItem('currentUser', JSON.stringify(user));
}

function clearCurrentUser() {
  localStorage.removeItem('currentUser');
}

// ---------- 导航栏 ----------
function renderNavbar() {
  const user = getCurrentUser();
  const nav = document.getElementById('navbar');
  if (!nav) return;

  const isAdmin = user && user.role === 'admin';

  nav.innerHTML = `
  <nav class="navbar navbar-expand-lg fixed-top" id="mainNav">
    <div class="container">
      <a class="navbar-brand" href="${getRoot()}index.html">
        <span class="brand-icon">&#x25C8;</span>CampusRes
      </a>
      <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navContent">
        <span class="navbar-toggler-icon"></span>
      </button>
      <div class="collapse navbar-collapse" id="navContent">
        <ul class="navbar-nav me-auto">
          <li class="nav-item"><a class="nav-link" href="${getRoot()}pages/resources.html">资源市场</a></li>
          <li class="nav-item"><a class="nav-link" href="${getRoot()}pages/articles.html">教程课堂</a></li>
        </ul>
        <div class="search-box-nav d-none d-lg-flex" id="navSearchBox">
          <input type="text" id="navSearchInput" placeholder="搜索资源..." />
          <button id="navSearchBtn"><i class="bi bi-search"></i></button>
        </div>
        <div class="nav-auth ms-3" id="navAuth">${renderAuthArea(user, isAdmin)}</div>
      </div>
    </div>
  </nav>`;

  document.getElementById('navSearchBtn').addEventListener('click', () => {
    const kw = document.getElementById('navSearchInput').value.trim();
    if (kw) location.href = getRoot() + 'pages/resources.html?keyword=' + encodeURIComponent(kw);
  });
  document.getElementById('navSearchInput').addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      const kw = e.target.value.trim();
      if (kw) location.href = getRoot() + 'pages/resources.html?keyword=' + encodeURIComponent(kw);
    }
  });

  bindLogout();
}

function renderAuthArea(user, isAdmin) {
  if (!user) {
    return `<a href="${getRoot()}pages/login.html" class="btn btn-outline-light btn-sm me-2">登录</a>
            <a href="${getRoot()}pages/register.html" class="btn btn-accent btn-sm">注册</a>`;
  }
  return `
    <div class="dropdown" id="userDropdown">
      <button class="btn btn-user dropdown-toggle" data-bs-toggle="dropdown">
        <i class="bi bi-person-circle me-1"></i>${escHtml(user.username)}
        ${isAdmin ? '<span class="badge badge-admin ms-1">ADMIN</span>' : ''}
      </button>
      <ul class="dropdown-menu dropdown-menu-end">
        <li><a class="dropdown-item" href="${getRoot()}pages/upload.html"><i class="bi bi-cloud-upload me-2"></i>上传资源</a></li>
        ${isAdmin ? `<li><hr class="dropdown-divider"></li>
        <li><a class="dropdown-item" href="${getRoot()}pages/admin/users.html"><i class="bi bi-shield-lock me-2"></i>管理后台</a></li>` : ''}
        <li><hr class="dropdown-divider"></li>
        <li><a class="dropdown-item" href="#" id="btnLogout"><i class="bi bi-box-arrow-right me-2"></i>退出登录</a></li>
      </ul>
    </div>`;
}

function bindLogout() {
  const btn = document.getElementById('btnLogout');
  if (btn) {
    btn.addEventListener('click', async (e) => {
      e.preventDefault();
      try { await API.get('/api/logout'); } catch {}
      clearCurrentUser();
      location.href = getRoot() + 'index.html';
    });
  }
}

// ---------- 登录/权限检查 ----------
async function checkAuth() {
  const user = getCurrentUser();
  if (!user) {
    try {
      const data = await API.get('/api/user/current');
      if (data) { setCurrentUser(data); return data; }
    } catch {
      showToast('请先登录', 'warning');
      setTimeout(() => { location.href = getRoot() + 'pages/login.html'; }, 800);
      return null;
    }
  }
  return user;
}

async function checkAdmin() {
  const user = await checkAuth();
  if (!user) return null;
  if (user.role !== 'admin') {
    showToast('仅管理员可访问此页面', 'error');
    setTimeout(() => { location.href = getRoot() + 'index.html'; }, 800);
    return null;
  }
  return user;
}

// ---------- 页脚 ----------
function renderFooter() {
  const el = document.getElementById('footer');
  if (!el) return;
  el.innerHTML = `
  <footer class="site-footer">
    <div class="container">
      <div class="row g-4">
        <div class="col-md-4">
          <h5><span class="brand-icon">&#x25C8;</span>CampusRes</h5>
          <p class="footer-desc">校园上网指南资源库 — 解决大学生上网刚需的一站式平台。</p>
        </div>
        <div class="col-md-4">
          <h6>快捷导航</h6>
          <ul class="footer-links">
            <li><a href="${getRoot()}pages/resources.html">资源市场</a></li>
            <li><a href="${getRoot()}pages/articles.html">教程课堂</a></li>
            <li><a href="${getRoot()}pages/upload.html">上传资源</a></li>
          </ul>
        </div>
        <div class="col-md-4">
          <h6>关于我们</h6>
          <p class="footer-desc">计算机大二课程项目 · 四人团队 · 全栈实践</p>
        </div>
      </div>
      <div class="footer-bottom">
        <p>&copy; 2026 CampusRes. Made with <i class="bi bi-heart-fill text-accent"></i> by 四人小组</p>
      </div>
    </div>
  </footer>`;
}

// ---------- Toast 提示 ----------
function showToast(msg, type = 'info') {
  var container = document.getElementById('toastContainer') || createToastContainer();
  const icons = { success: 'check-circle', error: 'x-circle', warning: 'exclamation-triangle', info: 'info-circle' };
  const toast = document.createElement('div');
  toast.className = `toast-item toast-${type}`;
  toast.innerHTML = `<i class="bi bi-${icons[type] || 'info-circle'} me-2"></i>${msg}`;
  container.appendChild(toast);
  setTimeout(() => { toast.classList.add('show'); }, 10);
  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 300);
  }, 3000);
}
window._toast = showToast;

function createToastContainer() {
  const c = document.createElement('div');
  c.id = 'toastContainer';
  c.className = 'toast-container';
  document.body.appendChild(c);
  return c;
}

// ---------- 小工具 ----------
function formatDate(dateStr) {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  if (isNaN(d.getTime())) return dateStr;
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function formatFileSize(bytes) {
  if (!bytes || bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  let i = 0;
  let size = bytes;
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++; }
  return size.toFixed(i === 0 ? 0 : 1) + ' ' + units[i];
}

function getQueryParam(name) {
  const p = new URLSearchParams(location.search);
  return p.get(name) || '';
}

function escHtml(str) {
  if (!str) return '';
  const d = document.createElement('div');
  d.textContent = str;
  return d.innerHTML;
}

function getRoot() {
  const depth = (location.pathname.match(/\//g) || []).length;
  if (location.pathname.endsWith('index.html') || location.pathname === '/' || location.pathname.endsWith('/webapp/')) return '';
  if (location.pathname.includes('/admin/')) return '../../';
  if (location.pathname.includes('/pages/')) return '../';
  return '';
}

// ---------- 初始化 ----------
document.addEventListener('DOMContentLoaded', () => {
  renderNavbar();
  renderFooter();
});
