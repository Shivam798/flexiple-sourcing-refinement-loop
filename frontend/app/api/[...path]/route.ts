import { NextRequest } from "next/server";

/**
 * Server-side proxy to the Spring API.
 *
 * <p>The browser only ever talks to this Next server, which means there is no CORS to
 * configure, no backend URL in the client bundle, and the same code path in local
 * development and in Docker — where the API is reachable as {@code http://api:8080} on the
 * internal network and not from the browser at all.
 *
 * <p>Deliberately a route handler rather than a {@code next.config} rewrite: rewrites are
 * resolved when the image is built, so the backend address would be frozen at build time.
 * Read here, it is an ordinary runtime environment variable.
 */
const API_BASE_URL = process.env.API_BASE_URL ?? "http://localhost:8080";

async function proxy(request: NextRequest, path: string[]) {
  const target = `${API_BASE_URL}/api/${path.join("/")}${request.nextUrl.search}`;

  try {
    const response = await fetch(target, {
      method: request.method,
      headers: { "Content-Type": "application/json" },
      body: request.method === "GET" || request.method === "DELETE" ? undefined : await request.text(),
      cache: "no-store",
    });

    return new Response(response.body, {
      status: response.status,
      headers: { "Content-Type": response.headers.get("Content-Type") ?? "application/json" },
    });
  } catch {
    // The API is down or unreachable. Answer in the same error shape the API itself uses,
    // so the interface has one failure format to render rather than two.
    return Response.json(
      {
        code: "NETWORK",
        message: "Could not reach the search API. Check that it is running, then try again.",
        retryAfterSeconds: null,
      },
      { status: 503 },
    );
  }
}

type Context = { params: Promise<{ path: string[] }> };

export async function GET(request: NextRequest, context: Context) {
  return proxy(request, (await context.params).path);
}

export async function POST(request: NextRequest, context: Context) {
  return proxy(request, (await context.params).path);
}

export async function PATCH(request: NextRequest, context: Context) {
  return proxy(request, (await context.params).path);
}

export async function DELETE(request: NextRequest, context: Context) {
  return proxy(request, (await context.params).path);
}
