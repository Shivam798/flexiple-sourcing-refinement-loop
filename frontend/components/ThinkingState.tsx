export type Stage = "interpreting" | "searching";

const STEPS: { key: Stage; done: string; doing: string }[] = [
  { key: "interpreting", done: "Read your requirement", doing: "Reading your requirement…" },
  {
    key: "searching",
    done: "Ranked the shortlist",
    doing: "Filtering 48 profiles, then scoring the survivors…",
  },
];

/**
 * Progress, not a spinner. The two model calls are genuinely different steps and take
 * noticeably different amounts of time, so naming them tells the recruiter what they are
 * waiting for — and the skeletons keep the page from collapsing while they wait.
 */
export function ThinkingState({ stage, cards = 4 }: { stage: Stage; cards?: number }) {
  const currentIndex = STEPS.findIndex((step) => step.key === stage);

  return (
    <div className="space-y-3">
      <ol aria-live="polite" className="rounded-2xl bg-surface px-5 py-4 shadow-card">
        {STEPS.map((step, index) => {
          const state = index < currentIndex ? "done" : index === currentIndex ? "doing" : "todo";
          return (
            <li
              key={step.key}
              className={`flex items-center gap-2.5 py-1 text-[14px] ${
                state === "todo" ? "text-faint" : state === "doing" ? "text-ink" : "text-muted"
              }`}
            >
              <span aria-hidden="true" className="flex w-4 justify-center">
                {state === "done" ? (
                  <svg width="13" height="13" viewBox="0 0 12 12" className="text-strong">
                    <path
                      d="M2.5 6.2 4.7 8.4 9.5 3.6"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.8"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                ) : state === "doing" ? (
                  <PulseDot />
                ) : (
                  <span className="h-1 w-1 rounded-full bg-hairline" />
                )}
              </span>
              {state === "done" ? step.done : step.doing}
            </li>
          );
        })}
      </ol>

      <div aria-hidden="true" className="space-y-3">
        {Array.from({ length: cards }).map((_, index) => (
          <div key={index} className="rounded-2xl bg-surface p-5 shadow-card">
            <div className="flex items-start gap-4">
              <div className="h-11 w-11 shrink-0 rounded-full bg-raised" />
              <div className="flex-1 space-y-2.5 pt-1">
                <div className="h-3.5 w-44 rounded-full bg-raised" />
                <div className="h-3 w-60 rounded-full bg-raised" />
                <div className="h-3 w-full max-w-md rounded-full bg-raised" />
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function PulseDot() {
  return (
    <span className="relative inline-flex h-2 w-2">
      <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-accent opacity-60" />
      <span className="relative inline-flex h-2 w-2 rounded-full bg-accent" />
    </span>
  );
}
