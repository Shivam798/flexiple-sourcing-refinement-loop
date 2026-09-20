"use client";

import { useState } from "react";
import type { ApiError } from "@/lib/api";
import { ErrorBanner } from "./ErrorBanner";
import { Wordmark } from "./Wordmark";

const EXAMPLES = [
  "RDS developers with 4-7 years of experience who have worked at startups, for a role based in Bangalore",
  "Senior frontend engineers who care about accessibility, ideally from product companies rather than agencies",
  "Someone who has run databases in production at scale — the title matters less to me than the ownership",
];

/** A genuine sequence, which is the only thing that justifies numbering the steps. */
const STEPS = [
  {
    title: "It reads your sentence",
    body: "You get filters a computer can check, and a rubric describing what good looks like. Both are yours to edit.",
  },
  {
    title: "It ranks everyone who passes",
    body: "Each explanation quotes something real from that profile. Anything it cannot verify is removed before you see it.",
  },
  {
    title: "You argue with it",
    body: "Say what is wrong. The brief changes, and it shows you exactly which field moved and why.",
  },
];

interface Props {
  busy: boolean;
  error: ApiError | null;
  onSubmit: (query: string) => void;
  onDismissError: () => void;
}

export function SearchScreen({ busy, error, onSubmit, onDismissError }: Props) {
  const [query, setQuery] = useState("");

  const submit = () => {
    const trimmed = query.trim();
    if (trimmed && !busy) onSubmit(trimmed);
  };

  return (
    <div className="min-h-dvh">
      {error && <ErrorBanner error={error} onDismiss={onDismissError} />}

      <header className="mx-auto flex max-w-[44rem] items-center justify-between px-6 pt-7">
        <Wordmark />
        <span className="tabular text-[13px] text-faint">48 profiles</span>
      </header>

      <main className="mx-auto max-w-[44rem] px-6 pb-28 pt-20 sm:pt-28">
        <h1 className="display max-w-[14ch] text-[48px] leading-[1.05] text-balance sm:text-[62px]">
          Describe the hire.
        </h1>
        <p className="mt-5 max-w-[44ch] text-[17px] leading-[1.5] text-muted">
          One sentence, the way you would say it to a colleague. You&rsquo;ll get a set of
          filters and a standard of fit — then you tell it what it got wrong, until the
          shortlist is right.
        </p>

        <div className="mt-10">
          <label htmlFor="requirement" className="sr-only">
            Describe who you are looking for
          </label>
          <textarea
            id="requirement"
            name="requirement"
            value={query}
            rows={3}
            autoFocus
            disabled={busy}
            spellCheck={false}
            autoComplete="off"
            placeholder="RDS developers with 4-7 years of experience who have worked at startups, in Bangalore…"
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter" && (event.metaKey || event.ctrlKey)) {
                event.preventDefault();
                submit();
              }
            }}
            className="w-full resize-none rounded-2xl bg-surface px-5 py-4 text-[17px] leading-[1.5] shadow-card outline-none transition-shadow placeholder:text-faint focus-visible:shadow-raised disabled:opacity-60"
            style={{ touchAction: "manipulation" }}
          />

          <div className="mt-5 flex flex-wrap items-center gap-x-4 gap-y-2">
            <button
              type="button"
              onClick={submit}
              disabled={busy || query.trim().length === 0}
              className="rounded-full bg-accent px-6 py-3 text-[15px] font-medium text-on-accent transition-colors hover:bg-accent-hover disabled:opacity-30"
            >
              {busy ? "Reading your requirement…" : "Start the search"}
            </button>
            <span className="text-[13px] text-faint">
              or press&nbsp;<kbd className="font-sans">⌘</kbd>&nbsp;<kbd className="font-sans">↵</kbd>
            </span>
          </div>
        </div>

        <section className="mt-16">
          <h2 className="text-[13px] font-medium text-faint">Start from one of these</h2>
          <ul className="mt-3 divide-y divide-hairline overflow-hidden rounded-2xl bg-surface shadow-card">
            {EXAMPLES.map((example) => (
              <li key={example}>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => setQuery(example)}
                  className="w-full px-5 py-4 text-left text-[15px] leading-[1.45] text-muted transition-colors hover:bg-raised hover:text-ink"
                >
                  {example}
                </button>
              </li>
            ))}
          </ul>
        </section>

        <section className="mt-16">
          <ol className="grid gap-x-8 gap-y-9 sm:grid-cols-3">
            {STEPS.map((step, index) => (
              <li key={step.title}>
                <span
                  aria-hidden="true"
                  className="tabular flex h-6 w-6 items-center justify-center rounded-full bg-accent-soft text-[12px] font-medium text-accent"
                >
                  {index + 1}
                </span>
                <h3 className="mt-3 text-[15px] font-semibold tracking-[-0.015em]">{step.title}</h3>
                <p className="mt-1.5 text-[14px] leading-[1.5] text-muted">{step.body}</p>
              </li>
            ))}
          </ol>
        </section>
      </main>
    </div>
  );
}
