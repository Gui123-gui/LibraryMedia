import { useEffect, useState, type FormEvent } from "react";
import { apiRequest } from "./api";
import type { components } from "./generated/api-schema";

type MediaType = "BOOK" | "MOVIE" | "SERIES";
type OpenApiMediaSummary = components["schemas"]["MediaSummary"];

type MediaSummary = Omit<
  OpenApiMediaSummary,
  "type" | "year" | "genre" | "synopsis" | "coverUrl" | "externalRating" | "entryId"
> & {
  type: MediaType;
  provider: NonNullable<OpenApiMediaSummary["provider"]>;
  externalId: NonNullable<OpenApiMediaSummary["externalId"]>;
  title: NonNullable<OpenApiMediaSummary["title"]>;
  year: number | null;
  genre: string | null;
  synopsis: string | null;
  coverUrl: string | null;
  externalRating: number | null;
  inLibrary: NonNullable<OpenApiMediaSummary["inLibrary"]>;
  entryId: number | null;
};

type OpenApiSearchResult = components["schemas"]["SearchResult"];
type SearchResult = Omit<
  OpenApiSearchResult,
  "content" | "page" | "size" | "totalElements" | "totalPages"
> & {
  content: MediaSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

interface MediaList {
  id: number;
  name: string;
  favorites: boolean;
  mediaCount: number;
  covers: string[];
  shared: boolean;
}

interface AddToLibraryResponse {
  entryId: number;
  alreadyInLibrary: boolean;
  addedToLists: number[];
  alreadyInLists: number[];
  status: string;
  rating: number | null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isMediaSummary(value: unknown): value is MediaSummary {
  if (!isRecord(value)) return false;
  return (value.type === "BOOK" || value.type === "MOVIE" || value.type === "SERIES")
    && typeof value.provider === "string"
    && typeof value.externalId === "string"
    && typeof value.title === "string"
    && (typeof value.year === "number" || value.year === null)
    && (typeof value.genre === "string" || value.genre === null)
    && (typeof value.synopsis === "string" || value.synopsis === null)
    && (typeof value.coverUrl === "string" || value.coverUrl === null)
    && (typeof value.externalRating === "number" || value.externalRating === null)
    && typeof value.inLibrary === "boolean"
    && (typeof value.entryId === "number" || value.entryId === null);
}

function isSearchResult(value: unknown): value is SearchResult {
  if (!isRecord(value) || !Array.isArray(value.content)) return false;
  return value.content.every(isMediaSummary)
    && typeof value.page === "number"
    && typeof value.size === "number"
    && typeof value.totalElements === "number"
    && typeof value.totalPages === "number";
}

function mediaTypeLabel(type: MediaType): string {
  return type === "BOOK" ? "Livro" : type === "MOVIE" ? "Filme" : "Série";
}

function isMediaList(value: unknown): value is MediaList {
  if (!isRecord(value)) return false;
  return typeof value.id === "number"
    && typeof value.name === "string"
    && typeof value.favorites === "boolean"
    && typeof value.mediaCount === "number"
    && Array.isArray(value.covers)
    && value.covers.every((cover) => typeof cover === "string")
    && typeof value.shared === "boolean";
}

function isAddToLibraryResponse(value: unknown): value is AddToLibraryResponse {
  if (!isRecord(value)) return false;
  return typeof value.entryId === "number"
    && typeof value.alreadyInLibrary === "boolean"
    && Array.isArray(value.addedToLists)
    && value.addedToLists.every((id) => typeof id === "number")
    && Array.isArray(value.alreadyInLists)
    && value.alreadyInLists.every((id) => typeof id === "number")
    && typeof value.status === "string"
    && (typeof value.rating === "number" || value.rating === null);
}

interface AddMediaDialogProps {
  media: MediaSummary;
  onClose: () => void;
  onAdded: (entryId: number) => void;
}

interface MediaDetailsDialogProps {
  media: MediaSummary;
  onClose: () => void;
}

function MediaDetailsDialog({ media, onClose }: MediaDetailsDialogProps) {
  const [details, setDetails] = useState<MediaSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    const params = new URLSearchParams({ type: media.type });
    apiRequest(
      `/api/v1/media/${encodeURIComponent(media.provider)}/${encodeURIComponent(media.externalId)}?${params}`,
    )
      .then((body) => {
        if (!isMediaSummary(body)) throw new Error("Os detalhes da mídia estão fora do contrato.");
        if (active) setDetails(body);
      })
      .catch((caught: unknown) => {
        if (active) setError(caught instanceof Error ? caught.message : "Não foi possível carregar os detalhes.");
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [media.externalId, media.provider, media.type]);

  return (
    <div className="dialog-backdrop">
      <section
        aria-labelledby="media-details-heading"
        aria-modal="true"
        className="confirm-dialog media-details-dialog"
        role="dialog"
      >
        <p className="eyebrow">DETALHES DA MÍDIA</p>
        <h3 id="media-details-heading">{media.title}</h3>
        {isLoading ? <p role="status">Carregando detalhes...</p> : error ? (
          <p className="form-error" role="alert">{error}</p>
        ) : details ? (
          <>
            {details.coverUrl && <img alt="" className="details-cover" src={details.coverUrl} />}
            <p>
              {[mediaTypeLabel(details.type), details.year, details.genre, details.externalRating !== null
                ? `Nota externa ${details.externalRating}`
                : null].filter(Boolean).join(" · ")}
            </p>
            <p>{details.synopsis || "Sinopse indisponível para esta mídia."}</p>
          </>
        ) : null}
        <button className="primary-button" onClick={onClose} type="button">Fechar</button>
      </section>
    </div>
  );
}

function AddMediaDialog({ media, onClose, onAdded }: AddMediaDialogProps) {
  const [lists, setLists] = useState<MediaList[]>([]);
  const [selectedListIds, setSelectedListIds] = useState<number[]>([]);
  const [hasConsumed, setHasConsumed] = useState(false);
  const [rating, setRating] = useState("");
  const [isLoadingLists, setIsLoadingLists] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    let active = true;
    apiRequest("/api/v1/lists")
      .then((body) => {
        if (!Array.isArray(body) || !body.every(isMediaList)) {
          throw new Error("A resposta de listas da API está fora do contrato.");
        }
        if (active) setLists(body);
      })
      .catch((caught: unknown) => {
        if (active) {
          setError(caught instanceof Error ? caught.message : "Não foi possível carregar suas listas.");
        }
      })
      .finally(() => {
        if (active) setIsLoadingLists(false);
      });
    return () => {
      active = false;
    };
  }, []);

  function toggleList(listId: number) {
    setSelectedListIds((current) => current.includes(listId)
      ? current.filter((id) => id !== listId)
      : [...current, listId]);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");
    if (selectedListIds.length === 0) {
      setError("Selecione pelo menos uma lista.");
      return;
    }
    if (hasConsumed && !rating) {
      setError("Informe uma nota de 1 a 10 para adicionar como concluída.");
      return;
    }

    setIsSubmitting(true);
    try {
      const payload: {
        provider: string;
        externalId: string;
        listIds: number[];
        consumed: boolean;
        rating?: number;
      } = {
        provider: media.provider,
        externalId: media.externalId,
        listIds: selectedListIds,
        consumed: hasConsumed,
      };
      if (hasConsumed) payload.rating = Number(rating);
      const body = await apiRequest("/api/v1/library", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      if (!isAddToLibraryResponse(body)) {
        throw new Error("A resposta de adição à biblioteca está fora do contrato.");
      }
      onAdded(body.entryId);
      const addedCount = body.addedToLists.length;
      const existingCount = body.alreadyInLists.length;
      setSuccess(body.alreadyInLibrary
        ? `Mídia já estava na biblioteca. ${addedCount} lista(s) adicionada(s); ${existingCount} já continha(m) a mídia.`
        : "Mídia adicionada à sua biblioteca.");
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Não foi possível adicionar a mídia.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="dialog-backdrop">
      <section
        aria-labelledby="add-media-heading"
        aria-modal="true"
        className="confirm-dialog add-media-dialog"
        role="dialog"
      >
        <p className="eyebrow">ADICIONE À SUA BIBLIOTECA</p>
        <h3 id="add-media-heading">{media.title}</h3>
        <p>{mediaTypeLabel(media.type)}{media.year ? ` · ${media.year}` : ""}</p>
        {error && <p className="form-error" role="alert">{error}</p>}
        {success ? (
          <>
            <p className="success-message" role="status">{success}</p>
            <button className="primary-button" onClick={onClose} type="button">Concluir</button>
          </>
        ) : (
          <form className="add-media-form" onSubmit={(event) => void handleSubmit(event)}>
            <fieldset disabled={isLoadingLists || isSubmitting}>
              <legend>Em quais listas deseja guardar?</legend>
              {isLoadingLists ? (
                <p role="status">Carregando listas...</p>
              ) : lists.length === 0 ? (
                <p>Crie uma lista antes de adicionar mídias.</p>
              ) : (
                <div className="add-media-lists">
                  {lists.map((list) => (
                    <label key={list.id}>
                      <input
                        checked={selectedListIds.includes(list.id)}
                        onChange={() => toggleList(list.id)}
                        type="checkbox"
                        value={list.id}
                      />
                      <span>{list.name}</span>
                    </label>
                  ))}
                </div>
              )}
            </fieldset>
            <fieldset disabled={isLoadingLists || isSubmitting || lists.length === 0}>
              <legend>Já leu ou assistiu?</legend>
              <label className="choice-option">
                <input
                  checked={!hasConsumed}
                  onChange={() => setHasConsumed(false)}
                  name="consumed"
                  type="radio"
                />
                <span>Não, quero {media.type === "BOOK" ? "ler" : "assistir"}</span>
              </label>
              <label className="choice-option">
                <input
                  checked={hasConsumed}
                  onChange={() => setHasConsumed(true)}
                  name="consumed"
                  type="radio"
                />
                <span>Sim, já concluí</span>
              </label>
              {hasConsumed && (
                <label className="rating-field">
                  Sua nota (1 a 10)
                  <input
                    max="10"
                    min="1"
                    onChange={(event) => setRating(event.target.value)}
                    required
                    type="number"
                    value={rating}
                  />
                </label>
              )}
            </fieldset>
            <div className="list-actions">
              <button
                className="primary-button"
                disabled={isLoadingLists || isSubmitting || lists.length === 0}
                type="submit"
              >
                {isSubmitting ? "Adicionando..." : "Adicionar mídia"}
              </button>
              <button className="text-button" onClick={onClose} type="button">Cancelar</button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
}

function MediaSearch() {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState<MediaSummary[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [suggestionError, setSuggestionError] = useState("");
  const [type, setType] = useState("");
  const [year, setYear] = useState("");
  const [genre, setGenre] = useState("");
  const [minRating, setMinRating] = useState("");
  const [result, setResult] = useState<SearchResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [mediaToAdd, setMediaToAdd] = useState<MediaSummary | null>(null);
  const [mediaToInspect, setMediaToInspect] = useState<MediaSummary | null>(null);

  useEffect(() => {
    const normalizedQuery = query.trim();
    if (normalizedQuery.length < 2) {
      setSuggestions([]);
      setShowSuggestions(false);
      setSuggestionError("");
      return undefined;
    }

    const controller = new AbortController();
    const timeout = window.setTimeout(() => {
      const params = new URLSearchParams({ q: normalizedQuery });
      apiRequest(`/api/v1/media/suggestions?${params}`, { signal: controller.signal })
        .then((body) => {
          if (!Array.isArray(body) || !body.every(isMediaSummary)) {
            throw new Error("A resposta de sugestões da API está fora do contrato.");
          }
          setSuggestions(body.slice(0, 8));
          setSuggestionError("");
        })
        .catch((caught: unknown) => {
          if (!controller.signal.aborted) {
            setSuggestionError(caught instanceof Error
              ? caught.message
              : "Não foi possível carregar sugestões.");
          }
        });
    }, 300);

    return () => {
      window.clearTimeout(timeout);
      controller.abort();
    };
  }, [query]);

  async function search(event?: FormEvent<HTMLFormElement>, requestedPage = 0, selectedQuery = query) {
    event?.preventDefault();
    const normalizedQuery = selectedQuery.trim();
    if (normalizedQuery.length < 2) {
      setError("Digite pelo menos 2 caracteres para pesquisar.");
      setResult(null);
      return;
    }

    const params = new URLSearchParams({
      q: normalizedQuery,
      page: String(requestedPage),
      size: "20",
    });
    if (type) params.set("type", type);
    if (year) params.set("year", year);
    if (genre.trim()) params.set("genre", genre.trim());
    if (minRating) params.set("minRating", minRating);

    setIsLoading(true);
    setError("");
    setResult(null);
    setShowSuggestions(false);
    try {
      const body = await apiRequest(`/api/v1/media/search?${params}`);
      if (!isSearchResult(body)) {
        throw new Error("A resposta de pesquisa da API está fora do contrato.");
      }
      setResult(body);
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Não foi possível pesquisar mídias.");
    } finally {
      setIsLoading(false);
    }
  }

  function chooseSuggestion(suggestion: MediaSummary) {
    setQuery(suggestion.title);
    setSuggestions([]);
    setShowSuggestions(false);
    void search(undefined, 0, suggestion.title);
  }

  return (
    <section className="media-search" aria-labelledby="search-heading">
      <p className="eyebrow">ENCONTRE SUA PRÓXIMA HISTÓRIA</p>
      <h2 id="search-heading">Pesquisa</h2>
      <p className="description">Busque livros, filmes e séries para descobrir onde guardar.</p>

      <form className="search-form" onSubmit={(event) => void search(event)}>
        <div className="search-query">
          <label htmlFor="media-query">Título ou palavra-chave</label>
          <input
            autoComplete="off"
            id="media-query"
            maxLength={100}
            minLength={2}
            onBlur={() => window.setTimeout(() => setShowSuggestions(false), 120)}
            onChange={(event) => {
              setQuery(event.target.value);
              setShowSuggestions(true);
            }}
            onFocus={() => setShowSuggestions(true)}
            value={query}
          />
          {showSuggestions && suggestions.length > 0 && (
            <ul className="suggestion-list" aria-label="Sugestões">
              {suggestions.map((suggestion) => (
                <li key={`${suggestion.provider}-${suggestion.externalId}`}>
                  <button onClick={() => chooseSuggestion(suggestion)} type="button">
                    <span>{suggestion.title}</span>
                    <span className="suggestion-meta">
                      {mediaTypeLabel(suggestion.type)}
                      {suggestion.year ? ` · ${suggestion.year}` : ""}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="search-filters">
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
            Ano
            <input
              max="9999"
              min="1"
              onChange={(event) => setYear(event.target.value)}
              type="number"
              value={year}
            />
          </label>
          <label>
            Gênero
            <input
              maxLength={100}
              onChange={(event) => setGenre(event.target.value)}
              value={genre}
            />
          </label>
          <label>
            Nota externa mínima
            <input
              max="10"
              min="0"
              onChange={(event) => setMinRating(event.target.value)}
              step="0.1"
              type="number"
              value={minRating}
            />
          </label>
          <button className="primary-button search-button" disabled={isLoading} type="submit">
            {isLoading ? "Pesquisando..." : "Pesquisar"}
          </button>
        </div>
      </form>

      {suggestionError && <p className="form-error" role="alert">{suggestionError}</p>}
      {error && <p className="form-error" role="alert">{error}</p>}
      {isLoading && <p className="search-status" role="status">Pesquisando mídias...</p>}

      {result && (
        <section className="search-results" aria-label="Resultados da pesquisa">
          {result.content.length === 0 ? (
            <div className="no-results" role="status">
              <span className="no-results-icon" aria-hidden="true">⌕</span>
              <h3>Nenhuma história encontrada</h3>
              <p>Tente outro título ou ajuste os filtros para ampliar sua busca.</p>
            </div>
          ) : (
            <>
              <p className="results-count">
                {result.totalElements} {result.totalElements === 1 ? "resultado" : "resultados"}
              </p>
              <div className="media-result-grid">
                {result.content.map((media) => (
                  <article
                    className="media-result-card"
                    key={`${media.provider}-${media.externalId}`}
                  >
                    {media.coverUrl
                      ? <img alt="" className="media-result-cover" src={media.coverUrl} />
                      : <div className="media-result-cover cover-placeholder" aria-hidden="true">P</div>}
                    <div className="media-result-content">
                      <p className="eyebrow">{mediaTypeLabel(media.type)}</p>
                      <h3>{media.title}</h3>
                      <p>
                        {[media.year, media.genre].filter(Boolean).join(" · ") || "Detalhes indisponíveis"}
                      </p>
                      {media.externalRating !== null && (
                        <span className="media-rating">Nota externa {media.externalRating}</span>
                      )}
                      <button
                        className="text-button add-media-button"
                        onClick={() => setMediaToInspect(media)}
                        type="button"
                      >
                        Ver detalhes
                      </button>
                      {media.inLibrary && <span className="list-badge">Já está na biblioteca</span>}
                      <button
                        className="text-button add-media-button"
                        onClick={() => setMediaToAdd(media)}
                        type="button"
                      >
                        {media.inLibrary ? "Adicionar a outras listas" : "Adicionar à biblioteca"}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
              {result.totalPages > 1 && (
                <nav className="search-pagination" aria-label="Paginação dos resultados">
                  <button
                    disabled={isLoading || result.page === 0}
                    onClick={() => void search(undefined, result.page - 1)}
                    type="button"
                  >
                    Anterior
                  </button>
                  <span>Página {result.page + 1} de {result.totalPages}</span>
                  <button
                    disabled={isLoading || result.page + 1 >= result.totalPages}
                    onClick={() => void search(undefined, result.page + 1)}
                    type="button"
                  >
                    Próxima
                  </button>
                </nav>
              )}
            </>
          )}
        </section>
      )}
      {mediaToAdd && (
        <AddMediaDialog
          media={mediaToAdd}
          onAdded={(entryId) => {
            setResult((current) => current ? {
              ...current,
              content: current.content.map((item) =>
                item.provider === mediaToAdd.provider && item.externalId === mediaToAdd.externalId
                  ? { ...item, inLibrary: true, entryId }
                  : item,
              ),
            } : current);
          }}
          onClose={() => setMediaToAdd(null)}
        />
      )}
      {mediaToInspect && (
        <MediaDetailsDialog media={mediaToInspect} onClose={() => setMediaToInspect(null)} />
      )}
    </section>
  );
}

export default MediaSearch;
