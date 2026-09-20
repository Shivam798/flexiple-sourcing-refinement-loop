"use client";

import { useState } from "react";

interface Props {
  label: string;
  values: string[];
  marked?: boolean;
  disabled?: boolean;
  placeholder: string;
  emptyNote?: string;
  onChange: (next: string[]) => void;
}

/**
 * An editable set of values. The recruiter removes a constraint by striking it out and adds
 * one by typing — the same gesture in both directions, with no modal in between. Editing the
 * brief has to be faster than re-describing the role, or nobody will do it.
 */
export function ChipList({
  label,
  values,
  marked,
  disabled,
  placeholder,
  emptyNote,
  onChange,
}: Props) {
  const [draft, setDraft] = useState("");

  const add = () => {
    const value = draft.trim();
    if (!value || values.some((v) => v.toLowerCase() === value.toLowerCase())) {
      setDraft("");
      return;
    }
    onChange([...values, value]);
    setDraft("");
  };

  return (
    <div>
      <p className={`text-[12px] font-medium text-faint ${marked ? "marked" : ""}`}>{label}</p>

      <div className="mt-2 flex flex-wrap items-center gap-1.5">
        {values.map((value) => (
          <span
            key={value}
            className="inline-flex items-center gap-1 rounded-full bg-raised py-1 pl-3 pr-1.5 text-[13px]"
          >
            {value}
            {!disabled && (
              <button
                type="button"
                onClick={() => onChange(values.filter((v) => v !== value))}
                aria-label={`Remove ${value}`}
                className="flex h-4 w-4 items-center justify-center rounded-full text-faint transition-colors hover:bg-hairline hover:text-ink"
              >
                ×
              </button>
            )}
          </span>
        ))}

        {values.length === 0 && (
          <span className="text-[13px] text-faint">{emptyNote ?? "No constraint"}</span>
        )}

        {!disabled && (
          <input
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            onBlur={add}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                event.preventDefault();
                add();
              }
            }}
            placeholder={placeholder}
            aria-label={`Add to ${label}`}
            autoComplete="off"
            spellCheck={false}
            className="min-w-24 flex-1 bg-transparent py-1 text-[13px] outline-none placeholder:text-faint"
          />
        )}
      </div>
    </div>
  );
}
