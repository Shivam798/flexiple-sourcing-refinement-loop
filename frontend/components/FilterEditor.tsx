"use client";

import type { Change, CompanyType, Filters } from "@/lib/types";
import { isMarked } from "@/lib/changes";
import { ChipList } from "./ChipList";

const COMPANY_TYPES: CompanyType[] = ["startup", "scaleup", "enterprise", "agency"];

interface Props {
  filters: Filters;
  changes: Change[];
  disabled: boolean;
  onChange: (next: Filters) => void;
}

/** The objective half of the brief: everything here is checked in code, not judged. */
export function FilterEditor({ filters, changes, disabled, onChange }: Props) {
  const patch = (fields: Partial<Filters>) => onChange({ ...filters, ...fields });

  return (
    <section className="space-y-4">
      <h2 className="text-[13px] font-semibold tracking-[-0.01em]">Filters</h2>

      <ChipList
        label="Must have"
        values={filters.requiredSkills}
        marked={isMarked(changes, "filters.requiredSkills")}
        disabled={disabled}
        placeholder="add a skill"
        emptyNote="Any skill"
        onChange={(requiredSkills) => patch({ requiredSkills })}
      />

      <ChipList
        label="Nice to have"
        values={filters.preferredSkills}
        marked={isMarked(changes, "filters.preferredSkills")}
        disabled={disabled}
        placeholder="add a skill"
        onChange={(preferredSkills) => patch({ preferredSkills })}
      />

      <div>
        <p
          className={`text-[12px] font-medium text-faint ${
            isMarked(changes, "filters.minYearsExperience") ||
            isMarked(changes, "filters.maxYearsExperience")
              ? "marked"
              : ""
          }`}
        >
          Experience
        </p>
        <div className="mt-2 flex items-center gap-2 text-[13px]">
          <NumberField
            value={filters.minYearsExperience}
            disabled={disabled}
            label="Minimum years"
            onChange={(minYearsExperience) => patch({ minYearsExperience })}
          />
          <span className="text-faint">to</span>
          <NumberField
            value={filters.maxYearsExperience}
            disabled={disabled}
            label="Maximum years"
            onChange={(maxYearsExperience) => patch({ maxYearsExperience })}
          />
          <span className="text-muted">years</span>
        </div>
      </div>

      <ChipList
        label="Location"
        values={filters.locations}
        marked={isMarked(changes, "filters.locations")}
        disabled={disabled}
        placeholder="add a city"
        emptyNote="Anywhere"
        onChange={(locations) => patch({ locations })}
      />

      <label className="flex items-center gap-2 text-[13px]">
        <input
          type="checkbox"
          checked={filters.includeRemote}
          disabled={disabled}
          onChange={(event) => patch({ includeRemote: event.target.checked })}
          className="h-4 w-4 accent-accent"
        />
        <span className={isMarked(changes, "filters.includeRemote") ? "marked" : ""}>
          Include remote candidates
        </span>
      </label>

      <CompanyTypeField
        label="Currently at"
        selected={filters.currentCompanyTypes}
        marked={isMarked(changes, "filters.currentCompanyTypes")}
        disabled={disabled}
        onChange={(currentCompanyTypes) => patch({ currentCompanyTypes })}
      />

      <CompanyTypeField
        label="Has worked at"
        selected={filters.pastCompanyTypes}
        marked={isMarked(changes, "filters.pastCompanyTypes")}
        disabled={disabled}
        onChange={(pastCompanyTypes) => patch({ pastCompanyTypes })}
      />

      <ChipList
        label="Title includes"
        values={filters.titleKeywords}
        marked={isMarked(changes, "filters.titleKeywords")}
        disabled={disabled}
        placeholder="add a word"
        emptyNote="Any title"
        onChange={(titleKeywords) => patch({ titleKeywords })}
      />

      <ChipList
        label="Title excludes"
        values={filters.excludeTitles}
        marked={isMarked(changes, "filters.excludeTitles")}
        disabled={disabled}
        placeholder="add a word"
        emptyNote="Nothing excluded"
        onChange={(excludeTitles) => patch({ excludeTitles })}
      />
    </section>
  );
}

function NumberField({
  value,
  label,
  disabled,
  onChange,
}: {
  value: number | null;
  label: string;
  disabled: boolean;
  onChange: (next: number | null) => void;
}) {
  return (
    <input
      type="number"
      min={0}
      max={50}
      inputMode="numeric"
      aria-label={label}
      value={value ?? ""}
      disabled={disabled}
      placeholder="any"
      onChange={(event) => {
        const raw = event.target.value;
        onChange(raw === "" ? null : Math.max(0, Number(raw)));
      }}
      className="tabular w-14 rounded-lg bg-raised px-2 py-1.5 text-center outline-none transition-colors placeholder:text-faint hover:bg-hairline/50 disabled:opacity-60"
    />
  );
}

function CompanyTypeField({
  label,
  selected,
  marked,
  disabled,
  onChange,
}: {
  label: string;
  selected: CompanyType[];
  marked: boolean;
  disabled: boolean;
  onChange: (next: CompanyType[]) => void;
}) {
  const toggle = (type: CompanyType) =>
    onChange(selected.includes(type) ? selected.filter((t) => t !== type) : [...selected, type]);

  return (
    <div>
      <p className={`text-[12px] font-medium text-faint ${marked ? "marked" : ""}`}>{label}</p>
      <div className="mt-2 flex flex-wrap gap-1.5">
        {COMPANY_TYPES.map((type) => {
          const active = selected.includes(type);
          return (
            <button
              key={type}
              type="button"
              disabled={disabled}
              aria-pressed={active}
              onClick={() => toggle(type)}
              className={`rounded-full px-3 py-1 text-[13px] transition-colors ${
                active ? "bg-accent text-on-accent" : "bg-raised text-muted hover:bg-hairline/60 hover:text-ink"
              } disabled:opacity-60`}
            >
              {type}
            </button>
          );
        })}
        {selected.length === 0 && (
          <span className="self-center text-[13px] text-faint">Any</span>
        )}
      </div>
    </div>
  );
}
