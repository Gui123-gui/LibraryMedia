import { useEffect, useState, type FormEvent } from "react";
import { ApiError, apiRequest } from "./api";

type MediaType = "BOOK" | "MOVIE" | "SERIES";
type ConsumptionStatus = "WANT" | "IN_PROGRESS" | "DONE";

interface ListReference {
  id: number;
  name: string;
}

interface LibraryEntry {
  entryId: number;
  mediaId: number;
  provider: string;
  externalId: string;
  type: MediaType;
  title: string;
  releaseYear: number | null;
  genre: string | null;
  synopsis: string | null;
  coverUrl: string | null;
  externalRating: number | null;
  status: ConsumptionStatus;
  statusLabel: string;
  rating: number | null;
  lists: ListReference[];
}

interface MediaList {
  id: number;
  name: string;
  favorites: boolean;
  mediaCount: number;
  covers: string[];
  shared: boolean;
}

interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isList(value: unknown): value is MediaList {
  if (!isRecord(value)) return false;
  return typeof value.id === "number"
    && typeof value.name === "string"
    && typeof value.favorites === "boolean"
    && typeof value.mediaCount === "number"
    && Array.isArray(value.covers)
    && value.covers.every((cover) => typeof cover === "string")
    && typeof value.shared === "boolean";
}

function isLibraryEntry(value: unknown): value is LibraryEntry {
  if (!isRecord(value) || !Array.isArray(value.lists)) return false;
  return typeof value.entryId === "number"
    && typeof value.mediaId === "number"
    && typeof value.provider === "string"
    && typeof value.externalId === "string"
    && (value.type === "BOOK" || value.type === "MOVIE" || value.type === "SERIES")
    && typeof value.title === "string"
    && (typeof value.releaseYear === "number" || value.releaseYear === null)
    && (typeof value.genre === "string" || value.genre === null)
    && (typeof value.synopsis === "string" || value.synopsis === null)
    && (typeof value.coverUrl === "string" || value.coverUrl === null)
    && (typeof value.externalRating === "number" || value.externalRating === null)
    && (value.status === "WANT" || value.status === "IN_PROGRESS" || value.status === "DONE")
    && typeof value.statusLabel === "string"
    && (typeof value.rating === "number" || value.rating === null)
    && value.lists.every((item) =>
      isRecord(item) && typeof item.id === "number" && typeof item.name === "string",
    );
}

function isPageResult<T>(value: unknown, isItem: (item: unknown) => item is T): value is PageResult<T> {
  return isRecord(value)
    && Array.isArray(value.content)
    && value.content.every(isItem)
    && typeof value.page === "number"
    && typeof value.size === "number"
    && typeof value.totalElements === "number"
    && typeof value.totalPages === "number";
}

interface LibraryManagerProps {
  favoritesOnly?: boolean;
}

