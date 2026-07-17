(() => {
  'use strict';

  const HP = window.HolidayPlanner || {};
  const labels = {
    username: 'Email', email: 'Email', password: 'Mật khẩu', currentPassword: 'Mật khẩu hiện tại',
    newPassword: 'Mật khẩu mới', confirmPassword: 'Xác nhận mật khẩu', fullName: 'Họ tên',
    title: 'Tên chuyến đi', status: 'Trạng thái', destinationId: 'Điểm đến', startDate: 'Ngày bắt đầu',
    endDate: 'Ngày kết thúc', peopleCount: 'Số người', budget: 'Ngân sách', notes: 'Ghi chú',
    dayNo: 'Ngày thứ', fromTime: 'Giờ bắt đầu', toTime: 'Giờ kết thúc', activity: 'Hoạt động',
    location: 'Địa điểm', note: 'Ghi chú', category: 'Danh mục', amount: 'Số tiền',
    spentDate: 'Ngày chi', dueDate: 'Hạn hoàn thành', type: 'Loại dịch vụ', provider: 'Nhà cung cấp',
    bookingCode: 'Mã đặt chỗ', price: 'Chi phí', name: 'Tên', city: 'Thành phố', country: 'Quốc gia',
    imageUrl: 'URL ảnh', imageFile: 'Tệp ảnh', description: 'Mô tả', q: 'Từ khóa tìm kiếm', role: 'Vai trò'
  };
  let generatedId = 0;

  const messageFor = control => {
    const label = labels[control.name] || 'Trường này';
    if (control.validity.valueMissing) return `${label} không được để trống.`;
    if (control.validity.typeMismatch) return `${label} chưa đúng định dạng.`;
    if (control.validity.tooShort) return `${label} cần ít nhất ${control.minLength} ký tự.`;
    if (control.validity.tooLong) return `${label} chỉ được tối đa ${control.maxLength} ký tự.`;
    if (control.validity.rangeUnderflow) return `${label} phải từ ${control.min} trở lên.`;
    if (control.validity.rangeOverflow) return `${label} không được vượt quá ${control.max}.`;
    if (control.validity.patternMismatch) return control.dataset.patternMessage || `${label} chưa đáp ứng yêu cầu.`;
    return control.validationMessage || `Vui lòng kiểm tra lại ${label.toLowerCase()}.`;
  };

  const feedbackContainer = control => control.closest('.password-wrap')?.parentElement || control.parentElement;
  const markInvalid = control => {
    if (!(control instanceof HTMLElement)) return;
    control.classList.add('is-invalid');
    control.setAttribute('aria-invalid', 'true');
    const container = feedbackContainer(control);
    let feedback = container?.querySelector('.invalid-feedback, .client-feedback');
    if (!feedback) {
      feedback = document.createElement('div');
      feedback.className = 'invalid-feedback client-feedback';
      container?.append(feedback);
    }
    if (feedback && !feedback.textContent.trim()) feedback.textContent = messageFor(control);
    if (feedback) {
      feedback.id ||= `${control.id || `hp-field-${++generatedId}`}-feedback`;
      const describedBy = new Set((control.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean));
      describedBy.add(feedback.id);
      control.setAttribute('aria-describedby', [...describedBy].join(' '));
    }
  };

  document.querySelectorAll('input:not([type="hidden"]), select, textarea').forEach(control => {
    if (!control.id) control.id = `hp-field-${++generatedId}`;
    const hasLabel = document.querySelector(`label[for="${CSS.escape(control.id)}"]`) || control.closest('label');
    if (!hasLabel && !control.hasAttribute('aria-label') && !control.hasAttribute('aria-labelledby') && labels[control.name]) {
      control.setAttribute('aria-label', labels[control.name]);
    }
    const clearState = () => {
      control.classList.remove('is-invalid');
      control.removeAttribute('aria-invalid');
      const feedback = feedbackContainer(control)?.querySelector('.client-feedback');
      if (feedback) feedback.remove();
    };
    control.addEventListener('input', clearState);
    control.addEventListener('change', clearState);
  });

  document.querySelectorAll('.field-error:not(:empty)').forEach(error => {
    const container = error.parentElement;
    const control = container?.querySelector('input:not([type="hidden"]), select, textarea');
    if (!control) return;
    control.classList.add('is-invalid');
    control.setAttribute('aria-invalid', 'true');
    error.id ||= `${control.id}-server-error`;
    const describedBy = new Set((control.getAttribute('aria-describedby') || '').split(/\s+/).filter(Boolean));
    describedBy.add(error.id);
    control.setAttribute('aria-describedby', [...describedBy].join(' '));
  });

  document.querySelectorAll('[data-password-toggle]').forEach(button => {
    const input = document.getElementById(button.dataset.passwordToggle);
    if (!input) return;
    button.addEventListener('click', () => {
      const show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      button.textContent = show ? 'Ẩn' : 'Hiện';
      button.setAttribute('aria-pressed', String(show));
      button.setAttribute('aria-label', show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
    });
  });

  document.querySelectorAll('[data-match-password]').forEach(control => {
    const source = document.getElementById(control.dataset.matchPassword);
    if (!source) return;
    const validate = () => {
      control.setCustomValidity(control.value && control.value !== source.value ? 'Mật khẩu xác nhận chưa trùng khớp.' : '');
    };
    source.addEventListener('input', validate);
    control.addEventListener('input', validate);
  });

  document.querySelectorAll('[data-password-strength]').forEach(meter => {
    const input = document.getElementById(meter.dataset.passwordStrength);
    if (!input) return;
    const update = () => {
      const value = input.value;
      const score = [value.length >= 8, /[A-Za-zÀ-ỹ]/.test(value), /\d/.test(value), /[^\w\s]/.test(value), value.length >= 12].filter(Boolean).length;
      meter.dataset.score = String(score);
      const label = meter.querySelector('[data-strength-label]');
      if (label) label.textContent = ['Chưa nhập', 'Rất yếu', 'Yếu', 'Khá', 'Mạnh', 'Rất mạnh'][score];
    };
    input.addEventListener('input', update);
    update();
  });

  const startDate = document.querySelector('input[name="startDate"]');
  const endDate = document.querySelector('input[name="endDate"]');
  const validateDateRange = () => {
    if (!startDate || !endDate) return;
    endDate.setCustomValidity(startDate.value && endDate.value && endDate.value < startDate.value ? 'Ngày kết thúc không được trước ngày bắt đầu.' : '');
  };
  startDate?.addEventListener('change', validateDateRange);
  endDate?.addEventListener('change', validateDateRange);

  const submitting = new WeakSet();
  const forms = [...document.querySelectorAll('form')];
  forms.forEach(form => {
    form.addEventListener('submit', event => {
      if (event.defaultPrevented) return;
      validateDateRange();
      if (!form.checkValidity()) {
        event.preventDefault();
        const invalid = [...form.querySelectorAll(':invalid')];
        invalid.forEach(markInvalid);
        invalid[0]?.focus({preventScroll: true});
        invalid[0]?.scrollIntoView({behavior: HP.reducedMotion ? 'auto' : 'smooth', block: 'center'});
        return;
      }
      if (submitting.has(form)) {
        event.preventDefault();
        return;
      }
      submitting.add(form);
      form.querySelectorAll('button[type="submit"], button:not([type])').forEach(button => {
        if (button.disabled) return;
        button.dataset.submitLocked = 'true';
        button.disabled = true;
        button.classList.add('is-loading');
        button.setAttribute('aria-busy', 'true');
      });
      if (form.hasAttribute('data-loading-overlay')) {
        const overlay = document.getElementById('hpLoadingOverlay');
        if (overlay) {
          overlay.hidden = false;
          overlay.setAttribute('aria-hidden', 'false');
        }
      }
    });
  });

  window.addEventListener('pageshow', () => {
    forms.forEach(form => submitting.delete(form));
    document.querySelectorAll('[data-submit-locked="true"]').forEach(button => {
      button.disabled = false;
      button.classList.remove('is-loading');
      button.removeAttribute('aria-busy');
      delete button.dataset.submitLocked;
    });
    const overlay = document.getElementById('hpLoadingOverlay');
    if (overlay) {
      overlay.hidden = true;
      overlay.setAttribute('aria-hidden', 'true');
    }
  });

  document.querySelectorAll('[data-auto-submit]').forEach(control => {
    control.addEventListener('change', () => control.form?.requestSubmit());
  });

  const validateImage = (input, file) => {
    if (!file) return true;
    const allowed = new Set(['image/jpeg', 'image/png', 'image/webp']);
    if (!allowed.has(file.type)) {
      input.setCustomValidity('Chỉ chấp nhận ảnh JPG, JPEG, PNG hoặc WebP.');
      markInvalid(input);
      return false;
    }
    if (file.size > 5 * 1024 * 1024) {
      input.setCustomValidity('Tệp ảnh không được vượt quá 5 MB.');
      markInvalid(input);
      return false;
    }
    input.setCustomValidity('');
    return true;
  };

  document.querySelectorAll('input[type="file"][data-image-preview]').forEach(input => {
    const preview = document.getElementById(input.dataset.imagePreview);
    if (!preview) return;
    const showPreview = file => {
      if (!validateImage(input, file)) return;
      const previous = preview.dataset.objectUrl;
      if (previous) URL.revokeObjectURL(previous);
      const objectUrl = URL.createObjectURL(file);
      preview.dataset.objectUrl = objectUrl;
      preview.src = objectUrl;
      preview.hidden = false;
    };
    input.addEventListener('change', () => {
      const file = input.files?.[0];
      if (file) showPreview(file);
    });
    const zone = input.closest('[data-drop-zone]');
    if (!zone) return;
    ['dragenter', 'dragover'].forEach(type => zone.addEventListener(type, event => {
      event.preventDefault();
      zone.classList.add('is-dragging');
    }));
    ['dragleave', 'drop'].forEach(type => zone.addEventListener(type, event => {
      event.preventDefault();
      zone.classList.remove('is-dragging');
    }));
    zone.addEventListener('drop', event => {
      const file = event.dataTransfer?.files?.[0];
      if (!file || !validateImage(input, file)) return;
      const transfer = new DataTransfer();
      transfer.items.add(file);
      input.files = transfer.files;
      showPreview(file);
    });
  });

  document.querySelectorAll('[data-url-preview]').forEach(input => {
    const preview = document.getElementById(input.dataset.urlPreview);
    if (!preview) return;
    input.addEventListener('input', () => {
      const value = input.value.trim();
      if (!value) return;
      preview.src = value;
      preview.hidden = false;
    });
  });

  document.querySelectorAll('select[data-destination-preview]').forEach(select => {
    const preview = document.getElementById(select.dataset.destinationPreview);
    if (!preview) return;
    const update = () => {
      const option = select.selectedOptions?.[0];
      preview.src = option?.dataset.image || select.dataset.fallback || '/images/destination-fallback.svg';
    };
    select.addEventListener('change', update);
    update();
  });

  document.querySelectorAll('[data-currency-preview]').forEach(input => {
    const output = document.getElementById(input.dataset.currencyPreview);
    if (!output) return;
    const formatter = new Intl.NumberFormat('vi-VN', {style: 'currency', currency: 'VND', maximumFractionDigits: 2});
    const update = () => {
      const value = Number(input.value);
      output.textContent = Number.isFinite(value) && input.value !== '' ? formatter.format(value) : '';
    };
    input.addEventListener('input', update);
    update();
  });

  const dirtyForms = new Set();
  document.querySelectorAll('form[data-dirty-guard]').forEach(form => {
    form.addEventListener('input', () => dirtyForms.add(form));
    form.addEventListener('submit', event => {
      if (!event.defaultPrevented && form.checkValidity()) dirtyForms.delete(form);
    });
  });
  document.addEventListener('click', async event => {
    const link = event.target.closest('a[href]');
    if (!link || !dirtyForms.size || link.dataset.noDirtyGuard === 'true' || link.target || link.hasAttribute('download')) return;
    const target = new URL(link.href, window.location.href);
    if (target.origin !== window.location.origin || target.href === window.location.href || typeof HP.confirm !== 'function') return;
    event.preventDefault();
    const leave = await HP.confirm({title: 'Rời trang khi chưa lưu?', message: 'Các thay đổi bạn vừa nhập chưa được lưu.', acceptLabel: 'Rời trang', tone: 'warning'});
    if (!leave) return;
    dirtyForms.clear();
    window.location.assign(target.href);
  });
})();
