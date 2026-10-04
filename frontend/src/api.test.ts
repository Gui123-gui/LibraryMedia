import { afterEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "./api";

describe("cenario3_sessaoAusenteOuExpirada", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("remove o token e notifica o app quando uma chamada autenticada recebe 401", async () => {
    sessionStorage.setItem("library.accessToken", "expired-token");
    sessionStorage.setItem("library.tokenExpiresAt", String(Date.now() - 1));
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      new Response(JSON.stringify({
        status: 401,
        code: "UNAUTHORIZED",
        detail: "Authentication is required",
      }), {
        status: 401,
        headers: { "Content-Type": "application/problem+json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);
    const unauthorized = vi.fn();
    window.addEventListener("library:unauthorized", unauthorized);

    await expect(apiRequest("/api/v1/users/me")).rejects.toMatchObject({
      status: 401,
      code: "UNAUTHORIZED",
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const meCall = fetchMock.mock.calls[0];
    if (!meCall) throw new Error("Expected the authenticated request.");
    const [path, request] = meCall;
    if (!request) throw new Error("Expected authenticated request options.");
    expect(path).toBe("/api/v1/users/me");
    expect(new Headers(request.headers).get("Authorization")).toBe("Bearer expired-token");
    expect(sessionStorage.getItem("library.accessToken")).toBeNull();
    expect(sessionStorage.getItem("library.tokenExpiresAt")).toBeNull();
    expect(unauthorized).toHaveBeenCalledTimes(1);
    window.removeEventListener("library:unauthorized", unauthorized);
  });
});
