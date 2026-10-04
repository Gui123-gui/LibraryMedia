import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "./api";
import App from "./App";

describe("cenario1_cadastroComFavoritos", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("envia os dados previstos e orienta a pessoa a entrar após o cadastro", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      new Response(JSON.stringify({ id: 17, name: "Maria Silva", email: "maria@example.com" }), {
        status: 201,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await user.click(screen.getByRole("button", { name: /criar conta/i }));
    await user.type(screen.getByLabelText(/nome/i), "Maria Silva");
    await user.type(screen.getByLabelText(/e-mail/i), "maria@example.com");
    await user.type(screen.getByLabelText(/^senha$/i), "senha-segura-123");
    await user.type(screen.getByLabelText(/confirme a senha/i), "senha-segura-123");
    await user.click(screen.getByRole("button", { name: /criar minha conta/i }));

    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1));
    const registrationCall = fetchMock.mock.calls[0];
    if (!registrationCall) throw new Error("Expected the registration request.");
    const [path, request] = registrationCall;
    if (!request) throw new Error("Expected registration request options.");
    expect(path).toBe("/api/v1/auth/register");
    expect(request.method).toBe("POST");
    expect(new Headers(request.headers).get("Content-Type")).toBe("application/json");
    expect(request.body).toBe(JSON.stringify({
      name: "Maria Silva",
      email: "maria@example.com",
      password: "senha-segura-123",
      passwordConfirmation: "senha-segura-123",
    }));
    expect(await screen.findByRole("heading", { name: /conta criada/i })).toBeInTheDocument();
    expect(screen.getByText(/lista favoritos/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /entrar/i })).toBeInTheDocument();
    expect(screen.queryByText("senha-segura-123")).not.toBeInTheDocument();
  });
});