function LibraryManager({ favoritesOnly = false }: LibraryManagerProps) {
  const [entries, setEntries] = useState<LibraryEntry[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [lists, setLists] = useState<MediaList[]>([]);
  const [type, setType] = useState("");
  const [status, setStatus] = useState("");
  const [listId, setListId] = useState("");
  const [query, setQuery] = useState("");
  const [sort, setSort] = useState("createdAt,desc");
  const [isLoading, setIsLoading] = useState(true);
  const [busyEntryId, setBusyEntryId] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [pendingRemoval, setPendingRemoval] = useState<{
    entry: LibraryEntry;
    list?: ListReference;
  } | null>(null);

  useEffect(() => {
    let active = true;
    apiRequest("/api/v1/lists")
      .then((body) => {
        if (!Array.isArray(body) || !body.every(isList)) {
          throw new Error("A resposta de listas da API está fora do contrato.");
        }
        if (active) {
          setLists(body);
          if (favoritesOnly) {
            const favorites = body.find((list) => list.favorites);
            if (favorites) setListId(String(favorites.id));
          }
        }
      })
      .catch((caught: unknown) => {
        if (active) setError(caught instanceof Error ? caught.message : "Não foi possível carregar listas.");
      });
    return () => {
      active = false;
    };
  }, [favoritesOnly]);

  async function loadEntries(requestedPage = page) {
    setIsLoading(true);
    setError("");
    const params = new URLSearchParams({
      page: String(requestedPage),
      size: "20",
      sort,
    });
    if (type) params.set("type", type);
    if (status) params.set("status", status);
    if (listId) params.set("listId", listId);
    if (query.trim()) params.set("q", query.trim());
    try {
      const body = await apiRequest(`/api/v1/library?${params}`);
      if (!isPageResult(body, isLibraryEntry)) {
        throw new Error("A resposta da biblioteca está fora do contrato.");
      }
      setEntries(body.content);
      setPage(body.page);
      setTotalPages(body.totalPages);
      setTotalElements(body.totalElements);
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Não foi possível carregar a biblioteca.");
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    void loadEntries(0);
  }, [type, status, listId, query, sort]);

  async function updateStatus(entry: LibraryEntry, nextStatus: ConsumptionStatus) {
    setBusyEntryId(entry.entryId);
    setError("");
    try {
      const body = await apiRequest(`/api/v1/library/${entry.entryId}/status`, {
        method: "PUT",
        body: JSON.stringify({ status: nextStatus }),
      });
      if (!isLibraryEntry(body)) throw new Error("A resposta da biblioteca está fora do contrato.");
      setEntries((current) => current.map((item) => item.entryId === entry.entryId ? body : item));
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Não foi possível atualizar o status.");
    } finally {
      setBusyEntryId(null);
    }
  }

  async function updateRating(entry: LibraryEntry, event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const rating = Number(form.get("rating"));
    setBusyEntryId(entry.entryId);
    setError("");
    try {
      const body = await apiRequest(`/api/v1/library/${entry.entryId}/rating`, {
        method: "PUT",
        body: JSON.stringify({ rating }),
      });
      if (!isLibraryEntry(body)) throw new Error("A resposta da biblioteca está fora do contrato.");
      setEntries((current) => current.map((item) => item.entryId === entry.entryId ? body : item));
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Não foi possível salvar a nota.");
    } finally {
      setBusyEntryId(null);
    }
  }

  async function removeFromList(entry: LibraryEntry, list: ListReference, confirmed = false) {
    setBusyEntryId(entry.entryId);
    setError("");
    try {
      await apiRequest(
        `/api/v1/lists/${list.id}/items/${entry.entryId}${confirmed ? "?confirm=true" : ""}`,
        { method: "DELETE" },
      );
      setPendingRemoval(null);
      await loadEntries(page);
    } catch (caught: unknown) {
      if (
        !confirmed
        && caught instanceof ApiError
        && caught.code === "LAST_LIST_CONFIRMATION_REQUIRED"
      ) {
        setPendingRemoval({ entry, list });
      } else if (caught instanceof Error) {
        setError(caught.message);
      } else {
        setError("Não foi possível remover a mídia da lista.");
      }
    } finally {
      setBusyEntryId(null);
    }
  }

  async function removeFromLibrary(entry: LibraryEntry, confirmed = false) {
    setBusyEntryId(entry.entryId);
    setError("");
    try {
      await apiRequest(
        `/api/v1/library/${entry.entryId}${confirmed ? "?confirm=true" : ""}`,
        { method: "DELETE" },
      );
      setPendingRemoval(null);
      await loadEntries(page);
    } catch (caught: unknown) {
      if (
        !confirmed
        && caught instanceof ApiError
        && caught.code === "LIBRARY_REMOVAL_CONFIRMATION_REQUIRED"
      ) {
        setPendingRemoval({ entry });
      } else if (caught instanceof Error) {
        setError(caught.message);
      } else {
        setError("Não foi possível remover a mídia da biblioteca.");
      }
    } finally {
      setBusyEntryId(null);
    }
  }

  return (
    <section className="library-manager" aria-labelledby="library-heading">
      <p className="eyebrow">SUA COLEÇÃO PESSOAL</p>
      <h2 id="library-heading">{favoritesOnly ? "Favoritos" : "Todas as mídias"}</h2>
      <p className="description">Acompanhe o que está na sua biblioteca, seu andamento e suas notas.</p>

      <form
        className="library-filters"
        onSubmit={(event) => {
          event.preventDefault();
          setPage(0);
          void loadEntries(0);
        }}
      >
        <label>
          Buscar título
          <input
            maxLength={200}
            onChange={(event) => setQuery(event.target.value)}
            value={query}
          />
        </label>
        <label>
          Tipo
          <select onChange={(event) => setType(event.target.value)} value={type}>
            <option value="">Todos</option>
            <option value="BOOK">Livros</option>
            <option value="MOVIE">Filmes</option>
            <option value="SERIES">Séries</option>
          </select>
        </label>
        <label>
          Status
          <select onChange={(event) => setStatus(event.target.value)} value={status}>
            <option value="">Todos</option>
            <option value="WANT">Quero ler/assistir</option>
            <option value="IN_PROGRESS">Em andamento</option>
            <option value="DONE">Concluído</option>
          </select>
        </label>
        {!favoritesOnly && (
          <label>
            Lista
            <select onChange={(event) => setListId(event.target.value)} value={listId}>
              <option value="">Todas as listas</option>
              {lists.map((list) => <option key={list.id} value={list.id}>{list.name}</option>)}
            </select>
          </label>
        )}
        <label>
          Ordenar por
          <select onChange={(event) => setSort(event.target.value)} value={sort}>
            <option value="createdAt,desc">Mais recentes</option>
            <option value="title,asc">Título A–Z</option>
            <option value="rating,desc">Nota pessoal</option>
            <option value="releaseYear,desc">Ano</option>
          </select>
        </label>
      </form>

      {error && <p className="form-error" role="alert">{error}</p>}
      {isLoading ? (
        <p className="search-status" role="status">Carregando biblioteca...</p>
      ) : entries.length === 0 ? (
        <div className="no-results" role="status">
          <span className="no-results-icon" aria-hidden="true">⌕</span>
          <h3>Nenhuma mídia nesta seleção</h3>
          <p>Experimente outros filtros ou adicione uma mídia pela pesquisa.</p>
        </div>
      ) : (
        <>
          <p className="results-count">{totalElements} {totalElements === 1 ? "mídia" : "mídias"}</p>
          <div className="library-entry-list">
            {entries.map((entry) => (
              <article className="library-entry-card" key={entry.entryId}>
                {entry.coverUrl
                  ? <img className="library-entry-cover" alt="" src={entry.coverUrl} />
                  : <div className="library-entry-cover cover-placeholder" aria-hidden="true">P</div>}
                <div className="library-entry-content">
                  <div className="library-entry-heading">
                    <div>
                      <p className="eyebrow">{entry.type === "BOOK" ? "LIVRO" : entry.type === "MOVIE" ? "FILME" : "SÉRIE"}</p>
                      <h3>{entry.title}</h3>
                    </div>
                    <span className="list-badge">{entry.statusLabel}</span>
                  </div>
                  <p className="library-entry-meta">
                    {[entry.releaseYear, entry.genre].filter(Boolean).join(" · ") || "Detalhes indisponíveis"}
                  </p>
                  {entry.synopsis && <p className="library-synopsis">{entry.synopsis}</p>}
                  <div className="library-entry-memberships">
                    <span>Listas:</span>
                    {entry.lists.map((list) => (
                      <span className="list-badge" key={list.id}>{list.name}</span>
                    ))}
                  </div>
                  <div className="library-entry-actions">
                    <label>
                      Status da mídia
                      <select
                        disabled={busyEntryId === entry.entryId}
                        onChange={(event) => void updateStatus(entry, event.target.value as ConsumptionStatus)}
                        value={entry.status}
                      >
                        <option value="WANT">Quero ler/assistir</option>
                        <option value="IN_PROGRESS">Em andamento</option>
                        <option value="DONE">Concluído</option>
                      </select>
                    </label>
                    {entry.status === "DONE" && (
                      <form className="rating-form" onSubmit={(event) => void updateRating(entry, event)}>
                        <label>
                          Minha nota
                          <input
                            defaultValue={entry.rating ?? ""}
                            max="10"
                            min="1"
                            name="rating"
                            required
                            type="number"
                          />
                        </label>
                        <button className="text-button" disabled={busyEntryId === entry.entryId} type="submit">
                          Salvar nota
                        </button>
                      </form>
                    )}
                  </div>
                  <div className="list-actions">
                    {entry.lists.map((list) => (
                      <button
                        className="text-button destructive-button"
                        disabled={busyEntryId === entry.entryId}
                        key={`remove-${list.id}`}
                        onClick={() => void removeFromList(entry, list)}
                        type="button"
                      >
                        Remover de {list.name}
                      </button>
                    ))}
                    <button
                      className="text-button destructive-button"
                      disabled={busyEntryId === entry.entryId}
                      onClick={() => void removeFromLibrary(entry)}
                      type="button"
                    >
                      Remover da biblioteca
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>
          {totalPages > 1 && (
            <nav className="search-pagination" aria-label="Paginação da biblioteca">
              <button
                disabled={page === 0 || isLoading}
                onClick={() => void loadEntries(page - 1)}
                type="button"
              >
                Anterior
              </button>
              <span>Página {page + 1} de {totalPages}</span>
              <button
                disabled={page + 1 >= totalPages || isLoading}
                onClick={() => void loadEntries(page + 1)}
                type="button"
              >
                Próxima
              </button>
            </nav>
          )}
        </>
      )}

      {pendingRemoval && (
        <div className="dialog-backdrop">
          <section
            aria-labelledby="confirm-removal-heading"
            aria-modal="true"
            className="confirm-dialog"
            role="alertdialog"
          >
            <p className="eyebrow">AÇÃO DESTRUTIVA</p>
            <h3 id="confirm-removal-heading">
              {pendingRemoval.list
                ? `Remover “${pendingRemoval.entry.title}” da última lista?`
                : `Remover “${pendingRemoval.entry.title}” da biblioteca?`}
            </h3>
            <p>
              {pendingRemoval.list
                ? "A mídia e sua nota serão removidas da biblioteca."
                : "A mídia será removida de todas as listas e sua nota será apagada."}
            </p>
            <div className="list-actions">
              <button
                className="primary-button"
                disabled={busyEntryId === pendingRemoval.entry.entryId}
                onClick={() => pendingRemoval.list
                  ? void removeFromList(pendingRemoval.entry, pendingRemoval.list, true)
                  : void removeFromLibrary(pendingRemoval.entry, true)}
                type="button"
              >
                Confirmar remoção
              </button>
              <button
                className="text-button"
                disabled={busyEntryId === pendingRemoval.entry.entryId}
                onClick={() => setPendingRemoval(null)}
                type="button"
              >
                Cancelar
              </button>
            </div>
          </section>
        </div>
      )}
    </section>
  );
}

export default LibraryManager;
