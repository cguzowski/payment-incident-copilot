const button = document.querySelector('#generate-incident');
const status = document.querySelector('#generation-status');
const result = document.querySelector('#generation-result');
const answerKey = document.querySelector('#answer-key');
const revealButton = document.querySelector('#reveal-answer-key');
const revealStatus = document.querySelector('#reveal-status');
const comparisonResults = document.querySelector('#comparison-results');
const comparisonStatus = document.querySelector('#comparison-status');
const retryComparison = document.querySelector('#retry-comparison');
const operatorId = '7b636625-53d1-46f7-92a9-9c8c27a243d1';
let currentIncidentId;
let currentReveal;
let viewVersion = 0;
let comparisonVersion = 0;

const setText = (selector, value) => {
  document.querySelector(selector).textContent = value ?? '—';
};

const showStatus = (message, isError = false) => {
  status.textContent = message;
  status.classList.toggle('error', isError);
};

const render = (generation) => {
  viewVersion++;
  comparisonVersion++;
  currentReveal = undefined;
  comparisonResults.hidden = true;
  retryComparison.hidden = true;
  document.querySelector('#report-metrics').replaceChildren();
  document.querySelector('#decision-metrics').replaceChildren();
  currentIncidentId = generation.incidentId;
  setText('#incident-id', generation.incidentId);
  setText('#alert-id', generation.alert.externalAlertId);
  setText('#severity', generation.alert.severity);
  setText('#detected-at', new Date(generation.alert.detectedAt).toISOString());
  setText('#queue-status', generation.queueStatus);
  setText('#alert-title', generation.alert.title);
  setText('#alert-description', generation.alert.description);
  answerKey.hidden = true;
  revealStatus.textContent = '';
  revealButton.disabled = false;
  result.hidden = false;
  result.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const addMetric = (container, label, score, expected, actual, reason) => {
  const row = document.createElement('section');
  row.className = 'comparison-metric';
  const heading = document.createElement('h4');
  heading.textContent = `${label} · ${score}/100`;
  const values = document.createElement('p');
  values.className = 'metric-values';
  values.textContent = `Expected: ${expected ?? 'No assertion'} · Actual: ${actual ?? 'No assertion'}`;
  const gauge = document.createElement('div');
  gauge.className = 'match-gauge';
  gauge.setAttribute('role', 'meter');
  gauge.setAttribute('aria-label', label);
  gauge.setAttribute('aria-valuemin', 0);
  gauge.setAttribute('aria-valuemax', 100);
  gauge.setAttribute('aria-valuenow', score);
  gauge.setAttribute('aria-valuetext', `${score} out of 100`);
  const track = document.createElement('div');
  track.className = 'gauge-track';
  const diamond = document.createElement('span');
  diamond.className = 'gauge-diamond';
  diamond.style.setProperty('--match', `${score}%`);
  diamond.setAttribute('aria-hidden', 'true');
  track.append(diamond);
  const scale = document.createElement('div');
  scale.className = 'gauge-scale';
  const minimum = document.createElement('span');
  minimum.textContent = '0';
  const maximum = document.createElement('span');
  maximum.textContent = '100';
  scale.append(minimum, maximum);
  gauge.append(track, scale);
  const explanation = document.createElement('p');
  explanation.className = 'metric-reason';
  explanation.textContent = reason;
  row.append(heading, values, gauge, explanation);
  container.append(row);
};

const setComparisonCard = (id, band, score) => {
  const names = { GOOD: 'Good', OK: 'OK', BAD: 'Bad' };
  document.querySelector(`#${id}-comparison`).className =
    `comparison-card comparison-${band?.toLowerCase() ?? 'unscored'}`;
  setText(
    `#${id}-comparison-label`,
    score == null ? 'Not scored' : `${names[band]} · ${score}/100`,
  );
};

const unscoredComparison = (message, loading = false) => {
  comparisonResults.hidden = false;
  comparisonStatus.textContent = message;
  document.querySelector('#report-metrics').replaceChildren();
  document.querySelector('#decision-metrics').replaceChildren();
  setComparisonCard('report');
  setComparisonCard('decision');
  if (loading) {
    setText('#report-comparison-label', 'Scoring…');
    setText('#decision-comparison-label', 'Scoring…');
  }
  setText('#comparison-provenance', '');
  retryComparison.hidden = loading;
};

const renderComparison = (comparison, reveal) => {
  if (comparison.status !== 'AVAILABLE' || !comparison.grade) {
    unscoredComparison(comparison.statusDetail ?? 'Comparison unavailable. No score was assigned.');
    return;
  }
  comparisonResults.hidden = false;
  retryComparison.hidden = true;
  comparisonStatus.textContent = 'Comparison complete. Scores are advisory.';
  const grade = comparison.grade;
  const key = reveal.answerKey;
  const report = comparison.report;
  setComparisonCard('report', grade.reportBand, grade.reportScore);
  setComparisonCard('decision', grade.decisionBand, grade.decisionScore);
  const metrics = document.querySelector('#report-metrics');
  metrics.replaceChildren();
  addMetric(
    metrics,
    'Disposition match',
    grade.disposition,
    key.expectedDisposition,
    report.disposition,
    'Exact match: 0 or 100.',
  );
  addMetric(
    metrics,
    'Confidence match',
    grade.confidence,
    grade.expectedConfidence ?? key.expectedConfidence,
    report.confidence.level,
    grade.confidenceReason
      ? `Exact LOW / MEDIUM / HIGH match: 0 or 100. Original key: ${grade.originalExpectedConfidence}. ${grade.confidenceRuleVersion}: ${grade.confidenceReason}`
      : 'Exact LOW / MEDIUM / HIGH match: 0 or 100.',
  );
  const insufficient = key.expectedDisposition === 'INSUFFICIENT_EVIDENCE';
  addMetric(
    metrics,
    insufficient ? 'Cause withheld correctly' : 'Root cause match · AI-assessed',
    grade.rootCause,
    insufficient ? 'No assertion' : key.rootCause,
    report.probableCause?.statement,
    grade.rootCauseReason,
  );
  addMetric(
    metrics,
    insufficient ? 'Recommendation withheld correctly' : 'Recommendation match · AI-assessed',
    grade.recommendation,
    insufficient ? 'No assertion' : key.recommendation,
    report.recommendation?.statement,
    grade.recommendationReason,
  );
  const decisionMetrics = document.querySelector('#decision-metrics');
  decisionMetrics.replaceChildren();
  addMetric(
    decisionMetrics,
    'Final decision match',
    grade.decisionScore,
    grade.expectedDecision,
    grade.actualDecision,
    grade.decisionScore === 100
      ? 'The recorded human decision matches the comparison rubric.'
      : 'The recorded human decision does not match the comparison rubric.',
  );
  setText(
    '#comparison-provenance',
    `Evaluator: ${comparison.modelId} · ${comparison.promptVersion} · ${comparison.rubricVersion} · Comparison ${comparison.comparisonId} · Report ${comparison.reportAttemptId}`,
  );
};

const compareCurrentInvestigation = async (reveal, version) => {
  const attempt = ++comparisonVersion;
  retryComparison.disabled = true;
  unscoredComparison(
    'Comparing the frozen report with the answer key… This may take a few minutes.',
    true,
  );
  try {
    const response = await fetch(`/api/generations/${reveal.incidentId}/comparison`, {
      method: 'POST',
      headers: { 'X-Synthetic-Operator-Id': operatorId },
    });
    if (!response.ok) throw new Error('Comparison unavailable');
    const comparison = await response.json();
    if (version !== viewVersion || attempt !== comparisonVersion) return;
    if (comparison.incidentId && comparison.incidentId !== currentIncidentId)
      throw new Error('Comparison incident mismatch');
    renderComparison(comparison, reveal);
  } catch (error) {
    if (version !== viewVersion || attempt !== comparisonVersion) return;
    unscoredComparison(
      'Comparison unavailable. The answer key remains revealed; no score was assigned.',
    );
  } finally {
    if (version === viewVersion && attempt === comparisonVersion) retryComparison.disabled = false;
  }
};

retryComparison.addEventListener('click', async () => {
  if (currentReveal) await compareCurrentInvestigation(currentReveal, viewVersion);
});

const renderAnswerKey = (reveal) => {
  const oracle = reveal.answerKey;
  setText('#terminal-status', reveal.terminalStatus);
  setText('#revealed-by', reveal.revealedBy);
  setText('#revealed-at', new Date(reveal.revealedAt).toISOString());
  setText('#oracle-version', reveal.oracleVersion);
  setText('#root-cause', oracle.rootCause);
  setText('#expected-disposition', oracle.expectedDisposition);
  setText('#expected-confidence', oracle.expectedConfidence);
  setText('#recommendation', oracle.recommendation);
  setText('#decision-rule', oracle.decisionRule);

  const evidenceList = document.querySelector('#required-evidence');
  evidenceList.replaceChildren();
  oracle.requiredEvidence.forEach((evidence) => {
    const item = document.createElement('li');
    item.textContent = evidence;
    evidenceList.append(item);
  });

  answerKey.hidden = false;
  answerKey.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

button.addEventListener('click', async () => {
  viewVersion++;
  comparisonVersion++;
  currentReveal = undefined;
  comparisonResults.hidden = true;
  button.disabled = true;
  result.hidden = true;
  showStatus('Selecting a scenario and submitting its sparse alert…');
  try {
    const response = await fetch('/api/generations', { method: 'POST' });
    if (!response.ok) {
      throw new Error(`Alert intake returned HTTP ${response.status}.`);
    }
    const generation = await response.json();
    render(generation);
    showStatus('Synthetic incident accepted. Continue in the operator console.');
  } catch (error) {
    showStatus('No incident was created. Start or check the copilot API, then try again.', true);
  } finally {
    button.disabled = false;
  }
});

revealButton.addEventListener('click', async () => {
  const version = viewVersion;
  const incidentId = currentIncidentId;
  revealButton.disabled = true;
  revealStatus.textContent = 'Confirming the final incident decision…';
  try {
    const response = await fetch(`/api/generations/${incidentId}/answer-key`, {
      method: 'POST',
      headers: { 'X-Synthetic-Operator-Id': operatorId },
    });
    if (!response.ok) {
      throw new Error(`Answer-key reveal returned HTTP ${response.status}.`);
    }
    const reveal = await response.json();
    if (version !== viewVersion) return;
    currentReveal = reveal;
    renderAnswerKey(reveal);
    revealStatus.textContent = 'Answer key revealed and audit metadata recorded.';
    await compareCurrentInvestigation(reveal, version);
  } catch (error) {
    if (version !== viewVersion) return;
    revealStatus.textContent =
      'Answer key remains sealed. Record an approved or rejected decision in the operator console, then retry.';
    revealButton.disabled = false;
  }
});
