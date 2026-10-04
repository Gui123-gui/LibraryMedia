import { useEffect, useState } from "react";
import { apiRequest } from "./api";

type RankingType = "books" | "movies" | "series";

interface RankingMedia {
  type: "BOOK" | "MOVIE" | "SERIES";
  title: string;
  year: number | null;
  coverUrl: string | null;
}

interface RankingEntry {
  position: number;
  entryId: number;
  media: RankingMedia;
  rating: number;
}

interface RankingPage {
  content: RankingEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isRankingEntry(value: unknown): value is RankingEntry {
  if (!isRecord(value) || !isRecord(value.media)) return false;
  return typeof value.position === "number"
    && typeof value.entryId === "number"
    && (value.media.type === "BOOK" || value.media.type === "MOVIE" || value.media.type === "SERIES")
    && typeof value.media.title === "string"
    && (typeof value.media.year === "number" || value.media.year === null)
    && (typeof value.media.coverUrl === "string" || value.media.coverUrl === null)
    && typeof value.rating === "number";
}

function isRankingPage(value: unknown): value is RankingPage {
  return isRecord(value)
    && Array.isArray(value.content)
    && value.content.every(isRankingEntry)
    && typeof value.page === "number"
    && typeof value.size === "number"
    && typeof value.totalElements === "number"
    && typeof value.totalPages === "number";
}

const RANKING_TYPES: Array<{ value: RankingType; label: string }> = [
  { value: "books", label: "Livros" },
  { value: "movies", label: "Filmes" },
  { value: "series", label: "Séries" },
];

function RankingManager() {
  const [type, setType] = useState<RankingType>("books");
  const [page, setPage] = useState<RankingPage | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setIsLoading(true);
    setError("");
    apiRequest(`/api/v1/rankings/${type}?page=${currentPage}&size=20`, {
      signal: controller.signal,
    })
      .then((body) => {
        if (!isRankingPage(body)) throw new Error("A resposta do ranking está fora do contrato.");
        setPage(body);
      })
      .catch((caught: unknown) => {
        if (!controller.signal.aborted) {
          setError(caught instanceof Error ? caught.message : "Não foi possível carregar o ranking.");
          setPage(null);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false);
      });
    return () => controller.abort();
  }, [type, currentPage]);

  return (
    <section className="ranking-manager" aria-labelledby="ranking-heading">
      <p className="eyebrow">SUAS OBRAS CONCLUÍDAS</p>
      <h2 id="ranking-heading">Ranking pessoal</h2>
      <p className="description">As melhores notas aparecem primeiro; empates seguem a ordem do servidor.</p>
      <div className="ranking-type-tabs" role="group" aria-label="Tipo de ranking">
        {RANKING_TYPES.map((item) => (
          <button
            aria-pressed={type === item.value}
            className={type === item.value ? "nav-item active" : "nav-item"}
            key={item.value}
            onClick={() => {
              setType(item.value);
              setCurrentPage(0);
            }}
            type="button"
          >
            {item.label}
          </button>
        ))}
      </div>

      {error && <p className="form-error" role="alert">{error}</p>}
      {isLoading ? (
        <p className="search-status" role="status">Carregando ranking...</p>
      ) : page && page.content.length === 0 ? (
        <div className="no-results" role="status">
          <span className="no-results-icon" aria-hidden="true">☆</span>
          <h3>Ainda sem notas neste ranking</h3>
          <p>Conclua uma mídia e dê sua nota para ela aparecer aqui.</p>
        </div>
      ) : page ? (
        <>
          <p className="results-count">{page.totalElements} {page.totalElements === 1 ? "obra" : "obras"}</p>
          <ol className="ranking-list">
            {page.content.map((entry) => (
              <li className="ranking-card" key={entry.entryId}>
                <span className="ranking-position">{entry.position}</span>
                {entry.media.coverUrl
                  ? <img alt="" className="ranking-cover" src={entry.media.coverUrl} />
                  : <div className="ranking-cover cover-placeholder" aria-hidden="true">P</div>}
                <div className="ranking-media">
                  <p className="eyebrow">
                    {entry.media.type === "BOOK" ? "LIVRO" : entry.media.type === "MOVIE" ? "FILME" : "SÉRIE"}
                  </p>
                  <h3>{entry.media.title}</h3>
                  {entry.media.year && <p>{entry.media.year}</p>}
                </div>
                <span className="personal-rating" aria-label={`Nota ${entry.rating} de 10`}>
                  {entry.rating}<small>/10</small>
                </span>
              </li>
            ))}
          </ol>
          {page.totalPages > 1 && (
            <nav className="search-pagination" aria-label="Paginação do ranking">
              <button
                disabled={page.page === 0 || isLoading}
                onClick={() => setCurrentPage((current) => current - 1)}
                type="button"
              >
                Anterior
              </button>
              <span>Página {page.page + 1} de {page.totalPages}</span>
              <button
                disabled={page.page + 1 >= page.totalPages || isLoading}
                onClick={() => setCurrentPage((current) => current + 1)}
                type="button"
              >
                Próxima
              </button>
            </nav>
          )}
        </>
      ) : null}
    </section>
  );
}

export default RankingManager;
