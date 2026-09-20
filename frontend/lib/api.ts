import type { ApiErrorBody, Health, Session } from "./types";

/**
 * Every failure reaches the UI as one of these, so a component never has to guess whether
 * it is looking at a rate limit, a setup problem or a genuine bug. The distinction drives
 * a different treatment on screen in each case.
 */
export class ApiError extends Error {
  readonly code: ApiErrorBody["code"];
  readonly retryAfterSeconds: number | null;

  constructor(body: ApiErrorBody) {
    super(body.message);
    this.name = "ApiError";
    this.code = body.code;
    this.retryAfterSeconds = body.retryAfterSeconds;
  }

  /** True when the previous results are still worth keeping on screen behind the message. */
  get isRecoverable() {
    return this.code !== "SESSION_NOT_FOUND" && this.code !== "LLM_NOT_CONFIGURED";
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      ...init,
      headers: { "Content-Type": "application/json", ...init?.headers },
    });
  } catch {
    throw new ApiError({
      code: "NETWORK",
      message: "Could not reach the server. Check that the API is running, then try again.",
      retryAfterSeconds: null,
    });
  }

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ApiErrorBody | null;
    throw new ApiError(
      body ?? {
        code: "INTERNAL_ERROR",
        message: `The server returned ${response.status}.`,
        retryAfterSeconds: null,
      },
    );
  }

  return (await response.json()) as T;
}

export const api = {
  health: () => request<Health>("/api/health"),

  start: (query: string) =>
    request<Session>("/api/sessions", { method: "POST", body: JSON.stringify({ query }) }),

  search: (id: string) => request<Session>(`/api/sessions/${id}/search`, { method: "POST" }),

  refine: (id: string, feedback: string, verdicts: { profileId: string; matches: boolean }[]) =>
    request<Session>(`/api/sessions/${id}/refine`, {
      method: "POST",
      body: JSON.stringify({ feedback, verdicts }),
    }),

  editBrief: (id: string, patch: Partial<Pick<Session["rounds"][number], "filters" | "rubric">>) =>
    request<Session>(`/api/sessions/${id}/criteria`, {
      method: "PATCH",
      body: JSON.stringify(patch),
    }),

  freeze: (id: string) => request<Session>(`/api/sessions/${id}/freeze`, { method: "POST" }),

  get: (id: string) => request<Session>(`/api/sessions/${id}`),

  /** Development only; the endpoint is absent unless the API runs with the dev profile. */
  armFault: (mode: "rate_limit" | "malformed" | "timeout") =>
    fetch(`/api/dev/fault?mode=${mode}`, { method: "POST" }),
};
