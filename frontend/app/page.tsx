"use client";

import { useCallback, useEffect, useState } from "react";
import { ApiError, api } from "@/lib/api";
import type { Filters, Health, Rubric, Session } from "@/lib/types";
import { BriefPane } from "@/components/BriefPane";
import { ChangeLog } from "@/components/ChangeLog";
import { ChatComposer } from "@/components/ChatComposer";
import { EmptyState } from "@/components/EmptyState";
import { ErrorBanner } from "@/components/ErrorBanner";
import { FrozenSummary } from "@/components/FrozenSummary";
import { ResultCard } from "@/components/ResultCard";
import { ThinkingState, type Stage } from "@/components/ThinkingState";
import { SearchScreen } from "@/components/SearchScreen";
import { SetupScreen } from "@/components/SetupScreen";

/**
 * Everything the recruiter can do, as data rather than as callbacks.
 *
 * <p>Describing actions this way means retrying one is re-dispatching the same value, so
 * there is exactly one place where a call is made and one place where a failure is handled.
 */
type Action =
  | { kind: "start"; query: string }
  | { kind: "refine"; feedback: string; verdicts: Record<string, boolean> }
  | { kind: "edit"; filters: Filters; rubric: Rubric }
  | { kind: "freeze" };

type Phase = "idle" | "interpreting" | "searching" | "ready";

/**
 * The whole loop lives on one screen.
 *
 * <p>No routing, because a session is held in the server's memory for the life of the
 * process — giving it a shareable URL would imply a durability this deliberately does not
 * have.
 */