describe("cenario2_loginAbreAreaPrivada", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("guarda o access token na sessão da aba e abre a área privada", async () => {
    const user = userEvent.setup();
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValue(
      new Response(JSON.stringify({
        accessToken: "access-token",
        tokenType: "Bearer",
        expiresIn: 3600,
      }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await user.type(screen.getByLabelText(/e-mail/i), "maria@example.com");
    await user.type(screen.getByLabelText(/^senha$/i), "senha-segura-123");
    await user.click(screen.getByRole("button", { name: /^entrar$/i }));

    expect(await screen.findByRole("navigation", { name: /navegação principal/i })).toBeInTheDocument();
    const loginCall = fetchMock.mock.calls.find(([path]) => path === "/api/v1/auth/login");
    if (!loginCall) throw new Error("Expected the login request.");
    const [path, request] = loginCall;
    if (!request) throw new Error("Expected login request options.");
    expect(path).toBe("/api/v1/auth/login");
    expect(request.method).toBe("POST");
    expect(request.body).toBe(JSON.stringify({
      email: "maria@example.com",
      password: "senha-segura-123",
    }));
    expect(sessionStorage.getItem("library.accessToken")).toBe("access-token");
  });
});

describe("cenario3_sessaoAusenteOuExpirada", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("mantém a área privada inacessível quando não há token de sessão", () => {
    render(<App />);

    expect(screen.getByRole("heading", { name: /sua biblioteca, do seu jeito/i })).toBeInTheDocument();
    expect(screen.queryByRole("navigation", { name: /navegação principal/i })).not.toBeInTheDocument();
  });

  it("encerra a área autenticada e limpa a sessão depois de receber 401", async () => {
    sessionStorage.setItem("library.accessToken", "expired-token");
    sessionStorage.setItem("library.tokenExpiresAt", String(Date.now() + 60_000));
    vi.stubGlobal("fetch", vi.fn<typeof fetch>().mockImplementation(async (input) => {
      const path = String(input);
      if (path === "/api/v1/users/me") {
        return new Response(JSON.stringify({
          status: 401,
          code: "UNAUTHORIZED",
          detail: "Authentication is required",
        }), {
          status: 401,
          headers: { "Content-Type": "application/problem+json" },
        });
      }
      const body = path === "/api/v1/lists" ? [] : {
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      };
      return new Response(JSON.stringify(body), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }));
    render(<App />);

    expect(screen.getByRole("navigation", { name: /navegação principal/i })).toBeInTheDocument();
    await expect(apiRequest("/api/v1/users/me")).rejects.toMatchObject({ code: "UNAUTHORIZED" });

    expect(await screen.findByRole("alert")).toHaveTextContent(/sessão expirou/i);
    expect(screen.queryByRole("navigation", { name: /navegação principal/i })).not.toBeInTheDocument();
    expect(sessionStorage.getItem("library.accessToken")).toBeNull();
  });
});

describe("cenario4_navegacaoPrincipal", () => {
  it("apresenta as seções principais na sessão autenticada", () => {
    sessionStorage.setItem("library.accessToken", "access-token");
    sessionStorage.setItem("library.tokenExpiresAt", String(Date.now() + 60_000));

    render(<App />);

    const navigation = screen.getByRole("navigation", { name: /navegação principal/i });
    expect(navigation).toHaveTextContent("Favoritos");
    expect(navigation).toHaveTextContent("Minhas listas");
    expect(navigation).toHaveTextContent("Pesquisa");
    expect(navigation).toHaveTextContent("Criar lista");
    expect(navigation).toHaveTextContent("Todas as mídias");
    expect(navigation).toHaveTextContent("Ranking");
  });
});

describe("cenario5_gerenciarListasComConfirmacao", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("cria, renomeia e só exclui uma lista exclusiva após confirmação explícita", async () => {
    const user = userEvent.setup();
    sessionStorage.setItem("library.accessToken", "access-token");
    sessionStorage.setItem("library.tokenExpiresAt", String(Date.now() + 60_000));

    const favorites = {
      id: 1,
      name: "Favoritos",
      favorites: true,
      mediaCount: 0,
      covers: [],
      shared: false,
    };
    const createdList = {
      id: 2,
      name: "Leituras",
      favorites: false,
      mediaCount: 0,
      covers: [],
      shared: false,
    };
    const renamedList = { ...createdList, name: "Livros" };
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (input, init) => {
      const url = String(input);
      const method = init?.method ?? "GET";
      if (url === "/api/v1/lists" && method === "GET") {
        return new Response(JSON.stringify([favorites]), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      if (url === "/api/v1/library" && method === "GET") {
        return new Response(JSON.stringify({
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      if (url === "/api/v1/lists" && method === "POST") {
        return new Response(JSON.stringify(createdList), {
          status: 201,
          headers: { "Content-Type": "application/json" },
        });
      }
      if (url === "/api/v1/lists/2" && method === "PATCH") {
        return new Response(JSON.stringify(renamedList), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      if (url === "/api/v1/lists/2" && method === "DELETE") {
        return new Response(JSON.stringify({
          type: "/errors/list-has-exclusive-media",
          title: "Confirmação necessária",
          status: 409,
          detail: "Uma mídia exclusiva será removida da sua biblioteca.",
          code: "LIST_HAS_EXCLUSIVE_MEDIA",
          affectedMediaCount: 1,
        }), {
          status: 409,
          headers: { "Content-Type": "application/problem+json" },
        });
      }
      if (url === "/api/v1/lists/2?confirm=true" && method === "DELETE") {
        return new Response(null, { status: 204 });
      }
      throw new Error(`Unexpected request: ${method} ${url}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await user.click(screen.getByRole("button", { name: "Minhas listas" }));
    expect(await screen.findByRole("heading", { name: "Minhas listas" })).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: "Favoritos" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Nova lista" }));
    await user.type(screen.getByLabelText("Nome da lista"), "Leituras");
    await user.click(screen.getByRole("button", { name: "Salvar lista" }));
    expect(await screen.findByRole("heading", { name: "Leituras" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Renomear" }));
    const renameInput = screen.getByLabelText("Novo nome");
    await user.clear(renameInput);
    await user.type(renameInput, "Livros");
    await user.click(screen.getByRole("button", { name: "Salvar" }));
    expect(await screen.findByRole("heading", { name: "Livros" })).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Excluir" }));
    const confirmation = await screen.findByRole("alertdialog");
    expect(confirmation).toHaveTextContent("1 mídia exclusiva será removida");
    expect(fetchMock.mock.calls.some(([path, request]) =>
      path === "/api/v1/lists/2" && request?.method === "DELETE",
    )).toBe(true);
    await user.click(screen.getByRole("button", { name: "Confirmar exclusão" }));

    await waitFor(() => expect(fetchMock.mock.calls.some(([path, request]) =>
      path === "/api/v1/lists/2?confirm=true" && request?.method === "DELETE",
    )).toBe(true));
    expect(await screen.findByRole("heading", { name: "Favoritos" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Livros" })).not.toBeInTheDocument();
  });
});

describe("cenario6_buscaFiltrosPaginacaoESemResultados", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it("debounces suggestions, searches with filters, paginates, and shows an empty state", async () => {
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
        return new Response(JSON.stringify([media]), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      if (url.pathname === "/api/v1/media/search") {
        const page = Number(url.searchParams.get("page"));
        return new Response(JSON.stringify(page === 0 ? {
          content: [media],
          page: 0,
          size: 20,
          totalElements: 21,
          totalPages: 2,
        } : {
          content: [],
          page: 1,
          size: 20,
          totalElements: 21,
          totalPages: 2,
        }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      throw new Error(`Unexpected request: ${url.pathname}`);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await user.click(screen.getByRole("button", { name: "Pesquisa" }));
    const queryInput = screen.getByLabelText("Título ou palavra-chave");
    await user.type(queryInput, "Duna");
    expect(await screen.findByRole("list", { name: "Sugestões" })).toBeInTheDocument();
    const suggestionsCall = fetchMock.mock.calls.find(([input]) =>
      String(input).includes("/api/v1/media/suggestions"),
    );
    expect(suggestionsCall?.[0]).toContain("q=Duna");
    expect(fetchMock.mock.calls.filter(([input]) =>
      String(input).includes("/api/v1/media/suggestions"),
    )).toHaveLength(1);

    await user.selectOptions(screen.getByLabelText("Tipo"), "BOOK");
    await user.type(screen.getByLabelText("Ano"), "1965");
    await user.type(screen.getByLabelText("Gênero"), "Ficção");
    await user.type(screen.getByLabelText("Nota externa mínima"), "7.5");
    await user.click(screen.getByRole("button", { name: "Pesquisar" }));

    expect(await screen.findByRole("heading", { name: "Duna" })).toBeInTheDocument();
    const searchCall = fetchMock.mock.calls.find(([input]) => {
      const url = new URL(String(input), "http://localhost");
      return url.pathname === "/api/v1/media/search" && url.searchParams.get("page") === "0";
    });
    expect(searchCall?.[0]).toContain("q=Duna");
    expect(searchCall?.[0]).toContain("type=BOOK");
    expect(searchCall?.[0]).toContain("year=1965");
    expect(searchCall?.[0]).toContain("genre=Fic%C3%A7%C3%A3o");
    expect(searchCall?.[0]).toContain("minRating=7.5");

    await user.click(screen.getByRole("button", { name: "Próxima" }));
    expect(await screen.findByRole("heading", { name: "Nenhuma história encontrada" })).toBeInTheDocument();
    expect(screen.getByText("Tente outro título ou ajuste os filtros para ampliar sua busca."))
      .toBeInTheDocument();
  });

  it("shows loading and provider errors instead of treating them as empty results", async () => {
    const user = userEvent.setup();
    sessionStorage.setItem("library.accessToken", "access-token");
    sessionStorage.setItem("library.tokenExpiresAt", String(Date.now() + 60_000));
    let resolveSearch: ((response: Response) => void) | undefined;
    const fetchMock = vi.fn<typeof fetch>().mockImplementation((input) => {
      const url = new URL(String(input), "http://localhost");
      if (url.pathname === "/api/v1/media/suggestions") {
        return Promise.resolve(new Response("[]", {
          status: 200,
          headers: { "Content-Type": "application/json" },
        }));
      }
      return new Promise<Response>((resolve) => {
        resolveSearch = resolve;
      });
    });

    vi.stubGlobal("fetch", fetchMock);

    render(<App />);
    await user.click(screen.getByRole("button", { name: "Pesquisa" }));
    await user.type(screen.getByLabelText("Título ou palavra-chave"), "Duna");
    await user.click(screen.getByRole("button", { name: "Pesquisar" }));
    expect(await screen.findByRole("status")).toHaveTextContent("Pesquisando mídias...");
    if (!resolveSearch) throw new Error("Expected the search request to be pending.");
    resolveSearch(new Response(JSON.stringify({
      status: 503,
      code: "EXTERNAL_PROVIDER_UNAVAILABLE",
      detail: "Os provedores estão temporariamente indisponíveis.",
    }), {
      status: 503,
      headers: { "Content-Type": "application/problem+json" },
    }));

    expect(await screen.findByRole("alert"))
      .toHaveTextContent("Os provedores estão temporariamente indisponíveis.");
    expect(screen.queryByRole("heading", { name: "Nenhuma história encontrada" }))
      .not.toBeInTheDocument();
  });
});
