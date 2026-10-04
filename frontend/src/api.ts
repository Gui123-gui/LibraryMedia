export const ACCESS_TOKEN_KEY = "library.accessToken";
export const TOKEN_EXPIRATION_KEY = "library.tokenExpiresAt";

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
    readonly fieldErrors: Record<string, string> = {},
    readonly affectedMediaCount?: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function fieldErrors(value: unknown): Record<string, string> {
  if (isRecord(value)) {
    return Object.fromEntries(
      Object.entries(value).filter((entry): entry is [string, string] =>
        typeof entry[1] === "string",
      ),
    );
  }

  if (Array.isArray(value)) {
    return value.reduce<Record<string, string>>((result, item) => {
      if (isRecord(item) && typeof item.field === "string" && typeof item.message === "string") {
        result[item.field] = item.message;
      }
      return result;
    }, {});
  }

  return {};
}

export async function apiRequest(path: string, init: RequestInit = {}): Promise<unknown> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const token = sessionStorage.getItem(ACCESS_TOKEN_KEY);
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const response = await fetch(path, { ...init, headers });
  const responseText = response.status === 204 ? "" : await response.text();
  const contentType = response.headers.get("Content-Type") ?? "";
  const body: unknown = responseText
    ? contentType.includes("json")
      ? JSON.parse(responseText)
      : responseText
    : undefined;

  if (response.status === 401 && token) {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(TOKEN_EXPIRATION_KEY);
    window.dispatchEvent(new Event("library:unauthorized"));
  }

  if (!response.ok) {
    const problem = isRecord(body) ? body : {};
    const detail = typeof problem.detail === "string"
      ? problem.detail
      : "Não foi possível concluir a solicitação.";
    const code = typeof problem.code === "string" ? problem.code : undefined;
    const affectedMediaCount = typeof problem.affectedMediaCount === "number"
      ? problem.affectedMediaCount
      : undefined;
    throw new ApiError(detail, response.status, code, fieldErrors(problem.errors), affectedMediaCount);
  }

  return body;
}

export function postJson(path: string, payload: unknown): Promise<unknown> {
  return apiRequest(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
}
