(() => {
  'use strict';

  const HP = window.HolidayPlanner || {};
  const reduced = HP.reducedMotion ?? false;
  const revealItems = [...document.querySelectorAll('[data-reveal]')];

  document.querySelectorAll('[data-stagger]').forEach(group => {
    [...group.children].forEach((child, index) => {
      child.style.transitionDelay = `${index * 70}ms`;
      revealItems.push(child);
    });
  });

  const uniqueRevealItems = [...new Set(revealItems)];
  uniqueRevealItems.forEach(item => item.classList.add('hp-reveal-pending'));

  if (reduced || !('IntersectionObserver' in window)) {
    uniqueRevealItems.forEach(item => item.classList.add('is-revealed'));
  } else {
    const revealObserver = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return;
        entry.target.classList.add('is-revealed');
        revealObserver.unobserve(entry.target);
      });
    }, {rootMargin: '0px 0px -8% 0px', threshold: .08});
    uniqueRevealItems.forEach(item => revealObserver.observe(item));
  }

  const counters = [...document.querySelectorAll('[data-counter]')];
  const animateCounter = counter => {
    if (counter.dataset.counterReady === 'true') return;
    counter.dataset.counterReady = 'true';
    const target = Number(counter.dataset.counter);
    if (!Number.isFinite(target) || reduced) return;
    const formatter = new Intl.NumberFormat('vi-VN');
    const start = performance.now();
    const duration = 760;
    const tick = now => {
      const progress = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      counter.textContent = formatter.format(Math.round(target * eased));
      if (progress < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
  };

  if (reduced || !('IntersectionObserver' in window)) {
    counters.forEach(animateCounter);
  } else {
    const counterObserver = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return;
        animateCounter(entry.target);
        counterObserver.unobserve(entry.target);
      });
    }, {threshold: .45});
    counters.forEach(counter => counterObserver.observe(counter));
  }

  const navbar = document.querySelector('.hp-navbar');
  const hero = document.querySelector('[data-parallax]');
  let frame = null;
  const updateScrollEffects = () => {
    frame = null;
    navbar?.classList.toggle('is-scrolled', window.scrollY > 16);
    if (hero && !reduced) {
      const offset = Math.min(window.scrollY * .08, 28);
      hero.style.setProperty('--hp-parallax', `${offset}px`);
    }
  };
  const requestUpdate = () => {
    if (frame === null) frame = requestAnimationFrame(updateScrollEffects);
  };
  window.addEventListener('scroll', requestUpdate, {passive: true});
  updateScrollEffects();
})();
