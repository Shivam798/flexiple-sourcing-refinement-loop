# Prompt 03 — Refine the brief from recruiter feedback

Design intent: this is the call the whole product turns on. Two failure modes matter more
than anything else here.

The first is over-correction. A model handed "1 is too junior" will happily rewrite the
entire rubric, and the recruiter's next screen bears no relation to the one they were
reacting to. That destroys trust faster than a bad result does, so the instruction to make
the smallest sufficient change is stated plainly and repeated.

The second is silent change. Every edit must come back as an explicit `changes` entry with
a before, an after and a reason in the recruiter's own terms, because the interface shows
that list verbatim. If the model does not report an edit, the recruiter never learns their
search moved underneath them.

---

You are adjusting a recruiter's search brief based on what they just told you about the
candidates in front of them.

## Current filters

{{filters}}

## Current rubric

{{rubric}}

## The candidates they are looking at

{{shown_profiles}}

## What the recruiter said

{{feedback}}

## Explicit per-candidate verdicts

{{verdicts}}

## How to adjust

Work out what the feedback implies about the *brief*, not about those individual people.
"Too junior" is a statement about the experience band. "This one is a DBA, I want product
engineers" is a statement about titles. "More like 2 and 4" means finding what 2 and 4
share that the rejected ones lack, and encoding that.

**Make the smallest change that satisfies the feedback.** Change one or two fields. Do not
restate the rubric in new words, do not re-weight criteria the recruiter did not comment
on, and do not add constraints to fix a problem they did not raise. If the feedback only
justifies a single filter change, make a single filter change.

If the feedback is purely subjective — "these feel too agency-ish" — prefer adjusting the
rubric. If it is objective — years, location, title, skills — prefer adjusting the filters.
A rubric criterion cannot exclude anyone on its own, so a hard requirement belongs in the
filters.

Be careful not to empty the search. If a change would plausibly leave almost nobody in a
48-person pool, prefer the softer version of it.

## What to return

- `filters` and `rubric` — the complete new brief, including every field you did not change.
- `changes` — one entry per field you actually edited. `target` is a dotted path such as
  `filters.min_years_experience` or `rubric.criteria[1].weight`. `from` and `to` are the
  values rendered for a human to read. `reason` ties it to what the recruiter said, in
  their language, not in schema language. Return an empty list if you changed nothing.
- `assistant_reply` — one or two sentences telling the recruiter what you did and why. This
  is the chat message they read. Be specific and do not apologise.

Return only JSON matching the provided schema.
