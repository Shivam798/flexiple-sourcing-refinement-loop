"use client";

import { useState } from "react";
import type { Change } from "@/lib/types";
import { humaniseTarget, renderValue } from "@/lib/changes";

/**
 * What the last refinement moved, in the recruiter's own terms.
 *
 * <p>This is the component the product turns on. A search that silently re-runs after
 * feedback gives a recruiter no reason to believe it understood them; showing the before,
 * the after and the reason makes the adjustment something they can check and disagree with.
 */
export function ChangeLog({ changes, relaxations }: { changes: Change[]; relaxations: string[] }) {
  const [open, setOpen] = useState(true);

  if (changes.length === 0 && relaxations.length === 0) {
    return null;
  }

  return (
    <div className="overflow-hidden rounded-2xl bg-flag-soft shadow-card">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        aria-expanded={open}
        className="flex w-full items-center justify-between px-5 py-3.5 text-left transition-colors hover:bg-flag/5"
      >
        <span className="text-[14px] font-semibold tracking-[-0.01em] text-flag">
          {changes.length > 0
            ? `${changes.length} ${changes.length === 1 ? "change" : "changes"} this round`
            : "Search widened to fill the page"}
        </span>
        <svg
          width="14"
          height="14"
          viewBox="0 0 16 16"
          aria-hidden="true"
          className={`shrink-0 text-flag transition-transform duration-200 ${open ? "rotate-180" : ""}`}
        >
          <path
            d="M4 6.5 8 10.5 12 6.5"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>

      {open && (
        <div className="space-y-3 px-5 pb-4">
          {changes.map((change, index) => (
            <div key={index}>
              <p className="text-[13px] leading-[1.45]">
                <span className="font-semibold">{humaniseTarget(change.target)}</span>{" "}
                <span className="text-muted">
                  <s>{renderValue(change.from)}</s> →{" "}
                  <strong className="font-semibold text-ink">{renderValue(change.to)}</strong>
                </span>
              </p>
              <p className="mt-0.5 text-[13px] leading-[1.45] text-muted">{change.reason}</p>
            </div>
          ))}

          {relaxations.length > 0 && (
            <p className="border-t border-flag/15 pt-3 text-[13px] leading-[1.45] text-muted">
              Too few exact matches, so the search also {relaxations.join(", and ")}.
            </p>
          )}
        </div>
      )}
    </div>
  );
}
