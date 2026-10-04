import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

interface Entry {
  entryId: number;
  mediaId: number;
  provider: string;
  externalId: string;
  type: "BOOK" | "MOVIE" | "SERIES";
  title: string;
  releaseYear: number | null;
  genre: string | null;
  synopsis: string | null;
  coverUrl: string | null;
  externalRating: number | null;
  status: "WANT" | "IN_PROGRESS" | "DONE";
  statusLabel: string;
  rating: number | null;
  lists: Array<{ id: number; name: string }>;
}

function setAuthenticatedSession() {
  sessionStorage.setItem("library.accessToken", "access-token");
  sessionStorage.setItem("library.tokenExpiresAt", String(Date.now() + 60_000));
}

function pageResponse(entries: Entry[]) {
  return {
    content: entries,
    page: 0,
    size: 20,
    totalElements: entries.length,
    totalPages: entries.length ? 1 : 0,
  };
}

function libraryEntry(): Entry {
  return {
    entryId: 77,
    mediaId: 19,
    provider: "tmdb",
    externalId: "movie-77",
    type: "MOVIE",
    title: "Filme da biblioteca",
    releaseYear: 2024,
    genre: "Drama",
    synopsis: "Uma história.",
    coverUrl: null,
    externalRating: 8.2,
    status: "DONE",
    statusLabel: "ASSISTIDO",
    rating: 8,
    lists: [{ id: 1, name: "Favoritos" }],
  };
}

function jsonResponse(value: unknown, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("cenario8_atualizarBibliotecaEConfirmarRemocao", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("filters entries, updates status and rating, and confirms removing the last list", async () => {
    const user = userEvent.setup();
    setAuthenticatedSession();
    let entry = libraryEntry();
    let membershipDeleteConfirmed = false;
    const libraryUrls: string[] = [];
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input, init) => {
      const url = new URL(String(input), "http://localhost");
      const method = init?.method ?? "GET";
      if (url.pathname === "/api/v1/lists" && method === "GET") {
        return jsonResponse([
          { id: 1, name: "Favoritos", favorites: true, mediaCount: 1, covers: [], shared: false },
        ]);
      }
      if (url.pathname === "/api/v1/library" && method === "GET") {
        libraryUrls.push(url.toString());
        return jsonResponse(pageResponse(membershipDeleteConfirmed ? [] : [entry]));
      }
      if (url.pathname === "/api/v1/library/77/status" && method === "PUT") {
        entry = { ...entry, status: "WANT", statusLabel: "QUERO_ASSISTIR" };
        return jsonResponse(entry);
      }
      if (url.pathname === "/api/v1/library/77/rating" && method === "PUT") {
        entry = { ...entry, rating: 9 };
        return jsonResponse(entry);
      }
      if (url.pathname === "/api/v1/lists/1/items/77" && method === "DELETE") {
        if (url.searchParams.get("confirm") === "true") {
          membershipDeleteConfirmed = true;
          return jsonResponse({ entryId: 77, removedFromLibrary: true });
        }
        return jsonResponse({
          status: 409,
          code: "LAST_LIST_CONFIRMATION_REQUIRED",
          detail: "Confirme para remover da sua biblioteca.",
        }, 409);
      }
      throw new Error(`Unexpected request: ${method} ${url.pathname}${url.search}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await user.click(screen.getByRole("button", { name: "Todas as mídias" }));
    expect(await screen.findByRole("heading", { name: "Filme da biblioteca" })).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText("Tipo"), "MOVIE");
    await user.selectOptions(screen.getByLabelText("Status"), "DONE");
    await user.type(screen.getByLabelText("Buscar título"), "biblioteca");
    await waitFor(() => expect(libraryUrls.some((url) => {
      const parsed = new URL(url);
      return parsed.searchParams.get("type") === "MOVIE"
        && parsed.searchParams.get("status") === "DONE"
        && parsed.searchParams.get("q") === "biblioteca";
    })).toBe(true));

    await user.clear(screen.getByLabelText("Minha nota"));
    await user.type(screen.getByLabelText("Minha nota"), "9");
    await user.click(screen.getByRole("button", { name: "Salvar nota" }));
    await waitFor(() => expect(entry.rating).toBe(9));
    await user.selectOptions(screen.getByLabelText("Status da mídia"), "WANT");
    await waitFor(() => expect(entry.status).toBe("WANT"));
    expect(entry.rating).toBe(9);

    await user.click(screen.getByRole("button", { name: "Remover de Favoritos" }));
    expect(await screen.findByRole("alertdialog")).toHaveTextContent(/última lista/i);
    await user.click(screen.getByRole("button", { name: "Confirmar remoção" }));
    await waitFor(() => expect(membershipDeleteConfirmed).toBe(true));
    expect(await screen.findByRole("heading", { name: "Nenhuma mídia nesta seleção" }))
      .toBeInTheDocument();
  });

  it("confirms direct removal from the library and sends confirm=true", async () => {
    const user = userEvent.setup();
    setAuthenticatedSession();
    let removed = false;
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input, init) => {
      const url = new URL(String(input), "http://localhost");
      const method = init?.method ?? "GET";
      if (url.pathname === "/api/v1/lists") {
        return jsonResponse([
          { id: 1, name: "Favoritos", favorites: true, mediaCount: 1, covers: [], shared: false },
        ]);
      }
      if (url.pathname === "/api/v1/library" && method === "GET") {
        return jsonResponse(pageResponse(removed ? [] : [libraryEntry()]));
      }
      if (url.pathname === "/api/v1/library/77" && method === "DELETE") {
        if (url.searchParams.get("confirm") === "true") {
          removed = true;
          return new Response(null, { status: 204 });
        }
        return jsonResponse({
          status: 409,
          code: "LIBRARY_REMOVAL_CONFIRMATION_REQUIRED",
          detail: "Confirme a remoção.",
        }, 409);
      }
      throw new Error(`Unexpected request: ${method} ${url.pathname}${url.search}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await user.click(screen.getByRole("button", { name: "Todas as mídias" }));
    expect(await screen.findByRole("heading", { name: "Filme da biblioteca" })).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Remover da biblioteca" }));
    expect(await screen.findByRole("alertdialog")).toHaveTextContent(/nota será apagada/i);
    await user.click(screen.getByRole("button", { name: "Confirmar remoção" }));
    expect(await screen.findByRole("heading", { name: "Nenhuma mídia nesta seleção" }))
      .toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([input]) =>
      String(input) === "/api/v1/library/77?confirm=true",
    )).toBe(true);
  });
});
