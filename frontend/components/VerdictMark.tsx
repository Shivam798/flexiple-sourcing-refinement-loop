import type { VerdictLabel } from "@/lib/types";

const COPY: Record<VerdictLabel, string> = {
  strong: "Strong fit",
  possible: "Possible",
  weak: "Weak fit",
};

const TONE: Record<VerdictLabel, string> = {
  strong: "text-strong",
  possible: "text-possible",
  weak: "text-weak",
};

/**
 * Score and verdict read as one unit. Ordinal information genuinely needs a colour scale
 * here, carried on the numeral itself rather than in a filled badge — a page of coloured
 * pills would shout louder than the candidates do.
 */
export function VerdictMark({
  score,
  verdict,
}: {
  score: number | null;
  verdict: VerdictLabel | null;
}) {
  if (score === null || verdict === null) {
    return (
      <div className="w-20 shrink-0 text-right">
        <p className="text-[13px] text-faint">Not scored</p>
        <p className="text-[11px] text-faint">Passed filters</p>
      </div>
    );
  }

  return (
    <div className="w-20 shrink-0 text-right">
      <p className={`tabular text-[26px] font-semibold leading-none tracking-[-0.03em] ${TONE[verdict]}`}>
        {score}
      </p>
      <p className="mt-1.5 text-[12px] text-muted">{COPY[verdict]}</p>
    </div>
  );
}
