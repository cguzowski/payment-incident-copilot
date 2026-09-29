const button = document.querySelector('#generate-incident');
const status = document.querySelector('#generation-status');
const result = document.querySelector('#generation-result');
const answerKey = document.querySelector('#answer-key');
const revealButton = document.querySelector('#reveal-answer-key');
const revealStatus = document.querySelector('#reveal-status');
const operatorId = '7b636625-53d1-46f7-92a9-9c8c27a243d1';
let currentIncidentId;

const setText = (selector, value) => {
  document.querySelector(selector).textContent = value ?? '—';
};

const showStatus = (message, isError = false) => {
  status.textContent = message;
  status.classList.toggle('error', isError);
};

const render = (generation) => {
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
  revealButton.disabled = true;
  revealStatus.textContent = 'Confirming the final incident decision…';
  try {
    const response = await fetch(`/api/generations/${currentIncidentId}/answer-key`, {
      method: 'POST',
      headers: { 'X-Synthetic-Operator-Id': operatorId },
    });
    if (!response.ok) {
      throw new Error(`Answer-key reveal returned HTTP ${response.status}.`);
    }
    renderAnswerKey(await response.json());
    revealStatus.textContent = 'Answer key revealed and audit metadata recorded.';
  } catch (error) {
    revealStatus.textContent =
      'Answer key remains sealed. Record an approved or rejected decision in the operator console, then retry.';
    revealButton.disabled = false;
  }
});
