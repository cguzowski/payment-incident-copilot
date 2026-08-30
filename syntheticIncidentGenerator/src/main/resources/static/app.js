const button = document.querySelector('#generate-incident');
const status = document.querySelector('#generation-status');
const result = document.querySelector('#generation-result');
const answerKey = document.querySelector('#answer-key');

const setText = (selector, value) => {
  document.querySelector(selector).textContent = value ?? '—';
};

const showStatus = (message, isError = false) => {
  status.textContent = message;
  status.classList.toggle('error', isError);
};

const render = (generation) => {
  setText('#incident-id', generation.incidentId);
  setText('#alert-id', generation.alert.externalAlertId);
  setText('#severity', generation.alert.severity);
  setText('#detected-at', new Date(generation.alert.detectedAt).toISOString());
  setText('#rarity', generation.rarity);
  setText('#scenario-code', generation.scenarioCode);
  setText('#queue-status', generation.queueStatus);
  setText('#alert-title', generation.alert.title);
  setText('#alert-description', generation.alert.description);
  setText('#root-cause', generation.answerKey.rootCause);
  setText('#expected-disposition', generation.answerKey.expectedDisposition);
  setText('#expected-confidence', generation.answerKey.expectedConfidence);
  setText('#recommendation', generation.answerKey.recommendation);
  setText('#decision-rule', generation.answerKey.decisionRule);

  const evidenceList = document.querySelector('#required-evidence');
  evidenceList.replaceChildren();
  generation.answerKey.requiredEvidence.forEach((evidence) => {
    const item = document.createElement('li');
    item.textContent = evidence;
    evidenceList.append(item);
  });

  answerKey.open = false;
  result.hidden = false;
  result.scrollIntoView({ behavior: 'smooth', block: 'start' });
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