export default function Home() {
  const [health, setHealth] = useState<Health | null>(null);
  const [healthChecked, setHealthChecked] = useState(false);
  const [phase, setPhase] = useState<Phase>("idle");
  const [session, setSession] = useState<Session | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [verdicts, setVerdicts] = useState<Record<string, boolean>>({});
  const [lastAction, setLastAction] = useState<Action | null>(null);
  const [confirmingFreeze, setConfirmingFreeze] = useState(false);

  useEffect(() => {
    api
      .health()
      .then(setHealth)
      .catch(() => setHealth(null))
      .finally(() => setHealthChecked(true));
  }, []);

  const round = session?.rounds.at(-1) ?? null;
  const busy = phase === "interpreting" || phase === "searching";
  const pendingVerdicts = Object.keys(verdicts).length;

  /**
   * Runs an action. A failure sets the banner and leaves the session untouched, so the
   * candidates the recruiter is reading survive every kind of model error.
   */
  const dispatch = useCallback(
    async (action: Action) => {
      setLastAction(action);
      setError(null);

      try {
        if (action.kind === "start") {
          setPhase("interpreting");
          const started = await api.start(action.query);
          setSession(started);
          setPhase("searching");
          setSession(await api.search(started.id));
        } else if (session) {
          setPhase("searching");

          if (action.kind === "refine") {
            const payload = Object.entries(action.verdicts).map(([profileId, matches]) => ({
              profileId,
              matches,
            }));
            setSession(await api.refine(session.id, action.feedback, payload));
            setVerdicts({});
          } else if (action.kind === "edit") {
            setSession(
              await api.editBrief(session.id, { filters: action.filters, rubric: action.rubric }),
            );
          } else {
            setSession(await api.freeze(session.id));
          }
        }
        setPhase("ready");
      } catch (caught) {
        setError(
          caught instanceof ApiError
            ? caught
            : new ApiError({
                code: "INTERNAL_ERROR",
                message: "Something unexpected went wrong.",
                retryAfterSeconds: null,
              }),
        );
        setPhase(session ? "ready" : "idle");
      }
    },
    [session],
  );

  const toggleVerdict = (profileId: string, matches: boolean) =>
    setVerdicts((current) => {
      if (current[profileId] === matches) {
        const next = { ...current };
        delete next[profileId];
        return next;
      }
      return { ...current, [profileId]: matches };
    });

  const stage: Stage = phase === "interpreting" ? "interpreting" : "searching";

  // Hold the first paint until the health check answers, so a missing key shows setup
  // instructions rather than a search box that was never going to work.
  if (!healthChecked) {
    return <div className="min-h-dvh bg-paper" aria-busy="true" />;
  }

  if (health && !health.llmConfigured) {
    return <SetupScreen />;
  }

  if (!session) {
    return (
      <SearchScreen
        busy={busy}
        error={error}
        onDismissError={() => setError(null)}
        onSubmit={(query) => dispatch({ kind: "start", query })}
      />
    );
  }

  if (session.frozen && round) {
    return (
      <FrozenSummary round={round} query={session.originalQuery} rounds={session.roundCount} />
    );
  }

  return (
    <div className="grid h-dvh grid-cols-1 lg:grid-cols-[22rem_minmax(0,1fr)]">
      {round && (
        <BriefPane
          filters={round.filters}
          rubric={round.rubric}
          changes={round.changes}
          frozen={session.frozen}
          busy={busy}
          onApply={(filters, rubric) => dispatch({ kind: "edit", filters, rubric })}
        />
      )}

      <main className="flex h-dvh min-w-0 flex-col">
        <header className="flex items-center gap-4 border-b border-hairline bg-surface/85 px-6 py-3.5 backdrop-blur">
          <div className="min-w-0 flex-1">
            <p className="truncate text-[15px] font-semibold tracking-[-0.015em]">
              {session.originalQuery}
            </p>
            <p className="tabular mt-0.5 text-[13px] text-faint">
              Round {session.roundCount}
              {round && round.results.length > 0 && (
                round.totalMatched > round.results.length
                  ? ` · top ${round.results.length} of ${round.totalMatched} matches`
                  : ` · ${round.results.length} candidates`
              )}
            </p>
          </div>
          <button
            type="button"
            onClick={() => setConfirmingFreeze(true)}
            disabled={busy || !round?.results.length}
            className="shrink-0 rounded-full bg-ink px-4 py-2 text-[13px] font-medium text-paper transition-opacity hover:opacity-85 disabled:opacity-25"
          >
            Freeze search
          </button>
        </header>

        <div className="flex-1 overflow-y-auto overscroll-contain px-6 py-5">
          <div className="mx-auto max-w-[46rem] space-y-3">
            {error && (
              <ErrorBanner
                key={`${error.code}-${error.message}`}
                error={error}
                onRetry={error.isRecoverable && lastAction ? () => dispatch(lastAction) : undefined}
                onDismiss={() => setError(null)}
              />
            )}

            {round?.assistantReply && !busy && (
              <p className="rounded-2xl bg-surface px-5 py-4 text-[15px] leading-[1.5] shadow-card">
                {round.assistantReply}
              </p>
            )}

            {busy ? (
              <ThinkingState stage={stage} />
            ) : round && round.results.length > 0 ? (
              <>
                <ChangeLog changes={round.changes} relaxations={round.relaxations} />
                {round.totalMatched > round.results.length && (
                  <p className="px-1 text-[13px] text-muted">
                    Showing the {round.results.length} strongest of {round.totalMatched} matches.
                    Refine the brief to change who makes the page.
                  </p>
                )}
                {round.results.map((scored, index) => (
                  <ResultCard
                    key={scored.profile.id}
                    scored={scored}
                    position={index + 1}
                    verdict={scored.profile.id in verdicts ? verdicts[scored.profile.id] : null}
                    disabled={session.frozen}
                    onVerdict={toggleVerdict}
                  />
                ))}
              </>
            ) : round ? (
              <EmptyState
                filters={round.filters}
                frozen={session.frozen}
                onLoosen={(filters) => dispatch({ kind: "edit", filters, rubric: round.rubric })}
              />
            ) : null}
          </div>
        </div>

        <ChatComposer
          busy={busy}
          pendingVerdicts={pendingVerdicts}
          onSend={(feedback) => dispatch({ kind: "refine", feedback, verdicts })}
        />
      </main>

      {confirmingFreeze && (
        <ConfirmFreeze
          candidates={round?.results.length ?? 0}
          onCancel={() => setConfirmingFreeze(false)}
          onConfirm={() => {
            setConfirmingFreeze(false);
            dispatch({ kind: "freeze" });
          }}
        />
      )}
    </div>
  );
}

/**
 * Freezing cannot be undone — the session refuses every further change — so it asks first.
 */
function ConfirmFreeze({
  candidates,
  onCancel,
  onConfirm,
}: {
  candidates: number;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="freeze-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-ink/25 p-6 backdrop-blur-sm"
      onKeyDown={(event) => event.key === "Escape" && onCancel()}
    >
      <div className="w-full max-w-sm rounded-2xl bg-surface p-6 shadow-raised">
        <h2 id="freeze-title" className="text-[17px] font-semibold tracking-[-0.02em]">
          Freeze this search?
        </h2>
        <p className="mt-2 text-[14px] leading-[1.5] text-muted">
          The brief and the {candidates} {candidates === 1 ? "candidate" : "candidates"} become
          final. You will not be able to refine further.
        </p>
        <div className="mt-6 flex gap-2.5">
          <button
            type="button"
            autoFocus
            onClick={onConfirm}
            className="flex-1 rounded-full bg-accent px-4 py-2.5 text-[14px] font-medium text-on-accent transition-colors hover:bg-accent-hover"
          >
            Freeze it
          </button>
          <button
            type="button"
            onClick={onCancel}
            className="flex-1 rounded-full bg-raised px-4 py-2.5 text-[14px] font-medium transition-colors hover:bg-hairline/60"
          >
            Keep refining
          </button>
        </div>
      </div>
    </div>
  );
}
