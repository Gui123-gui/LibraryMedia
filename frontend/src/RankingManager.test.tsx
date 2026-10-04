import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

function setAuthenticatedSession() {
  sessionStorage.setItem("library.accessToken", "access-token");
  sessionStorage.setItem("library.tokenExpiresAt", String(Date.now() + 60_000));
}

function rankingEntry(position: number, title: string, rating: number) {
  return {
    position,
    entryId: position + 10,
    media: {
      type: "BOOK",
      title,
      year: 1965,
      coverUrl: null,
    },
    rating,
  };
}

describe("cenario9_consultarRankingPessoal", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("renders server positions and order and pages through the selected type", async () => {
    const user = userEvent.setup();
    setAuthenticatedSession();
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input) => {
      const url = new URL(String(input), "http://localhost");
      expect(url.pathname).toMatch(/^\/api\/v1\/rankings\//);
      const category = url.pathname.split("/").at(-1);
      const page = Number(url.searchParams.get("page"));
      if (category === "movies") {
        return new Response(JSON.stringify({
          content: [],
          page,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        }), { status: 200, headers: { "Content-Type": "application/json" } });
      }
      const content = page === 0
        ? [rankingEntry(1, "Duna", 10), rankingEntry(2, "O Hobbit", 9)]
        : [rankingEntry(21, "Fundação", 8)];
      return new Response(JSON.stringify({
        content,
        page,
        size: 20,
        totalElements: 21,
        totalPages: 2,
      }), { status: 200, headers: { "Content-Type": "application/json" } });
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await user.click(screen.getByRole("button", { name: "Ranking" }));
    expect(await screen.findByRole("heading", { name: "Duna" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "O Hobbit" })).toBeInTheDocument();
    const list = screen.getByRole("list");
    const titles = Array.from(list.querySelectorAll("h3"), (heading) => heading.textContent);
    expect(titles).toEqual(["Duna", "O Hobbit"]);
    expect(screen.getByLabelText("Nota 10 de 10")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Próxima" }));
    expect(await screen.findByRole("heading", { name: "Fundação" })).toBeInTheDocument();
    expect(screen.getByText("21")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Filmes" }));
    expect(await screen.findByRole("heading", { name: "Ainda sem notas neste ranking" }))
      .toBeInTheDocument();
    await waitFor(() => expect(fetchMock.mock.calls.some(([input]) =>
      String(input).includes("/api/v1/rankings/movies"),
    )).toBe(true));
  });
});
