(() => {
  'use strict';

  const HP = window.HolidayPlanner || {};

  class ConfirmationDialog {
    constructor(element) {
      this.element = element;
      this.panel = element?.querySelector('.confirm-dialog');
      this.title = element?.querySelector('#hpConfirmTitle');
      this.message = element?.querySelector('#hpConfirmMessage');
      this.accept = element?.querySelector('#hpConfirmAccept');
      this.cancel = element?.querySelector('#hpConfirmCancel');
      this.symbol = element?.querySelector('[data-confirm-symbol]');
      this.resolver = null;
      this.previousFocus = null;
      this.hideTimer = null;
      this.onKeydown = this.onKeydown.bind(this);
      this.accept?.addEventListener('click', () => this.close(true));
      this.cancel?.addEventListener('click', () => this.close(false));
      this.element?.addEventListener('mousedown', event => {
        if (event.target === this.element) this.close(false);
      });
    }

    ask(options = {}) {
      if (!this.element || !this.panel || this.resolver) return Promise.resolve(false);
      if (this.hideTimer !== null) {
        window.clearTimeout(this.hideTimer);
        this.hideTimer = null;
      }
      const tone = options.tone === 'warning' ? 'warning' : 'danger';
      this.title.textContent = options.title || 'Bạn có chắc chắn?';
      this.message.textContent = options.message || 'Thao tác này sẽ thay đổi dữ liệu của bạn.';
      this.accept.textContent = options.acceptLabel || 'Xác nhận';
      this.accept.className = `btn ${tone === 'warning' ? 'btn-primary' : 'btn-danger'}`;
      this.symbol.textContent = '!';
      this.panel.dataset.tone = tone;
      this.previousFocus = document.activeElement;
      this.element.hidden = false;
      document.body.classList.add('dialog-open');
      document.addEventListener('keydown', this.onKeydown);
      const promise = new Promise(resolve => { this.resolver = resolve; });
      requestAnimationFrame(() => {
        if (!this.resolver) return;
        this.element.classList.add('is-open');
        this.cancel?.focus({preventScroll: true});
      });
      return promise;
    }

    close(accepted) {
      if (!this.resolver) return;
      const resolve = this.resolver;
      this.resolver = null;
      document.removeEventListener('keydown', this.onKeydown);
      this.element.classList.remove('is-open');
      document.body.classList.remove('dialog-open');
      const focusTarget = this.previousFocus;
      this.previousFocus = null;
      this.hideTimer = window.setTimeout(() => {
        this.element.hidden = true;
        this.hideTimer = null;
      }, HP.reducedMotion ? 0 : 180);
      if (focusTarget instanceof HTMLElement) focusTarget.focus({preventScroll: true});
      resolve(accepted);
    }

    onKeydown(event) {
      if (event.key === 'Escape') {
        event.preventDefault();
        this.close(false);
        return;
      }
      if (event.key !== 'Tab' || !this.panel) return;
      const focusable = [...this.panel.querySelectorAll('button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])')];
      if (!focusable.length) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }
  }

  const dialog = new ConfirmationDialog(document.getElementById('hpConfirmDialog'));
  HP.confirm = options => dialog.ask(options);
  const bypass = new WeakSet();
  const pending = new WeakSet();

  document.querySelectorAll('form[data-confirm]').forEach(form => {
    form.addEventListener('submit', async event => {
      if (bypass.has(form)) {
        bypass.delete(form);
        return;
      }
      const conditionalField = form.dataset.confirmWhenField;
      if (conditionalField) {
        const control = form.elements.namedItem(conditionalField);
        if (!(control instanceof HTMLElement) || control.value !== form.dataset.confirmWhenValue) return;
      }
      event.preventDefault();
      if (pending.has(form)) return;
      pending.add(form);
      const submitter = event.submitter;
      try {
        const accepted = await dialog.ask({
          title: form.dataset.confirmTitle || 'Xác nhận thao tác',
          message: form.dataset.confirm,
          acceptLabel: form.dataset.confirmAccept || 'Xác nhận',
          tone: form.dataset.confirmTone || 'danger'
        });
        if (!accepted) return;
        bypass.add(form);
        form.requestSubmit(submitter instanceof HTMLElement ? submitter : undefined);
      } finally {
        pending.delete(form);
      }
    });
  });

  window.HolidayPlanner = HP;
})();
