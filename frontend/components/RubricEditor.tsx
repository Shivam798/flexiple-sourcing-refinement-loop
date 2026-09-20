"use client";

import type { Change, Rubric } from "@/lib/types";
import { isMarked } from "@/lib/changes";
import { ChipList } from "./ChipList";

interface Props {
  rubric: Rubric;
  changes: Change[];
  disabled: boolean;
  onChange: (next: Rubric) => void;
}

/**
 * The subjective half of the brief. Set in serif, because unlike the filters above it is
 * not checked by anything — it is the standard a reader is asked to agree with, and it
 * should look like prose that can be argued with rather than configuration.
 */
export function RubricEditor({ rubric, changes, disabled, onChange }: Props) {
  const patch = (fields: Partial<Rubric>) => onChange({ ...rubric, ...fields });

  const updateCriterion = (index: number, fields: Partial<Rubric["criteria"][number]>) =>
    patch({
      criteria: rubric.criteria.map((criterion, i) =>
        i === index ? { ...criterion, ...fields } : criterion,
      ),
    });

  return (
    <section className="space-y-4">
      <h2 className="text-[13px] font-semibold tracking-[-0.01em]">Fit rubric</h2>

      <AutoTextarea
        value={rubric.roleSummary}
        disabled={disabled}
        ariaLabel="Role summary"
        className={`w-full text-[14px] leading-[1.5] ${
          isMarked(changes, "rubric.roleSummary") ? "marked" : ""
        }`}
        onChange={(roleSummary) => patch({ roleSummary })}
      />

      <ol className="space-y-3">
        {rubric.criteria.map((criterion, index) => {
          const marked = isMarked(changes, `rubric.criteria[${index}]`);
          return (
            <li key={index} className="rounded-xl bg-raised p-3">
              <div className="flex items-baseline gap-2">
                <input
                  value={criterion.name}
                  disabled={disabled}
                  aria-label={`Criterion ${index + 1} name`}
                  onChange={(event) => updateCriterion(index, { name: event.target.value })}
                  className={`min-w-0 flex-1 bg-transparent text-[13px] font-semibold tracking-[-0.01em] outline-none ${
                    marked ? "marked" : ""
                  }`}
                />
                <input
                  type="number"
                  min={0}
                  max={100}
                  step={5}
                  disabled={disabled}
                  aria-label={`Criterion ${index + 1} weight, percent`}
                  value={Math.round(criterion.weight * 100)}
                  onChange={(event) =>
                    updateCriterion(index, { weight: Math.max(0, Number(event.target.value)) / 100 })
                  }
                  className="tabular w-11 shrink-0 rounded-md bg-surface px-1.5 py-0.5 text-right text-[12px] outline-none disabled:opacity-60"
                />
                <span className="shrink-0 text-[12px] text-faint">%</span>
                {!disabled && rubric.criteria.length > 1 && (
                  <button
                    type="button"
                    aria-label={`Remove criterion ${index + 1}`}
                    onClick={() =>
                      patch({ criteria: rubric.criteria.filter((_, i) => i !== index) })
                    }
                    className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-faint transition-colors hover:bg-hairline hover:text-ink"
                  >
                    ×
                  </button>
                )}
              </div>

              <div
                className="mt-2 h-1 w-full overflow-hidden rounded-full bg-hairline"
                role="presentation"
                aria-hidden
              >
                <div
                  className="h-full bg-accent"
                  style={{ width: `${Math.min(100, Math.round(criterion.weight * 100))}%` }}
                />
              </div>

              <AutoTextarea
                value={criterion.whatGoodLooksLike}
                disabled={disabled}
                ariaLabel={`What good looks like for ${criterion.name}`}
                className="mt-2 w-full text-[13px] leading-[1.5] text-muted"
                onChange={(whatGoodLooksLike) => updateCriterion(index, { whatGoodLooksLike })}
              />

              {criterion.redFlags && (
                <p className="mt-1.5 text-[12px] leading-[1.45] text-possible">
                  Watch for: {criterion.redFlags}
                </p>
              )}
            </li>
          );
        })}
      </ol>

      <ChipList
        label="Dealbreakers"
        values={rubric.dealbreakers}
        marked={isMarked(changes, "rubric.dealbreakers")}
        disabled={disabled}
        placeholder="add a dealbreaker"
        emptyNote="None"
        onChange={(dealbreakers) => patch({ dealbreakers })}
      />
    </section>
  );
}

/** A textarea that grows with its content, so editing never hides what was written. */
function AutoTextarea({
  value,
  onChange,
  disabled,
  ariaLabel,
  className,
}: {
  value: string;
  onChange: (next: string) => void;
  disabled: boolean;
  ariaLabel: string;
  className?: string;
}) {
  return (
    <textarea
      value={value}
      disabled={disabled}
      aria-label={ariaLabel}
      rows={Math.max(1, Math.ceil(value.length / 44))}
      onChange={(event) => onChange(event.target.value)}
      className={`resize-none bg-transparent outline-none disabled:opacity-80 ${className ?? ""}`}
    />
  );
}
