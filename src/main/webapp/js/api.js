/**
 * 统一 API 请求工具
 * 后端返回格式: { code: number, message: string, data: any }
 * code: 200 成功 | 400 参数错误 | 401 未登录 | 403 权限不足 | 404 不存在 | 500 服务器错误
 */
// Auto-detect context path (e.g., /demo) from current page URL.
// Works for both /demo/index.html (returns /demo) and ROOT-deploy /index.html (returns '').
const BASE = '/demo-1.0-SNAPSHOT';

const API = {
  /**
   * @param {string} url   - 接口路径，如 '/api/resources'
   * @param {object} options - { method, body, params, formData, raw }
   *   - method: GET(默认) / POST / DELETE
   *   - body: JSON 对象 (POST 且非 FormData 时用)
   *   - formData: FormData 实例 (文件上传)
   *   - params: URL 查询参数对象 { categoryId: 1, keyword: 'steam' }
   *   - raw: true 时直接返回 Response（用于文件下载）
   * @returns {Promise<any>}
   */
  async request(url, options = {}) {
    const method = options.method || 'GET';
    const params = options.params;
    const raw = options.raw || false;

    let fullUrl = BASE + url;
    if (params) {
      const qs = Object.entries(params)
        .filter(([, v]) => v !== null && v !== undefined && v !== '')
        .map(([k, v]) => encodeURIComponent(k) + '=' + encodeURIComponent(v))
        .join('&');
      if (qs) fullUrl += '?' + qs;
    }

    const fetchOptions = { method, credentials: 'same-origin' };

    if (options.formData) {
      fetchOptions.body = options.formData;
    } else if (options.body && method !== 'GET') {
      fetchOptions.headers = { 'Content-Type': 'application/json' };
      fetchOptions.body = JSON.stringify(options.body);
    }

    try {
      const res = await fetch(fullUrl, fetchOptions);

      if (raw) return res;

      const json = await res.json();

      if (json.code === 401) {
        if (window._toast) window._toast('请先登录', 'warning');
        localStorage.removeItem('currentUser');
        var root = (typeof getRoot === 'function') ? getRoot() : '';
        setTimeout(() => { location.href = root + 'pages/login.html'; }, 1000);
        throw new Error('未登录');
      }

      if (json.code === 403) {
        if (window._toast) window._toast('权限不足', 'error');
        throw new Error('权限不足');
      }

      if (json.code !== 200) {
        if (window._toast) window._toast(json.message || '请求失败', 'error');
        throw new Error(json.message || '请求失败');
      }

      return json.data;
    } catch (err) {
      if (err.name === 'TypeError' && err.message === 'Failed to fetch') {
        if (window._toast) window._toast('网络连接失败，请检查后端服务', 'error');
      }
      throw err;
    }
  },

  get(url, params)     { return this.request(url, { method: 'GET', params }); },
  post(url, body)      { return this.request(url, { method: 'POST', body }); },
  del(url)             { return this.request(url, { method: 'DELETE' }); },
  upload(url, formData){ return this.request(url, { method: 'POST', formData }); },
  download(url)        { return this.request(url, { method: 'GET', raw: true }); },
};
