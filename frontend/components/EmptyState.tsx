"use client";

import type { Filters } from "@/lib/types";

interface Props {
  filters: Filters;
  frozen: boolean;
  onLoosen: (next: Filters) => void;
}

/**
 * Nobody matched, even after the backend's relaxation ladder ran. An empty screen is the
 * moment a recruiter decides whether a tool is worth reopening, so it offers the specific
 * concessions that would actually change this result rather than an apology.
 */
export function EmptyState({ filters, frozen, onLoosen }: Props) {
  const options: { label: string; next: Filters }[] = [];

  if (filters.requiredSkills.length > 1) {
    options.push({
      label: `Require only ${filters.requiredSkills[0]}`,
      next: { ...filters, requiredSkills: filters.requiredSkills.slice(0, 1) },
    });
  }
  if (filters.minYearsExperience !== null || filters.maxYearsExperience !== null) {
    options.push({
      label: "Drop the experience range",
      next: { ...filters, minYearsExperience: null, maxYearsExperience: null },
    });
  }
  if (filters.locations.length > 0) {
    options.push({ label: "Search everywhere", next: { ...filters, locations: [] } });
  }
  if (filters.excludeTitles.length > 0) {
    options.push({ label: "Stop excluding titles", next: { ...filters, excludeTitles: [] } });
  }

  return (
    <div className="rounded-2xl bg-surface px-6 py-10 shadow-card">
      <p className="text-[17px] font-semibold tracking-[-0.015em]">
        Nobody in the pool matches this brief.
      </p>
      <p className="mt-2 max-w-md text-[15px] leading-[1.5] text-muted">
        The search already widened as far as it could and still found no one. These are the
        constraints doing the excluding.
      </p>

      {options.length > 0 && !frozen && (
        <ul className="mt-6 flex flex-wrap gap-2">
          {options.map((option) => (
            <li key={option.label}>
              <button
                type="button"
                onClick={() => onLoosen(option.next)}
                className="rounded-full bg-raised px-4 py-2 text-[13px] font-medium transition-colors hover:bg-accent-soft hover:text-accent"
              >
                {option.label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
