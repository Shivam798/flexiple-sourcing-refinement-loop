"use client";

import { useState } from "react";
import type { Change, Filters, Rubric } from "@/lib/types";
import { FilterEditor } from "./FilterEditor";
import { Wordmark } from "./Wordmark";
import { RubricEditor } from "./RubricEditor";

interface Props {
  filters: Filters;
  rubric: Rubric;
  changes: Change[];
  frozen: boolean;
  busy: boolean;
  onApply: (filters: Filters, rubric: Rubric) => void;
}

/**
 * The brief, always on screen.
 *
 * <p>Edits are collected into a draft and applied on a single action rather than saved per
 * keystroke. Every apply re-runs the search, and re-running it on each character typed into
 * a skill name would be both wasteful and disorienting.
 */
export function BriefPane({ filters, rubric, changes, frozen, busy, onApply }: Props) {
  const [draftFilters, setDraftFilters] = useState(filters);
  const [draftRubric, setDraftRubric] = useState(rubric);
  const [serverBrief, setServerBrief] = useState({ filters, rubric });

  // A refinement replaces the brief from the server, and the local draft follows it.
  // Adjusted during render rather than in an effect: React re-renders immediately with the
  // new draft instead of painting the stale one first.
  if (serverBrief.filters !== filters || serverBrief.rubric !== rubric) {
    setServerBrief({ filters, rubric });
    setDraftFilters(filters);
    setDraftRubric(rubric);
  }

  const dirty =
    JSON.stringify(draftFilters) !== JSON.stringify(filters) ||
    JSON.stringify(draftRubric) !== JSON.stringify(rubric);

  return (
    <aside className="flex h-dvh flex-col border-r border-hairline bg-surface">
      <div className="border-b border-hairline px-5 py-4">
        <Wordmark />
      </div>
      <div className="flex-1 space-y-7 overflow-y-auto overscroll-contain px-5 py-6">
        <FilterEditor
          filters={draftFilters}
          changes={changes}
          disabled={frozen}
          onChange={setDraftFilters}
        />

        <hr className="border-0 border-t border-hairline" />

        <RubricEditor
          rubric={draftRubric}
          changes={changes}
          disabled={frozen}
          onChange={setDraftRubric}
        />
      </div>

      {dirty && !frozen && (
        <div className="flex items-center gap-2 border-t border-hairline bg-surface px-5 py-3.5">
          <button
            type="button"
            disabled={busy}
            onClick={() => onApply(draftFilters, draftRubric)}
            className="rounded-full bg-accent px-4 py-2 text-[13px] font-medium text-on-accent transition-colors hover:bg-accent-hover disabled:opacity-40"
          >
            {busy ? "Running…" : "Apply & re-run"}
          </button>
          <button
            type="button"
            onClick={() => {
              setDraftFilters(filters);
              setDraftRubric(rubric);
            }}
            className="rounded-full px-3 py-2 text-[13px] font-medium text-muted transition-colors hover:bg-raised hover:text-ink"
          >
            Discard
          </button>
        </div>
      )}
    </aside>
  );
}
