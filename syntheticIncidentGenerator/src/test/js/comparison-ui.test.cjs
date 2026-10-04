const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

class Element {
  constructor() {
    this.hidden = true;
    this.disabled = false;
    this.textContent = '';
    this.children = [];
    this.attributes = {};
    this.events = {};
    this.className = '';
    this.classes = new Set();
    this.classList = {
      toggle: (name, on) => (on ? this.classes.add(name) : this.classes.delete(name)),
      add: (name) => this.classes.add(name),
      remove: (name) => this.classes.delete(name),
    };
    this.style = {
      setProperty: (name, value) => {
        this.style[name] = value;
      },
    };
  }
  addEventListener(name, handler) {
    this.events[name] = handler;
  }
  append(...children) {
    this.children.push(...children);
  }
  replaceChildren(...children) {
    this.children = children;
  }
  setAttribute(name, value) {
    this.attributes[name] = String(value);
  }
  scrollIntoView() {}
}

const key = {
  incidentId: 'incident-a',
  terminalStatus: 'REJECTED',
  revealedBy: 'operator',
  revealedAt: '2026-10-01T12:00:00Z',
  oracleVersion: 'v1',
  answerKey: {
    rootCause: 'Gateway unreachable',
    expectedDisposition: 'PROPOSED',
    expectedConfidence: 'HIGH',
    recommendation: 'Escalate',
    decisionRule: 'Match report',
    requiredEvidence: ['GATEWAY_TIMEOUT'],
  },
};
const grade = {
  disposition: 100,
  confidence: 100,
  rootCause: 0,
  rootCauseReason: 'Wrong cause',
  recommendation: 0,
  recommendationReason: 'Wrong action',
  reportScore: 50,
  reportBand: 'OK',
  expectedDecision: 'REJECTED',
  actualDecision: 'REJECTED',
  decisionScore: 100,
  decisionBand: 'GOOD',
};
const comparison = {
  incidentId: 'incident-a',
  status: 'AVAILABLE',
  grade,
  report: {
    disposition: 'PROPOSED',
    confidence: { level: 'HIGH' },
    probableCause: { statement: '<script>wrong cause</script>' },
    recommendation: { statement: 'Wrong action' },
  },
  modelId: 'judge',
  promptVersion: 'v1',
  rubricVersion: 'v1',
  comparisonId: 'attempt',
  reportAttemptId: 'report',
  completedAt: '2026-10-01T12:00:00Z',
};
const generation = (id) => ({
  incidentId: id,
  queueStatus: 'NEW',
  alert: {
    externalAlertId: 'opaque',
    severity: 'HIGH',
    detectedAt: '2026-10-01T12:00:00Z',
    title: 'Alert',
    description: 'Synthetic',
  },
});
const response = (body, ok = true) => ({
  ok,
  status: ok ? 200 : 503,
  json: async () => body,
});
function setup(fetcher) {
  const elements = new Map();
  const document = {
    querySelector: (id) => {
      if (!elements.has(id)) elements.set(id, new Element());
      return elements.get(id);
    },
    createElement: () => new Element(),
  };
  const context = vm.createContext({ document, fetch: fetcher, Date, console });
  vm.runInContext(fs.readFileSync('src/main/resources/static/app.js', 'utf8'), context);
  vm.runInContext('render(' + JSON.stringify(generation('incident-a')) + ')', context);
  return {
    elements,
    context,
    click: async (id) => document.querySelector(id).events.click(),
    run: (code) => vm.runInContext(code, context),
  };
}
function findMeters(element) {
  return element.children.flatMap((child) => [
    ...(child.attributes.role === 'meter' ? [child] : []),
    ...findMeters(child),
  ]);
}
function allText(element) {
  return [element.textContent, ...element.children.map(allText)].join(' ');
}

test('confidence displays calibrated expectation and retains the original key', () => {
  const ui = setup(async () => response({}));
  ui.run('renderComparison(' + JSON.stringify({ ...comparison, grade: { ...grade,
    expectedConfidence: 'MEDIUM', originalExpectedConfidence: 'HIGH',
    confidenceRuleVersion: 'confidence-evidence/v1', confidenceReason: 'Bounded rate-limit evidence lacks traffic shape.'
  }, report: { ...comparison.report, confidence: { level: 'MEDIUM' } } }) + ', ' + JSON.stringify(key) + ')');
  const text = allText(ui.elements.get('#report-metrics'));
  assert.match(text, /Expected: MEDIUM · Actual: MEDIUM/);
  assert.match(text, /Original key: HIGH/);
  assert.match(text, /confidence-evidence\/v1/);
  assert.match(text, /lacks traffic shape/);
});

