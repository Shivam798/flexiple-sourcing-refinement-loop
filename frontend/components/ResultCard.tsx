"use client";

import type { Evidence, ScoredProfile } from "@/lib/types";
import { Avatar } from "./Avatar";
import { VerdictMark } from "./VerdictMark";

interface Props {
  scored: ScoredProfile;
  position: number;
  verdict: boolean | null;
  disabled: boolean;
  onVerdict: (profileId: string, matches: boolean) => void;
}

/**
 * One candidate. The ordinal number is load-bearing rather than decorative: recruiters say
 * "2 and 4 are right", and that phrasing only works if the positions are on screen.
 */
export function ResultCard({ scored, position, verdict, disabled, onVerdict }: Props) {
  const { profile, why, evidence, concerns } = scored;

  return (
    <article className="rounded-2xl bg-surface p-5 shadow-card">
      <div className="flex items-start gap-4">
        <span aria-hidden="true" className="tabular mt-2.5 w-4 shrink-0 text-[13px] text-faint">
          {position}
        </span>
        <Avatar name={profile.name} id={profile.id} />

        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-baseline gap-x-2.5">
            <h3 className="text-[16px] font-semibold tracking-[-0.015em]">{profile.name}</h3>
            <span className="truncate text-[14px] text-muted">{profile.currentTitle}</span>
          </div>

          <p className="tabular mt-1 text-[13px] text-faint">
            {profile.yearsExperience} yrs · {profile.location} · {profile.currentCompany} ·{" "}
            {profile.currentCompanyType}
          </p>

          {why && <p className="mt-3 text-[15px] leading-[1.5] text-ink">{why}</p>}

          {evidence.length > 0 && (
            <ul className="mt-3 flex flex-wrap gap-1.5">
              {evidence.map((item, index) => (
                <li
                  key={`${item.field}-${index}`}
                  className="inline-flex items-center gap-1.5 rounded-full bg-accent-soft px-2.5 py-1 text-[12px] text-accent"
                  title={`Checked against this candidate's ${item.field.replace(/_/g, " ")}`}
                >
                  <svg width="11" height="11" viewBox="0 0 12 12" aria-hidden="true" className="shrink-0">
                    <path
                      d="M2.5 6.2 4.7 8.4 9.5 3.6"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.8"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                  {labelFor(item)}
                </li>
              ))}
            </ul>
          )}

          {concerns.length > 0 && (
            <ul className="mt-2.5 space-y-1">
              {concerns.map((concern) => (
                <li key={concern} className="text-[13px] leading-[1.45] text-possible">
                  {concern}
                </li>
              ))}
            </ul>
          )}
        </div>

        <VerdictMark score={scored.score} verdict={scored.verdict} />
      </div>

      {!disabled && (
        <div className="mt-4 flex gap-2 pl-[4.5rem]">
          <VerdictButton active={verdict === true} onClick={() => onVerdict(profile.id, true)}>
            Right for this role
          </VerdictButton>
          <VerdictButton active={verdict === false} onClick={() => onVerdict(profile.id, false)}>
            Not a fit
          </VerdictButton>
        </div>
      )}
    </article>
  );
}

/**
 * A citation's value is usually self-describing ("AWS RDS", "startup"), but a bare number
 * is not — "6" alone tells a reader nothing. Those get their unit back.
 */
function labelFor(evidence: Evidence): string {
  switch (evidence.field) {
    case "years_experience":
      return `${evidence.value} yrs experience`;
    case "location":
      return `Based in ${evidence.value}`;
    case "education":
    case "summary":
      return truncate(evidence.value, 48);
    default:
      return truncate(evidence.value, 48);
  }
}

function truncate(value: string, limit: number): string {
  return value.length <= limit ? value : `${value.slice(0, limit).trimEnd()}…`;
}

function VerdictButton({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      style={{ touchAction: "manipulation" }}
      className={`rounded-full px-3.5 py-1.5 text-[13px] font-medium transition-colors ${
        active
          ? "bg-accent text-on-accent"
          : "bg-raised text-muted hover:bg-accent-soft hover:text-accent"
      }`}
    >
      {children}
    </button>
  );
}
