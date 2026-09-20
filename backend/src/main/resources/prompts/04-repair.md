# Prompt 04 — Repair a malformed response

Design intent: a structured-output hint constrains generation, it does not guarantee it.
When a response fails to parse or fails a domain rule, one focused repair attempt — handed
the schema, the exact failure and the original text — recovers the common cases (a trailing
comma, a truncated array, a weight set that does not sum) for the cost of one call.

It is one attempt, not a loop. A model that cannot fix its own output given the error is
unlikely to succeed on a third try, and the recruiter is waiting.

---

Your previous response could not be used. Return a corrected version.

## The schema it must satisfy

{{schema}}

## What went wrong

{{error}}

## Your previous response

{{raw}}

Fix only what the error describes. Keep every value that was already valid — do not
re-answer the original question, do not invent new content, and do not drop fields that
were correct. If the previous response was truncated, complete it.

Return only JSON matching the schema. No explanation, no Markdown fence.
