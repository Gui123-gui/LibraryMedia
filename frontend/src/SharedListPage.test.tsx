import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

describe("cenario11_visualizarListaPublicaSomenteLeitura", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/");
    sessionStorage.clear();
  });

  it("mostra a lista paginada publicamente sem pedir autenticação nem exibir dados privados", async () => {
    window.history.replaceState({}, "", "/shared/public-token");
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(new Response(JSON.stringify({
      name: "Histórias para assistir",
      mediaCount: 21,
      items: {
        content: [{
          title: "A Test Movie",
          type: "MOVIE",
          year: 2025,
          genre: "Drama",
          synopsis: "Uma sinopse pública.",
          coverUrl: null,
        }],
        page: 0,
        size: 20,
        totalElements: 21,
        totalPages: 2,
      },
    }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    }));
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);

    expect(await screen.findByRole("heading", { name: "Histórias para assistir" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "A Test Movie" })).toBeInTheDocument();
    expect(screen.getByText("Uma sinopse pública.")).toBeInTheDocument();
    expect(screen.getByRole("navigation", { name: "Paginação da lista compartilhada" })).toBeInTheDocument();
    expect(screen.queryByLabelText(/e-mail/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/nota|status do dono/i)).not.toBeInTheDocument();
    expect(screen.getByText(/not endorsed or certified by TMDB/i)).toBeInTheDocument();
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    expect(fetchMock.mock.calls[0]?.[0]).toBe("/api/v1/public/lists/public-token?page=0&size=20");
    expect(sessionStorage.getItem("library.accessToken")).toBeNull();
  });
});

describe("cenario12_exibirEstadosEAtribuicao", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/");
    sessionStorage.clear();
  });

  it("exibe carregamento, erro de API e a atribuição TMDB", async () => {
    window.history.replaceState({}, "", "/shared/unavailable-token");
    let resolveList: ((response: Response) => void) | undefined;
    vi.stubGlobal("fetch", vi.fn<typeof fetch>().mockImplementation(() => new Promise((resolve) => {
      resolveList = resolve;
    })));

    render(<App />);
    expect(screen.getByRole("status")).toHaveTextContent("Carregando lista...");
    expect(screen.getByText(/not endorsed or certified by TMDB/i)).toBeInTheDocument();
    if (!resolveList) throw new Error("Expected the public list request to be pending.");
    resolveList(new Response(JSON.stringify({
      status: 503,
      code: "EXTERNAL_PROVIDER_UNAVAILABLE",
      detail: "A lista não pôde ser carregada.",
    }), {
      status: 503,
      headers: { "Content-Type": "application/problem+json" },
    }));

    expect(await screen.findByRole("alert")).toHaveTextContent("A lista não pôde ser carregada.");
  });
});
