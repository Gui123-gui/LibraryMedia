import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import ListsManager from "./ListsManager";

describe("cenario10_gerenciarLinkCompartilhado", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("ativa e desativa o link público mantendo o token retornado pela API", async () => {
    const user = userEvent.setup();
    sessionStorage.setItem("library.accessToken", "access-token");
    const list = {
      id: 4,
      name: "Leituras",
      favorites: false,
      mediaCount: 2,
      covers: [],
      shared: false,
    };
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input, init) => {
      const url = String(input);
      const method = init?.method ?? "GET";
      if (url === "/api/v1/lists" && method === "GET") {
        return new Response(JSON.stringify([list]), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      if (url === "/api/v1/lists/4/share" && method === "GET") {
        return new Response(JSON.stringify({ shared: false, url: null, token: null }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      if (url === "/api/v1/lists/4/share" && method === "PUT") {
        const active = JSON.parse(String(init?.body)).active as boolean;
        return new Response(JSON.stringify({
          shared: active,
          url: active ? "http://localhost:3000/shared/secure-token" : "http://localhost:3000/shared/secure-token",
          token: "secure-token",
        }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      throw new Error(`Unexpected request: ${method} ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<ListsManager />);
    await user.click(await screen.findByRole("button", { name: "Compartilhar" }));
    await user.click(await screen.findByRole("button", { name: "Ativar compartilhamento" }));

    const shareLink = await screen.findByRole("link", { name: "http://localhost:3000/shared/secure-token" });
    expect(shareLink).toHaveAttribute("href", "http://localhost:3000/shared/secure-token");
    expect(await screen.findByText("Compartilhada")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Desativar link" }));
    await waitFor(() => expect(screen.queryByRole("link", {
      name: "http://localhost:3000/shared/secure-token",
    })).not.toBeInTheDocument());
    expect(fetchMock.mock.calls.filter(([, request]) => request?.method === "PUT")).toHaveLength(2);
  });
});
