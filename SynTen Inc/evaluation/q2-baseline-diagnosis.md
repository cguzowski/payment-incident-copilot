# Q2 retained baseline diagnosis

Run: [`5847a80f655349ec8c3f9be986e52663`](results/5847a80f655349ec8c3f9be986e52663-FAIL.json); artifact SHA-256: `cd072e1b9407f5713813292f6c2a1bb0797c2bc80d94e5c8d53b9eff8830ad7a`.

All 37 variants ran through unchanged product retrieval. Catalog backfill validation reported COMPLETE_SAME_MODEL, 705/705 vectors, noOp=true; fingerprint `5d704fee24f9754176f1be2e449050190e0b78ffa3fdb9ddc92b464006d198e9`. Runtime: Java 21.0.11, PostgreSQL 18.3, local Ollama nomic-embed-text (768 dimensions); model digest `0a109f422b47e3a30ba2b10eca18548e944e8a23073ee3f3e947efcf3c45e59f`. No corpus bytes changed.

Aggregate: 19/22 primary, 12/20 required policies, 17/21 ordering (19 required); zero ineligible candidates and all special semantics passed.

The table includes every variant with a failed assertion. L/V are best type-local ranks before the 20-candidate limits; vector ranks apply only at cosine >=0.55. F is best retained fused position, absent means lost before fusion. S is best selected position. Exact queries, evidence codes, filters and chunk locators are retained in the linked JSON artifact. Pre-limit ranks were inspected read-only using those queries and fresh embeddings from the same model; vectors were not saved.

| Case/variant | Primary L/V/F/S | Policy L/V/F/S | Weak F | Failed assertions |
|---|---|---|---|---|
| KQ-001/S001 | RB-002 1/2/1/1 | PL-006 33/24/absent/absent |  | supportingPolicySelected |
| KQ-001/S002 | RB-002 57/42/absent/absent | PL-006 59/22/absent/absent |  | primaryRunbookSelected, supportingPolicySelected, primaryOutranksWeakMatch |
| KQ-002/S003 | RB-003 13/23/43/absent | PL-006 61/28/absent/absent |  | primaryRunbookSelected, supportingPolicySelected |
| KQ-002/S009 | RB-003 1/1/3/3 | PL-006 34/12/44/absent | 54 | supportingPolicySelected |
| KQ-005/S006 | RB-006 1/34/14/6 | PL-003 19/11/10/5 | 1 | primaryOutranksWeakMatch |
| KQ-005/S210 | RB-006 1/7/1/1 | PL-003 33/21/absent/absent |  | supportingPolicySelected |
| KQ-006/S007 | RB-007 1/1/1/1 | PL-003 28/21/absent/absent |  | supportingPolicySelected |
| KQ-009/S011 | RB-010 1/1/3/2 | PL-003 27/9/38/absent |  | supportingPolicySelected |
| KQ-010/S012 | RB-011 1/1/1/1 | PL-006 33/8/29/absent | 51 | supportingPolicySelected |
| KQ-010/S202 | RB-011 1/4/1/1 | PL-006 14/22/51/absent | 2 | supportingPolicySelected |
| KQ-013/S101 | RB-014 1/1/1/1 | PL-006 36/15/45/absent | 49 | supportingPolicySelected |
| KQ-013/S203 | RB-014 1/13/2/2 | PL-006 35/19/62/absent | 21 | supportingPolicySelected |
| KQ-014/S102 | RB-011 1/2/1/1 | PL-006 54/29/absent/absent |  | supportingPolicySelected |
| KQ-014/S208 | RB-011 1/1/1/1 | PL-006 10/9/12/absent | 16 | supportingPolicySelected |
| KQ-017/S110 | RB-017 1/9/10/5 | PL-005 1/3/2/2 | 4 | primaryOutranksWeakMatch |
| KQ-018/S107 | RB-018 1/9/4/3 | PL-003 27/15/51/absent |  | supportingPolicySelected |
| KQ-018/S201 | RB-018 39/24/absent/absent | PL-003 29/27/absent/absent | 14 | primaryRunbookSelected, supportingPolicySelected, primaryOutranksWeakMatch |
| KQ-021/S205 | RB-021 1/1/1/1 | PL-003 33/27/absent/absent |  | supportingPolicySelected |

Policy slots repeatedly contain generic PL-005, PL-001 and PL-002. Test exact observed-code coverage for runbook recall/ordering, then source-derived document relationships for policy selection. These are separate hypotheses; relationship expansion must not relax eligibility or overwrite direct-match provenance.
