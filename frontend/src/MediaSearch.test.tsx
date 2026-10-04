import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

interface TestMedia {
  type: string;
  provider: string;
  externalId: string;
  title: string;
  year: number | null;
  genre: string | null;
  synopsis: string | null;
  coverUrl: string | null;
  externalRating: number | null;
  inLibrary: boolean;
  entryId: number | null;
}

const book: TestMedia = {
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

function authenticatedSession() {
  sessionStorage.setItem("library.accessToken", "access-token");
  sessionStorage.setItem("library.tokenExpiresAt", String(Date.now() + 60_000));
}

function searchResponse(media: TestMedia) {
  return new Response(JSON.stringify({
    content: [media],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  }), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  });
}

describe("cenario7_adicionarMidiaALista", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("selects lists, asks if consumed, requires a rating, and submits a completed entry", async () => {
    const user = userEvent.setup();
    authenticatedSession();
    const lists = [
      { id: 1, name: "Favoritos", favorites: true, mediaCount: 0, covers: [], shared: false },
      { id: 2, name: "Ficção", favorites: false, mediaCount: 0, covers: [], shared: false },
    ];
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input, init) => {
      const url = new URL(String(input), "http://localhost");
      if (url.pathname === "/api/v1/media/suggestions") {
        return new Response("[]", {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      if (url.pathname === "/api/v1/media/search") return searchResponse(book);
      if (url.pathname === "/api/v1/lists" && (init?.method ?? "GET") === "GET") {
        return new Response(JSON.stringify(lists), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      if (url.pathname === "/api/v1/library" && init?.method === "POST") {
        return new Response(JSON.stringify({
          entryId: 45,
          alreadyInLibrary: false,
          addedToLists: [1, 2],
          alreadyInLists: [],
          status: "DONE",
          rating: 9,
        }), {
          status: 201,
          headers: { "Content-Type": "application/json" },
        });
      }
      throw new Error(`Unexpected request: ${init?.method ?? "GET"} ${url.pathname}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await user.click(screen.getByRole("button", { name: "Pesquisa" }));
    await user.type(screen.getByLabelText("Título ou palavra-chave"), "Duna");
    await user.click(screen.getByRole("button", { name: "Pesquisar" }));
    await user.click(await screen.findByRole("button", { name: "Adicionar à biblioteca" }));

    expect(await screen.findByRole("dialog", { name: "Duna" })).toBeInTheDocument();
    await user.click(await screen.findByLabelText("Favoritos"));
    await user.click(screen.getByLabelText("Ficção"));
    await user.click(screen.getByLabelText("Sim, já concluí"));

    const ratingInput = screen.getByLabelText("Sua nota (1 a 10)");
    expect(ratingInput).toBeRequired();
    await user.click(screen.getByRole("button", { name: "Adicionar mídia" }));
    expect(fetchMock.mock.calls.some(([input, init]) =>
      new URL(String(input), "http://localhost").pathname === "/api/v1/library"
      && init?.method === "POST",
    )).toBe(false);
    await user.type(ratingInput, "9");
    await user.click(screen.getByRole("button", { name: "Adicionar mídia" }));

    expect(await screen.findByRole("status"))
      .toHaveTextContent("Mídia adicionada à sua biblioteca.");
    const addCall = fetchMock.mock.calls.find(([input, init]) =>
      new URL(String(input), "http://localhost").pathname === "/api/v1/library"
      && init?.method === "POST",
    );
    expect(addCall?.[1]?.body).toBe(JSON.stringify({
      provider: "openlibrary",
      externalId: "OL893415W",
      listIds: [1, 2],
      consumed: true,
      rating: 9,
    }));
    expect(await screen.findByText("Já está na biblioteca")).toBeInTheDocument();
  });

  it("adds an unconsumed media without sending a rating", async () => {
    const user = userEvent.setup();
    authenticatedSession();
    const movie = {
      ...book,
      type: "MOVIE",
      provider: "tmdb",
      externalId: "movie-42",
      title: "Filme de teste",
      year: null,
      genre: null,
      synopsis: null,
      coverUrl: null,
      externalRating: null,
    };
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input, init) => {
      const url = new URL(String(input), "http://localhost");
      if (url.pathname === "/api/v1/media/suggestions") {
        return new Response("[]", { status: 200, headers: { "Content-Type": "application/json" } });
      }
      if (url.pathname === "/api/v1/media/search") return searchResponse(movie);
      if (url.pathname === "/api/v1/lists") {
        return new Response(JSON.stringify([
          { id: 3, name: "Favoritos", favorites: true, mediaCount: 0, covers: [], shared: false },
        ]), { status: 200, headers: { "Content-Type": "application/json" } });
      }
      if (url.pathname === "/api/v1/library" && init?.method === "POST") {
        return new Response(JSON.stringify({
          entryId: 46,
          alreadyInLibrary: false,
          addedToLists: [3],
          alreadyInLists: [],
          status: "WANT",
          rating: null,
        }), { status: 201, headers: { "Content-Type": "application/json" } });
      }
      throw new Error(`Unexpected request: ${url.pathname}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await user.click(screen.getByRole("button", { name: "Pesquisa" }));
    await user.type(screen.getByLabelText("Título ou palavra-chave"), "Filme");
    await user.click(screen.getByRole("button", { name: "Pesquisar" }));
    await user.click(await screen.findByRole("button", { name: "Adicionar à biblioteca" }));
    await user.click(await screen.findByLabelText("Favoritos"));
    await user.click(screen.getByRole("button", { name: "Adicionar mídia" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Mídia adicionada à sua biblioteca.");
    const addCall = fetchMock.mock.calls.find(([input, init]) =>
      new URL(String(input), "http://localhost").pathname === "/api/v1/library"
      && init?.method === "POST",
    );
    expect(addCall?.[1]?.body).toBe(JSON.stringify({
      provider: "tmdb",
      externalId: "movie-42",
      listIds: [3],
      consumed: false,
    }));
  });
});
