/**
 * Shown when the API reports no model key. Reached on first load rather than after the
 * recruiter has typed out a requirement and pressed the button — the health check exists
 * precisely so this failure is discovered before any work is lost.
 */
export function SetupScreen() {
  return (
    <div className="flex min-h-dvh items-center justify-center px-4">
      <div className="w-full max-w-lg">
        <h1 className="display text-[32px] leading-[1.1] text-balance">One thing left to set up.</h1>
        <p className="mt-4 text-[16px] leading-[1.5] text-muted">
          This app calls Google Gemini on the server for every search. It needs an API key,
          supplied as an environment variable — it is never committed and never reaches the
          browser.
        </p>

        <ol className="mt-8 space-y-5 text-[14px]">
          <li>
            <p className="text-muted">1 — Get a free key</p>
            <a
              href="https://aistudio.google.com/apikey"
              target="_blank"
              rel="noreferrer"
              className="text-accent underline underline-offset-4 hover:text-accent-hover"
            >
              aistudio.google.com/apikey
            </a>
          </li>
          <li>
            <p className="text-muted">2 — Put it in the project&rsquo;s .env file</p>
            <pre className="mt-2 overflow-x-auto rounded-xl bg-surface px-4 py-3 text-[13px] shadow-card">
              GEMINI_API_KEY=your-key-here
            </pre>
          </li>
          <li>
            <p className="text-muted">3 — Restart the API</p>
            <pre className="mt-2 overflow-x-auto rounded-xl bg-surface px-4 py-3 text-[13px] shadow-card">
              docker compose up --build
            </pre>
          </li>
        </ol>

        <p className="mt-6 text-[13px] text-faint">
          This page checks again on reload.
        </p>
      </div>
    </div>
  );
}