test('comparison follows successful reveal and shows separate independently colored cards', async () => {
  const calls = [];
  const ui = setup(async (url) => {
    calls.push(url);
    return response(url.endsWith('/answer-key') ? key : comparison);
  });
  await ui.click('#reveal-answer-key');
  assert.deepEqual(calls, [
    '/api/generations/incident-a/answer-key',
    '/api/generations/incident-a/comparison',
  ]);
  assert.equal(ui.elements.get('#answer-key').hidden, false);
  assert.match(ui.elements.get('#report-comparison').className, /comparison-ok/);
  assert.match(ui.elements.get('#decision-comparison').className, /comparison-good/);
  const meters = findMeters(ui.elements.get('#report-metrics'));
  assert.equal(meters.length, 4);
  assert.equal(meters[0].attributes['aria-valuenow'], '100');
  assert.equal(meters[2].attributes['aria-valuenow'], '0');
  assert.match(allText(ui.elements.get('#report-metrics')), /<script>wrong cause<\/script>/);
});

test('sealed answer key never requests comparison', async () => {
  let calls = 0;
  const ui = setup(async () => {
    calls++;
    return response({}, false);
  });
  await ui.click('#reveal-answer-key');
  assert.equal(calls, 1);
  assert.equal(ui.elements.get('#comparison-results').hidden, true);
});

test('comparison failure preserves answer key and displays unscored cards with retry', async () => {
  const ui = setup(async (url) =>
    response(
      url.endsWith('/answer-key')
        ? key
        : { status: 'UNAVAILABLE', statusDetail: 'Evaluator unavailable' },
    ),
  );
  await ui.click('#reveal-answer-key');
  assert.equal(ui.elements.get('#answer-key').hidden, false);
  assert.equal(ui.elements.get('#report-comparison-label').textContent, 'Not scored');
  assert.equal(ui.elements.get('#decision-comparison-label').textContent, 'Not scored');
  assert.equal(findMeters(ui.elements.get('#report-metrics')).length, 0);
  assert.equal(ui.elements.get('#retry-comparison').hidden, false);
});

test('retry evaluates without revealing again', async () => {
  const calls = [];
  const ui = setup(async (url) => {
    calls.push(url);
    return response(
      url.endsWith('/answer-key')
        ? key
        : calls.length === 2
          ? { status: 'UNAVAILABLE' }
          : comparison,
    );
  });
  await ui.click('#reveal-answer-key');
  await ui.click('#retry-comparison');
  assert.equal(calls.filter((url) => url.endsWith('/answer-key')).length, 1);
  assert.equal(ui.elements.get('#retry-comparison').hidden, true);
});

test('new incident clears comparisons and ignores an old in-flight result', async () => {
  let finish;
  const ui = setup(async (url) =>
    url.endsWith('/answer-key')
      ? response(key)
      : new Promise((resolve) => {
          finish = resolve;
        }),
  );
  const pending = ui.click('#reveal-answer-key');
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(ui.elements.get('#report-comparison-label').textContent, 'Scoring…');
  ui.run('render(' + JSON.stringify(generation('incident-b')) + ')');
  finish(response(comparison));
  await pending;
  assert.equal(ui.elements.get('#comparison-results').hidden, true);
  assert.equal(ui.elements.get('#answer-key').hidden, true);
});

test('HTTP evaluation failure is separate from sealed-key failure', async () => {
  const ui = setup(async (url) =>
    response(url.endsWith('/answer-key') ? key : {}, url.endsWith('/answer-key')),
  );
  await ui.click('#reveal-answer-key');
  assert.equal(ui.elements.get('#answer-key').hidden, false);
  assert.match(ui.elements.get('#comparison-status').textContent, /unavailable/i);
  assert.doesNotMatch(ui.elements.get('#reveal-status').textContent, /sealed/);
});

test('all score bands and endpoint diamonds render with accessible numeric labels', () => {
  const ui = setup(async () => response({}));
  for (const [score, band] of [
    [0, 'BAD'],
    [50, 'OK'],
    [100, 'GOOD'],
  ]) {
    ui.run(
      'renderComparison(' +
        JSON.stringify({
          ...comparison,
          grade: {
            ...grade,
            reportScore: score,
            reportBand: band,
            decisionScore: score,
            decisionBand: band,
          },
        }) +
        ', ' +
        JSON.stringify(key) +
        ')',
    );
    assert.match(
      ui.elements.get('#report-comparison').className,
      new RegExp('comparison-' + band.toLowerCase()),
    );
    assert.equal(
      findMeters(ui.elements.get('#decision-metrics'))[0].attributes['aria-valuenow'],
      String(score),
    );
  }
});
