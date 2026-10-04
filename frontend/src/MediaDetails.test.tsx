import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

describe("cenario6_exibirDetalhesDaMidia", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("loads the complete media details through the provider endpoint", async () => {
    const user = userEvent.setup();
    sessionStorage.setItem("library.accessToken", "access-token");
    sessionStorage.setItem("library.tokenExpiresAt", String(Date.now() + 60_000));
    const media = {
      type: "BOOK",
      provider: "openlibrary",
      externalId: "OL893415W",
      title: "Duna",
      year: 1965,
      genre: "Ficção científica",
      synopsis: "Uma história em Arrakis.",
      coverUrl: "https://example.test/duna.jpg",
      externalRating: 8.4,
      inLibrary: false,
      entryId: null,
    };
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input) => {
      const url = new URL(String(input), "http://localhost");
      if (url.pathname === "/api/v1/media/suggestions") {
        return new Response("[]", { status: 200, headers: { "Content-Type": "application/json" } });
      }
      if (url.pathname === "/api/v1/media/search") {
        return new Response(JSON.stringify({
          content: [media],
          page: 0,
          size: 20,
          totalElements: 1,
          totalPages: 1,
        }), { status: 200, headers: { "Content-Type": "application/json" } });
      }
      if (url.pathname === "/api/v1/media/openlibrary/OL893415W") {
        return new Response(JSON.stringify(media), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      throw new Error(`Unexpected request: ${url.pathname}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await user.click(screen.getByRole("button", { name: "Pesquisa" }));
    await user.type(screen.getByLabelText("Título ou palavra-chave"), "Duna");
    await user.click(screen.getByRole("button", { name: "Pesquisar" }));
    await user.click(await screen.findByRole("button", { name: "Ver detalhes" }));

    expect(await screen.findByRole("dialog", { name: "Duna" }))
      .toHaveTextContent("Uma história em Arrakis.");
    const detailsCall = fetchMock.mock.calls.find(([input]) =>
      String(input).includes("/api/v1/media/openlibrary/OL893415W"),
    );
    expect(detailsCall?.[0]).toContain("type=BOOK");
  });
});
