import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { apiRequest } from "./api";
import AttributionNotice from "./AttributionNotice";

interface PublicMedia {
  title: string;
  type: "BOOK" | "MOVIE" | "SERIES";
  year: number | null;
  genre: string | null;
  synopsis: string | null;
  coverUrl: string | null;
}

interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface SharedList {
  name: string;
  mediaCount: number;
  items: PageResult<PublicMedia>;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isPublicMedia(value: unknown): value is PublicMedia {
  return isRecord(value)
    && typeof value.title === "string"
    && (value.type === "BOOK" || value.type === "MOVIE" || value.type === "SERIES")
    && (typeof value.year === "number" || value.year === null)
    && (typeof value.genre === "string" || value.genre === null)
    && (typeof value.synopsis === "string" || value.synopsis === null)
    && (typeof value.coverUrl === "string" || value.coverUrl === null)
    && !("rating" in value)
    && !("status" in value);
}

function isSharedList(value: unknown): value is SharedList {
  if (!isRecord(value) || !isRecord(value.items)) return false;
  const items = value.items;
  return typeof value.name === "string"
    && typeof value.mediaCount === "number"
    && Array.isArray(items.content)
    && items.content.every(isPublicMedia)
    && typeof items.page === "number"
    && typeof items.size === "number"
    && typeof items.totalElements === "number"
    && typeof items.totalPages === "number";
}

function SharedListPage() {
  const { token } = useParams();
  const [list, setList] = useState<SharedList | null>(null);
  const [page, setPage] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    setError("");
    apiRequest(`/api/v1/public/lists/${encodeURIComponent(token ?? "")}?page=${page}&size=20`)
      .then((body) => {
        if (!isSharedList(body)) throw new Error("A lista pública está fora do contrato.");
        if (active) setList(body);
      })
      .catch((caught: unknown) => {
        if (active) setError(caught instanceof Error ? caught.message : "Não foi possível carregar esta lista.");
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });
    return () => {
      active = false;
    };
  }, [page, token]);

  return (
    <div className="app-shell">
      <header className="app-header">
        <a className="brand app-brand" href="/" aria-label="Personal Media Library">
          <span className="brand-mark" aria-hidden="true">P</span>
          <span>Personal Media Library</span>
        </a>
        <Link className="text-button" to="/">Minha biblioteca</Link>
      </header>
      <main className="workspace shared-list-page">
        <p className="eyebrow">LISTA COMPARTILHADA · SOMENTE LEITURA</p>
        {isLoading ? <p role="status">Carregando lista...</p> : error ? (
          <div className="no-results" role="alert">
            <h1>Lista indisponível</h1>
            <p>{error}</p>
          </div>
        ) : list ? (
          <>
            <h1>{list.name}</h1>
            <p className="description">
              {list.mediaCount} {list.mediaCount === 1 ? "mídia" : "mídias"} nesta lista.
            </p>
            {list.items.content.length === 0 ? (
              <p className="empty-state">Esta lista ainda não tem mídias.</p>
            ) : (
              <div className="library-entry-list">
                {list.items.content.map((media, index) => (
                  <article
                    className="library-entry-card"
                    key={`${media.type}-${media.title}-${media.year}-${index}`}
                  >
                    {media.coverUrl
                      ? <img className="library-entry-cover" alt="" src={media.coverUrl} />
                      : <div className="library-entry-cover cover-placeholder" aria-hidden="true">P</div>}
                    <div className="library-entry-content">
                      <p className="eyebrow">
                        {media.type === "BOOK" ? "LIVRO" : media.type === "MOVIE" ? "FILME" : "SÉRIE"}
                      </p>
                      <h2>{media.title}</h2>
                      <p className="library-entry-meta">
                        {[media.year, media.genre].filter(Boolean).join(" · ") || "Detalhes indisponíveis"}
                      </p>
                      {media.synopsis && <p className="library-synopsis">{media.synopsis}</p>}
                    </div>
                  </article>
                ))}
              </div>
            )}
            {list.items.totalPages > 1 && (
              <nav className="search-pagination" aria-label="Paginação da lista compartilhada">
                <button disabled={page === 0} onClick={() => setPage((current) => current - 1)} type="button">
                  Anterior
                </button>
                <span>Página {page + 1} de {list.items.totalPages}</span>
                <button
                  disabled={page + 1 >= list.items.totalPages}
                  onClick={() => setPage((current) => current + 1)}
                  type="button"
                >
                  Próxima
                </button>
              </nav>
            )}
          </>
        ) : null}
      </main>
      <AttributionNotice />
    </div>
  );
}

export default SharedListPage;
