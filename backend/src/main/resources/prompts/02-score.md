# Prompt 02 — Score candidates against the fit rubric

Design intent: every candidate here already passed the objective filters, so this call is
only ever answering "how well does this person meet the standard", never "do they have the
skill". The hard requirement is that explanations are checkable. Each one must cite fields
that genuinely exist on that profile, because the citations are verified in code after this
call returns and anything unverifiable is stripped before the recruiter sees it. Generic
praise is not a style problem here, it is an unfalsifiable claim in a hiring decision.

One call scores the whole batch rather than one call per candidate: it is a fraction of the
latency the recruiter waits through, and it keeps a free-tier quota intact across the many
refinement rounds a real session involves.

---

You are scoring shortlisted candidates for a recruiter against a specific fit rubric.

## The rubric

{{rubric}}

## The candidates

{{profiles}}

## How to score

For each candidate, produce a weighted judgement against the rubric criteria above.

- `score` — 0 to 100. Use the range honestly. If everyone scores 80-90 the ranking tells
  the recruiter nothing; a genuinely borderline candidate should land in the 50s.
- `verdict` — `strong`, `possible` or `weak`, consistent with the score.
- `why` — at most 25 words. Name the specific thing about *this* person that drove the
  score. No adjectives that would fit any competent engineer.
- `evidence` — two to four citations. Each is a `field` and the `value` you are relying on.
  `field` must be one of: `skills`, `current_title`, `years_experience`, `location`,
  `current_company`, `current_company_type`, `past_companies`, `education`, `summary`.
  `value` must appear in that field on that candidate — copy it exactly, do not paraphrase
  it, and do not cite anything you inferred rather than read.
- `concerns` — zero to two honest reservations. A candidate with no reservations worth
  naming should have an empty list rather than an invented one.

Apply the dealbreakers strictly: a candidate who trips one cannot be `strong`.

Score every candidate you were given, using their exact `id`.

Return only JSON matching the provided schema.
