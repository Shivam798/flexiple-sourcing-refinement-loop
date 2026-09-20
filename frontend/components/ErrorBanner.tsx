"use client";

import { useEffect, useState } from "react";
import type { ApiError } from "@/lib/api";

/**
 * Failures are shown above the results, never instead of them.
 *
 * <p>A model call that fails must not cost the recruiter the candidates they were reading,
 * so this is a banner rather than a screen. A rate limit gets a live countdown, because
 * "try again later" is not actionable and "try again in 14s" is.
 */
export function ErrorBanner({
  error,
  onRetry,
  onDismiss,
}: {
  error: ApiError;
  onRetry?: () => void;
  onDismiss: () => void;
}) {
  // The parent remounts this per distinct error via `key`, so the countdown initialises
  // from props rather than being reset by an effect.
  const [secondsLeft, setSecondsLeft] = useState(error.retryAfterSeconds ?? 0);

  useEffect(() => {
    if (secondsLeft <= 0) return;
    const timer = setTimeout(() => setSecondsLeft((value) => value - 1), 1000);
    return () => clearTimeout(timer);
  }, [secondsLeft]);

  const waiting = secondsLeft > 0;

  return (
    <div
      role="alert"
      aria-live="polite"
      className="flex items-start gap-4 rounded-2xl bg-surface p-4 shadow-card ring-1 ring-possible/25"
    >
      <span
        aria-hidden="true"
        className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-possible/15 text-[12px] font-semibold text-possible"
      >
        !
      </span>

      <div className="min-w-0 flex-1">
        <p className="text-[14px] font-semibold tracking-[-0.01em]">{title(error.code)}</p>
        <p className="mt-0.5 text-[14px] leading-[1.45] text-muted">{error.message}</p>
      </div>

      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          disabled={waiting}
          className="shrink-0 rounded-full bg-raised px-3.5 py-1.5 text-[13px] font-medium transition-colors hover:bg-hairline/60 disabled:opacity-50"
        >
          {waiting ? `Retry in ${secondsLeft}s` : "Try again"}
        </button>
      )}

      <button
        type="button"
        onClick={onDismiss}
        aria-label="Dismiss this message"
        className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-faint transition-colors hover:bg-raised hover:text-ink"
      >
        ×
      </button>
    </div>
  );
}

function title(code: ApiError["code"]): string {
  switch (code) {
    case "LLM_RATE_LIMITED":
      return "Rate limited by the model";
    case "LLM_INVALID_RESPONSE":
      return "The model returned something unusable";
    case "LLM_UNAVAILABLE":
      return "The model did not respond";
    case "LLM_NOT_CONFIGURED":
      return "No API key configured";
    case "NETWORK":
      return "Cannot reach the server";
    case "SEARCH_FROZEN":
      return "This search is frozen";
    default:
      return "Something went wrong";
  }
}
