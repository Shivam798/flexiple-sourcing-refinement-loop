"use client";

import { useState } from "react";
import type { Round } from "@/lib/types";
import { Avatar } from "./Avatar";

/**
 * The terminal state. Read-only, wider than the working view, and organised for handing on
 * — the recruiter's next action is telling someone else about this shortlist, so the
 * shortlist is copyable as text.
 */
export function FrozenSummary({ round, query, rounds }: { round: Round; query: string; rounds: number }) {
  const [copied, setCopied] = useState(false);

  const copy = async () => {
    const text = round.results
      .map((r, i) => `${i + 1}. ${r.profile.name} — ${r.profile.currentTitle}, ${r.profile.currentCompany} (${r.profile.yearsExperience} yrs, ${r.profile.location})${r.score !== null ? ` — ${r.score}/100` : ""}`)
      .join("\n");
    await navigator.clipboard.writeText(`${query}\n\n${text}`);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="mx-auto max-w-3xl px-6 py-10">
      <p className="text-[13px] text-muted">
        Frozen after {rounds} {rounds === 1 ? "round" : "rounds"} · {round.results.length}{" "}
        {round.results.length === 1 ? "candidate" : "candidates"}
      </p>
      <h1 className="mt-2 display text-[34px] leading-[1.1] text-balance">{query}</h1>

      <section className="mt-8">
        <div className="flex items-baseline justify-between">
          <h2 className="text-[13px] font-semibold tracking-[-0.01em]">Shortlist</h2>
          <button
            type="button"
            onClick={copy}
            className="rounded-full bg-raised px-3 py-1.5 text-[12px] font-medium transition-colors hover:bg-accent-soft hover:text-accent"
          >
            {copied ? "Copied" : "Copy as text"}
          </button>
        </div>

        <ol className="mt-3 divide-y divide-hairline overflow-hidden rounded-2xl bg-surface shadow-card">
          {round.results.map((scored, index) => (
            <li key={scored.profile.id} className="flex items-start gap-3 px-5 py-4">
              <span className="tabular mt-1 w-4 text-[13px] text-faint">{index + 1}</span>
              <Avatar name={scored.profile.name} id={scored.profile.id} />
              <div className="min-w-0 flex-1">
                <p className="text-[15px] font-medium">{scored.profile.name}</p>
                <p className="tabular text-[13px] text-muted">
                  {scored.profile.currentTitle} · {scored.profile.currentCompany} ·{" "}
                  {scored.profile.yearsExperience} yrs · {scored.profile.location}
                </p>
                {scored.why && (
                  <p className="mt-1 text-[14px] leading-[1.5] text-muted">{scored.why}</p>
                )}
              </div>
              {scored.score !== null && (
                <span className="tabular text-[18px] text-strong">{scored.score}</span>
              )}
            </li>
          ))}
        </ol>
      </section>

      <div className="mt-10 grid gap-10 sm:grid-cols-2">
        <section>
          <h2 className="text-[13px] font-semibold tracking-[-0.01em]">Frozen filters</h2>
          <dl className="mt-3 space-y-1.5 text-[13px]">
            <Row label="Must have" value={round.filters.requiredSkills.join(", ")} />
            <Row label="Nice to have" value={round.filters.preferredSkills.join(", ")} />
            <Row
              label="Experience"
              value={
                round.filters.minYearsExperience === null && round.filters.maxYearsExperience === null
                  ? ""
                  : `${round.filters.minYearsExperience ?? 0}–${round.filters.maxYearsExperience ?? "any"} yrs`
              }
            />
            <Row
              label="Location"
              value={
                round.filters.locations.join(", ") +
                (round.filters.includeRemote ? " (remote included)" : "")
              }
            />
            <Row label="Currently at" value={round.filters.currentCompanyTypes.join(", ")} />
            <Row label="Has worked at" value={round.filters.pastCompanyTypes.join(", ")} />
            <Row label="Title includes" value={round.filters.titleKeywords.join(", ")} />
            <Row label="Title excludes" value={round.filters.excludeTitles.join(", ")} />
          </dl>
        </section>

        <section>
          <h2 className="text-[13px] font-semibold tracking-[-0.01em]">Frozen rubric</h2>
          <p className="mt-3 text-[15px] leading-[1.5]">{round.rubric.roleSummary}</p>
          <ol className="mt-3 space-y-2.5">
            {round.rubric.criteria.map((criterion) => (
              <li key={criterion.name} className="rounded-xl bg-surface p-4 shadow-card">
                <p className="text-[13px] font-medium">
                  {criterion.name}{" "}
                  <span className="tabular text-muted">{Math.round(criterion.weight * 100)}%</span>
                </p>
                <p className="text-[14px] leading-[1.5] text-muted">
                  {criterion.whatGoodLooksLike}
                </p>
              </li>
            ))}
          </ol>
          {round.rubric.dealbreakers.length > 0 && (
            <p className="mt-3 text-[13px] text-muted">
              Dealbreakers: {round.rubric.dealbreakers.join("; ")}
            </p>
          )}
        </section>
      </div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-3">
      <dt className="w-28 shrink-0 text-muted">{label}</dt>
      <dd className="flex-1">{value || <span className="text-faint">No constraint</span>}</dd>
    </div>
  );
}
