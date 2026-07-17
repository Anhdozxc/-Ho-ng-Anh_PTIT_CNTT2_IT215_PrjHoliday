import {mkdir, writeFile} from 'node:fs/promises';
import path from 'node:path';

const baseUrl = process.env.HP_BASE_URL || 'http://127.0.0.1:8080';
const debugUrl = process.env.HP_CDP_URL || 'http://127.0.0.1:9222';
const outputDir = path.resolve(process.env.HP_SCREENSHOT_DIR || 'docs/screenshots');
const credentials = {
  user: [process.env.HP_USER_EMAIL, process.env.HP_USER_PASSWORD],
  admin: [process.env.HP_ADMIN_EMAIL, process.env.HP_ADMIN_PASSWORD]
};

for (const [role, [email, password]] of Object.entries(credentials)) {
  if (!email || !password) throw new Error(`Missing browser-smoke credentials for ${role}`);
}

const delay = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));
await mkdir(outputDir, {recursive: true});

const target = await fetch(`${debugUrl}/json/new?${encodeURIComponent(`${baseUrl}/login`)}`, {method: 'PUT'});
if (!target.ok) throw new Error(`Cannot create Chrome target: HTTP ${target.status}`);
const {webSocketDebuggerUrl} = await target.json();

class CdpClient {
  constructor(url) {
    this.socket = new WebSocket(url);
    this.sequence = 0;
    this.pending = new Map();
    this.listeners = new Map();
  }

  async open() {
    await new Promise((resolve, reject) => {
      this.socket.addEventListener('open', resolve, {once: true});
      this.socket.addEventListener('error', reject, {once: true});
    });
    this.socket.addEventListener('message', event => {
      const message = JSON.parse(event.data);
      if (message.id) {
        const pending = this.pending.get(message.id);
        if (!pending) return;
        this.pending.delete(message.id);
        if (message.error) pending.reject(new Error(message.error.message));
        else pending.resolve(message.result);
        return;
      }
      for (const listener of this.listeners.get(message.method) || []) listener(message.params || {});
    });
  }

  send(method, params = {}) {
    const id = ++this.sequence;
    return new Promise((resolve, reject) => {
      this.pending.set(id, {resolve, reject});
      this.socket.send(JSON.stringify({id, method, params}));
    });
  }

  on(method, listener) {
    const listeners = this.listeners.get(method) || [];
    listeners.push(listener);
    this.listeners.set(method, listeners);
  }

  close() {
    this.socket.close();
  }
}

const client = new CdpClient(webSocketDebuggerUrl);
await client.open();
await Promise.all([
  client.send('Page.enable'),
  client.send('Runtime.enable'),
  client.send('Network.enable'),
  client.send('Log.enable')
]);
await client.send('Network.clearBrowserCookies');

const consoleErrors = [];
const failedResources = [];
client.on('Runtime.exceptionThrown', event => {
  consoleErrors.push(event.exceptionDetails?.text || 'Uncaught browser exception');
});
client.on('Log.entryAdded', ({entry}) => {
  if (entry?.level !== 'error') return;
  const expectedNotFoundDocument = entry.source === 'network'
    && entry.url?.includes('/definitely-not-a-real-page')
    && entry.text?.includes('404');
  if (!expectedNotFoundDocument) consoleErrors.push(entry.text);
});
client.on('Network.loadingFailed', event => {
  if (!event.canceled) failedResources.push(`${event.type}: ${event.errorText}`);
});
client.on('Network.responseReceived', ({response, type}) => {
  if (response.status >= 400 && type !== 'Document') {
    failedResources.push(`${response.status} ${response.url}`);
  }
});

const evaluate = async expression => {
  const result = await client.send('Runtime.evaluate', {
    expression,
    awaitPromise: true,
    returnByValue: true
  });
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text);
  return result.result?.value;
};

const navigate = async pathname => {
  await client.send('Page.navigate', {url: new URL(pathname, baseUrl).href});
  for (let attempt = 0; attempt < 50; attempt += 1) {
    await delay(100);
    const ready = await evaluate("document.readyState === 'complete'");
    if (ready) break;
  }
  await delay(350);
  return evaluate('location.pathname + location.search');
};

const setViewport = (width, height, mobile = false) => client.send('Emulation.setDeviceMetricsOverride', {
  width,
  height,
  deviceScaleFactor: 1,
  mobile,
  screenWidth: width,
  screenHeight: height
});

const checks = [];
const record = (name, passed, detail = '') => checks.push({name, passed: Boolean(passed), detail});

