(() => {
  'use strict';

  const HP = window.HolidayPlanner || {};
  HP.reducedMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;

  class ToastManager {
    constructor(host) {
      this.host = host;
      this.sequence = 0;
      this.timers = new WeakMap();
      host?.querySelectorAll('[data-toast]').forEach(toast => this.activate(toast));
    }

    show(type, title, message, duration = 5200) {
      if (!this.host || !message) return null;
      const safeType = ['success', 'error', 'warning', 'info'].includes(type) ? type : 'info';
      const toast = document.createElement('article');
      toast.className = `hp-toast hp-toast-${safeType}`;
      toast.dataset.toast = '';
      toast.dataset.toastDuration = String(duration);
      toast.setAttribute('role', safeType === 'error' ? 'alert' : 'status');

      const icon = document.createElement('span');
      icon.className = 'toast-icon';
      icon.setAttribute('aria-hidden', 'true');
      icon.textContent = {success: '✓', error: '!', warning: '!', info: 'i'}[safeType];

      const content = document.createElement('div');
      content.className = 'toast-content';
      const heading = document.createElement('strong');
      heading.textContent = title || {success: 'Thành công', error: 'Không thể thực hiện', warning: 'Lưu ý', info: 'Thông tin'}[safeType];
      const copy = document.createElement('span');
      copy.textContent = message;
      content.append(heading, copy);

      const close = document.createElement('button');
      close.className = 'toast-close';
      close.type = 'button';
      close.dataset.toastClose = '';
      close.setAttribute('aria-label', 'Đóng thông báo');
      close.textContent = '×';

      const progress = document.createElement('span');
      progress.className = 'toast-progress';
      progress.setAttribute('aria-hidden', 'true');
      toast.append(icon, content, close, progress);
      this.host.append(toast);
      this.activate(toast);
      return toast;
    }

    activate(toast) {
      if (!toast || toast.dataset.toastReady === 'true') return;
      toast.dataset.toastReady = 'true';
      toast.classList.add('is-pending');
      toast.id ||= `hp-toast-${++this.sequence}`;
      const duration = Number.parseInt(toast.dataset.toastDuration || '5200', 10);
      const progress = toast.querySelector('.toast-progress');
      progress?.style.setProperty('--hp-toast-duration', `${duration}ms`);
      toast.querySelector('[data-toast-close]')?.addEventListener('click', () => this.dismiss(toast));

      const state = {remaining: duration, startedAt: 0, timer: null};
      const schedule = () => {
        if (state.timer !== null || state.remaining <= 0) return;
        state.startedAt = performance.now();
        state.timer = window.setTimeout(() => this.dismiss(toast), state.remaining);
        progress?.classList.remove('is-paused');
      };
      const pause = () => {
        if (state.timer === null) return;
        window.clearTimeout(state.timer);
        state.timer = null;
        state.remaining = Math.max(0, state.remaining - (performance.now() - state.startedAt));
        progress?.classList.add('is-paused');
      };
      this.timers.set(toast, state);
      toast.addEventListener('mouseenter', pause);
      toast.addEventListener('mouseleave', schedule);
      schedule();
      requestAnimationFrame(() => {
        toast.classList.remove('is-pending');
        toast.classList.add('is-visible');
      });
    }

    dismiss(toast) {
      if (!toast?.isConnected || toast.classList.contains('is-leaving')) return;
      const state = this.timers.get(toast);
      if (state?.timer !== null) window.clearTimeout(state.timer);
      this.timers.delete(toast);
      toast.classList.add('is-leaving');
      window.setTimeout(() => toast.remove(), HP.reducedMotion ? 0 : 260);
    }
  }

  const toastManager = new ToastManager(document.getElementById('toastHost'));
  HP.toast = (type, title, message, duration) => toastManager.show(type, title, message, duration);

  document.querySelectorAll('img[data-fallback]').forEach(image => {
    image.addEventListener('error', () => {
      const fallback = image.dataset.fallback;
      if (!fallback) return;
      const fallbackUrl = new URL(fallback, window.location.origin).href;
      if (image.src !== fallbackUrl) image.src = fallbackUrl;
    });
  });

  const currentPath = window.location.pathname.replace(/\/$/, '') || '/';
  document.querySelectorAll('.hp-navbar [data-nav-link]').forEach(link => {
    const target = new URL(link.href, window.location.origin).pathname.replace(/\/$/, '') || '/';
    const active = currentPath === target || (target !== '/dashboard' && target !== '/' && currentPath.startsWith(`${target}/`));
    if (!active) return;
    link.classList.add('active');
    link.setAttribute('aria-current', 'page');
  });

  const copyText = async value => {
    if (!value) return false;
    try {
      await navigator.clipboard.writeText(value);
      return true;
    } catch (_error) {
      const helper = document.createElement('textarea');
      helper.value = value;
      helper.setAttribute('readonly', '');
      helper.style.position = 'fixed';
      helper.style.opacity = '0';
      document.body.append(helper);
      helper.select();
      const copied = document.execCommand('copy');
      helper.remove();
      return copied;
    }
  };

  document.querySelectorAll('[data-copy-value], [data-copy-target]').forEach(button => {
    button.addEventListener('click', async () => {
      const target = button.dataset.copyTarget ? document.getElementById(button.dataset.copyTarget) : null;
      const value = button.dataset.copyValue || target?.textContent?.trim();
      const copied = await copyText(value);
      HP.toast(copied ? 'success' : 'error', copied ? 'Đã sao chép' : 'Không thể sao chép', copied ? 'Mã đặt chỗ đã được lưu vào bộ nhớ tạm.' : 'Vui lòng chọn và sao chép mã theo cách thủ công.', 2800);
    });
  });

  const tabs = document.getElementById('tripTabs');
  if (tabs && window.bootstrap?.Tab) {
    const storageKey = `hp-trip-tab:${window.location.pathname}`;
    const allowed = new Set(['overview', 'itinerary', 'expenses', 'checklist', 'bookings']);
    const params = new URLSearchParams(window.location.search);
    const serverTab = document.getElementById('activeTripTab')?.dataset.tab;
    const queryTab = params.get('tab');
    const hashTab = window.location.hash.replace(/^#/, '');
    const storedTab = sessionStorage.getItem(storageKey)?.replace(/^#/, '');
    const requested = [serverTab, queryTab, params.has('expenseCategory') ? 'expenses' : null, hashTab, storedTab]
      .find(value => allowed.has(value)) || 'overview';
    const trigger = tabs.querySelector(`[data-bs-target="#${requested}"]`);
    if (trigger) window.bootstrap.Tab.getOrCreateInstance(trigger).show();

    tabs.querySelectorAll('[data-bs-toggle="pill"]').forEach(tab => {
      tab.addEventListener('shown.bs.tab', event => {
        const target = event.target.dataset.bsTarget;
        if (!target) return;
        sessionStorage.setItem(storageKey, target);
        const nextUrl = `${window.location.pathname}${window.location.search}${target}`;
        history.replaceState(history.state, '', nextUrl);
      });
    });
  }

  window.HolidayPlanner = HP;
})();
