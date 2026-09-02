package com.cguzowski.paymentcopilot.knowledge.retrieval;

record QueryEmbeddingOutcome(QueryEmbeddingStatus status, String modelId, int dimensions, boolean normalized) {}