const capture = async (name, width, height, mobile = false) => {
  await setViewport(width, height, mobile);
  await delay(900);
  const layout = await client.send('Page.getLayoutMetrics');
  const content = layout.cssContentSize || layout.contentSize;
  const screenshot = await client.send('Page.captureScreenshot', {
    format: 'png',
    captureBeyondViewport: true,
    fromSurface: true,
    clip: {x: 0, y: 0, width: Math.max(width, content.width), height: Math.min(content.height, 12000), scale: 1}
  });
  await writeFile(path.join(outputDir, `${name}.png`), Buffer.from(screenshot.data, 'base64'));
  const metrics = await evaluate(`({
    path: location.pathname,
    title: document.title,
    overflow: document.documentElement.scrollWidth > window.innerWidth,
    width: window.innerWidth,
    headings: document.querySelectorAll('h1').length
  })`);
  record(`${name}: no horizontal overflow`, !metrics.overflow, `${metrics.path} @ ${metrics.width}px`);
  record(`${name}: page has title and h1`, Boolean(metrics.title) && metrics.headings > 0, metrics.title);
};

const checkViewport = async (name, width, height) => {
  await setViewport(width, height);
  await delay(500);
  const metrics = await evaluate(`({
    path: location.pathname,
    overflow: document.documentElement.scrollWidth > window.innerWidth,
    width: window.innerWidth
  })`);
  record(`${name}: no horizontal overflow`, !metrics.overflow, `${metrics.path} @ ${metrics.width}px`);
};

const login = async ([email, password]) => {
  await navigate('/login');
  await evaluate(`(() => {
    const form = document.querySelector('form[action$="/login"]');
    form.querySelector('[name="username"]').value = ${JSON.stringify(email)};
    form.querySelector('[name="password"]').value = ${JSON.stringify(password)};
    form.submit();
  })()`);
  for (let attempt = 0; attempt < 60; attempt += 1) {
    await delay(150);
    if (await evaluate("location.pathname === '/dashboard'")) return true;
  }
  return false;
};

await navigate('/login');
record('login form invalid without credentials', !(await evaluate("document.querySelector('form').checkValidity()")));
await capture('login-desktop', 1440, 900);
await capture('login-mobile', 375, 812, true);

record('USER form login redirects to dashboard', await login(credentials.user));
await capture('dashboard-desktop', 1440, 900);
await capture('dashboard-mobile', 375, 812, true);
for (const [name, width, height] of [
  ['dashboard-tablet', 768, 1024],
  ['dashboard-laptop', 1024, 768],
  ['dashboard-wide', 1920, 1080]
]) {
  await checkViewport(name, width, height);
}
await navigate('/trips');
await capture('trip-list', 1440, 900);

await setViewport(375, 812, true);
await evaluate("document.querySelector('.navbar-toggler')?.click()");
await delay(400);
record('mobile navbar opens', await evaluate("document.querySelector('#hpNav')?.classList.contains('show')"));

const tripPath = await evaluate(`(() => {
  const paths = [...document.querySelectorAll('a[href]')]
    .map(link => new URL(link.href).pathname)
    .filter(path => /^\\/trips\\/\\d+$/.test(path));
  return paths[0] || null;
})()`);
record('trip list exposes a detail link', Boolean(tripPath), tripPath || 'none');
if (tripPath) {
  await navigate(tripPath);
  await capture('trip-detail-overview', 1440, 900);
  for (const tab of ['itinerary', 'expenses', 'checklist', 'bookings']) {
    await evaluate(`document.querySelector('[data-bs-target="#${tab}"]')?.click()`);
    await delay(300);
    record(`trip tab ${tab} activates`, await evaluate(`document.querySelector('#${tab}')?.classList.contains('active')`));
    await capture(`trip-detail-${tab}`, 1440, 900);
  }
  const hasConfirmForm = await evaluate("Boolean(document.querySelector('form[data-confirm]'))");
  if (hasConfirmForm) {
    await evaluate("document.querySelector('form[data-confirm]').requestSubmit()");
    await delay(250);
    record('confirmation modal opens', await evaluate("document.querySelector('#hpConfirmDialog')?.classList.contains('is-open')"));
    await evaluate("document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', code: 'Escape', bubbles: true}))");
    await delay(250);
    record('confirmation modal closes with Escape', !(await evaluate("document.querySelector('#hpConfirmDialog')?.classList.contains('is-open')")));
  }
}

await navigate('/profile');
await capture('profile', 1440, 900);
await navigate('/definitely-not-a-real-page');
record('friendly 404 renders', (await evaluate("document.body.innerText.includes('404')")));
await capture('error-404', 1440, 900);

await client.send('Network.clearBrowserCookies');
record('ADMIN form login redirects to dashboard', await login(credentials.admin));
await navigate('/admin/destinations');
await capture('admin-destinations', 1440, 900);
await navigate('/admin/users');
await capture('admin-users', 1440, 900);

record('no uncaught JavaScript errors', consoleErrors.length === 0, consoleErrors.join(' | '));
record('no failed non-document resources', failedResources.length === 0, failedResources.join(' | '));

const summary = {
  generatedAt: new Date().toISOString(),
  baseUrl,
  passed: checks.filter(check => check.passed).length,
  failed: checks.filter(check => !check.passed).length,
  checks,
  consoleErrors,
  failedResources
};
await writeFile(path.join(outputDir, 'browser-smoke.json'), `${JSON.stringify(summary, null, 2)}\n`);
client.close();
console.log(JSON.stringify(summary, null, 2));
if (summary.failed) process.exitCode = 1;
