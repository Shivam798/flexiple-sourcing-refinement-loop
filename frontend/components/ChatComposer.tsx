"use client";

import { useState } from "react";

interface Props {
  busy: boolean;
  pendingVerdicts: number;
  onSend: (feedback: string) => void;
}

const SUGGESTIONS = [
  "Too junior — I need people who have led the work",
  "These are all DBAs, I want product engineers",
  "Open it up to remote candidates in India",
];

/**
 * How the recruiter argues with the results. Free text first, because that is how they
 * actually talk about candidates; the per-card buttons above are a shortcut, not the
 * primary input.
 */
export function ChatComposer({ busy, pendingVerdicts, onSend }: Props) {
  const [text, setText] = useState("");

  const send = () => {
    const feedback = text.trim();
    if (!feedback && pendingVerdicts === 0) return;
    onSend(feedback);
    setText("");
  };

  const canSend = !busy && (text.trim().length > 0 || pendingVerdicts > 0);

  return (
    <div className="border-t border-hairline bg-surface/85 px-6 py-4 backdrop-blur">
      {text.length === 0 && pendingVerdicts === 0 && (
        <ul className="mb-3 flex flex-wrap gap-1.5">
          {SUGGESTIONS.map((suggestion) => (
            <li key={suggestion}>
              <button
                type="button"
                onClick={() => setText(suggestion)}
                className="rounded-full bg-raised px-3 py-1.5 text-[12px] text-muted transition-colors hover:bg-accent-soft hover:text-accent"
              >
                {suggestion}
              </button>
            </li>
          ))}
        </ul>
      )}

      <div className="flex items-end gap-2.5">
        <label htmlFor="feedback" className="sr-only">
          Tell the search what to change
        </label>
        <textarea
          id="feedback"
          name="feedback"
          value={text}
          rows={2}
          disabled={busy}
          autoComplete="off"
          placeholder="1 is too junior, 2 and 4 are right…"
          onChange={(event) => setText(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter" && (event.metaKey || event.ctrlKey)) {
              event.preventDefault();
              send();
            }
          }}
          className="flex-1 resize-none rounded-2xl bg-raised px-4 py-3 text-[15px] leading-[1.45] outline-none transition-colors placeholder:text-faint focus-visible:bg-surface focus-visible:shadow-card disabled:opacity-60"
          style={{ touchAction: "manipulation" }}
        />
        <button
          type="button"
          onClick={send}
          disabled={!canSend}
          className="rounded-full bg-accent px-5 py-3 text-[14px] font-medium text-on-accent transition-colors hover:bg-accent-hover disabled:opacity-30"
        >
          {busy ? "Working…" : "Refine"}
        </button>
      </div>

      {pendingVerdicts > 0 && (
        <p aria-live="polite" className="mt-2 text-[12px] text-muted">
          {pendingVerdicts} {pendingVerdicts === 1 ? "verdict" : "verdicts"} will be sent with this
          message.
        </p>
      )}
    </div>
  );
}
