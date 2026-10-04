import { useEffect, useState, type FormEvent } from "react";
import { ApiError, apiRequest, postJson } from "./api";

interface MediaList {
  id: number;
  name: string;
  favorites: boolean;
  mediaCount: number;
  covers: string[];
  shared: boolean;
}

interface ShareSettings {
  shared: boolean;
  url: string | null;
  token: string | null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isMediaList(value: unknown): value is MediaList {
  if (!isRecord(value)) return false;
  const list = value;
  return typeof list.id === "number"
    && typeof list.name === "string"
    && typeof list.favorites === "boolean"
    && typeof list.mediaCount === "number"
    && Array.isArray(list.covers)
    && list.covers.every((cover) => typeof cover === "string")
    && typeof list.shared === "boolean";
}

function isShareSettings(value: unknown): value is ShareSettings {
  return isRecord(value)
    && typeof value.shared === "boolean"
    && (typeof value.url === "string" || value.url === null)
    && (typeof value.token === "string" || value.token === null);
}

interface ListsManagerProps {
  initialCreate?: boolean;
}

function ListsManager({ initialCreate = false }: ListsManagerProps) {
  const [lists, setLists] = useState<MediaList[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [name, setName] = useState("");
  const [editingListId, setEditingListId] = useState<number | null>(null);
  const [editingName, setEditingName] = useState("");
  const [pendingDelete, setPendingDelete] = useState<{ list: MediaList; count: number } | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(initialCreate);
  const [sharingList, setSharingList] = useState<MediaList | null>(null);
  const [shareSettings, setShareSettings] = useState<ShareSettings | null>(null);
  const [isLoadingShare, setIsLoadingShare] = useState(false);

  async function loadLists() {
    setIsLoading(true);
    setError("");
    try {
      const result = await apiRequest("/api/v1/lists");
      if (!Array.isArray(result) || !result.every(isMediaList)) {
        throw new Error("A resposta de listas da API está fora do contrato.");
      }
      setLists(result);
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Não foi possível carregar suas listas.");
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    void loadLists();
  }, []);

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setError("");
    try {
      const created = await postJson("/api/v1/lists", { name });
      if (!isMediaList(created)) throw new Error("A resposta de criação está fora do contrato.");
      setLists((current) => [...current, created].sort((first, second) =>
        Number(second.favorites) - Number(first.favorites) || first.name.localeCompare(second.name),
      ));
      setName("");
      setShowCreateForm(false);
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Não foi possível criar a lista.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleRename(list: MediaList, event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setError("");
    try {
      const updated = await apiRequest(`/api/v1/lists/${list.id}`, {
        method: "PATCH",
        body: JSON.stringify({ name: editingName }),
      });
      if (!isMediaList(updated)) throw new Error("A resposta de atualização está fora do contrato.");
      setLists((current) => current.map((item) => item.id === list.id ? updated : item)
        .sort((first, second) =>
          Number(second.favorites) - Number(first.favorites) || first.name.localeCompare(second.name),
        ));
      setEditingListId(null);
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Não foi possível renomear a lista.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function deleteList(list: MediaList, confirmed = false) {
    setIsSubmitting(true);
    setError("");
    try {
      await apiRequest(`/api/v1/lists/${list.id}${confirmed ? "?confirm=true" : ""}`, {
        method: "DELETE",
      });
      setLists((current) => current.filter((item) => item.id !== list.id));
      setPendingDelete(null);
    } catch (caught: unknown) {
      if (
        !confirmed
        && caught instanceof ApiError
        && caught.code === "LIST_HAS_EXCLUSIVE_MEDIA"
        && typeof caught.affectedMediaCount === "number"
      ) {
        setPendingDelete({ list, count: caught.affectedMediaCount });
      } else if (caught instanceof Error) {
        setError(caught.message);
      } else {
        setError("Não foi possível excluir a lista.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  async function openSharing(list: MediaList) {
    setSharingList(list);
    setShareSettings(null);
    setError("");
    setIsLoadingShare(true);
    try {
      const result = await apiRequest(`/api/v1/lists/${list.id}/share`);
      if (!isShareSettings(result)) throw new Error("A resposta de compartilhamento está fora do contrato.");
      setShareSettings(result);
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Não foi possível carregar o compartilhamento.");
      setSharingList(null);
    } finally {
      setIsLoadingShare(false);
    }
  }

  async function updateSharing(shared: boolean) {
    if (!sharingList) return;
    setIsSubmitting(true);
    setError("");
    try {
      const result = await apiRequest(`/api/v1/lists/${sharingList.id}/share`, {
        method: "PUT",
        body: JSON.stringify({ active: shared }),
      });
      if (!isShareSettings(result)) throw new Error("A resposta de compartilhamento está fora do contrato.");
      setShareSettings(result);
      setLists((current) => current.map((list) =>
        list.id === sharingList.id ? { ...list, shared: result.shared } : list,
      ));
    } catch (caught: unknown) {
      setError(caught instanceof Error ? caught.message : "Não foi possível atualizar o compartilhamento.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function copyShareLink() {
    if (!shareSettings?.url) return;
    try {
      await navigator.clipboard.writeText(shareSettings.url);
    } catch {
      setError("Não foi possível copiar o link. Selecione e copie o endereço exibido.");
    }
  }

  return (
    <section className="lists-manager" aria-labelledby="lists-heading">
      <div className="lists-heading-row">
        <div>
          <p className="eyebrow">ORGANIZE SUAS HISTÓRIAS</p>
          <h2 id="lists-heading">Minhas listas</h2>
          <p className="description">Crie espaços para reunir suas mídias favoritas.</p>
        </div>
        <button
          className="primary-button create-list-toggle"
          onClick={() => setShowCreateForm((visible) => !visible)}
          type="button"
        >
          {showCreateForm ? "Cancelar" : "Nova lista"}
        </button>
      </div>

      {error && <p className="form-error" role="alert">{error}</p>}

      {showCreateForm && (
        <form className="list-form" onSubmit={handleCreate}>
          <label htmlFor="new-list-name">Nome da lista</label>
          <div className="list-form-row">
            <input
              id="new-list-name"
              maxLength={100}
              minLength={1}
              onChange={(event) => setName(event.target.value)}
              required
              value={name}
            />
            <button className="primary-button" disabled={isSubmitting} type="submit">
              Salvar lista
            </button>
          </div>
        </form>
      )}

      {isLoading ? (
        <p role="status">Carregando listas...</p>
      ) : lists.length === 0 ? (
        <p className="empty-state">Você ainda não tem listas.</p>
      ) : (
        <div className="list-grid">
          {lists.map((list) => (
            <article className="list-card" key={list.id}>
              <div className="list-covers" aria-hidden="true">
                {list.covers.length > 0
                  ? list.covers.map((cover, index) => (
                    <img alt="" key={`${cover}-${index}`} src={cover} />
                  ))
                  : <span className="cover-placeholder">P</span>}
              </div>
              <div className="list-card-content">
                {editingListId === list.id ? (
                  <form className="rename-form" onSubmit={(event) => void handleRename(list, event)}>
                    <label htmlFor={`rename-list-${list.id}`}>Novo nome</label>
                    <input
                      autoFocus
                      id={`rename-list-${list.id}`}
                      maxLength={100}
                      minLength={1}
                      onChange={(event) => setEditingName(event.target.value)}
                      required
                      value={editingName}
                    />
                    <div className="list-actions">
                      <button className="text-button" disabled={isSubmitting} type="submit">Salvar</button>
                      <button className="text-button" onClick={() => setEditingListId(null)} type="button">
                        Cancelar
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <div className="list-card-title">
                      <h3>{list.name}</h3>
                      {list.favorites && <span className="list-badge">Padrão</span>}
                    </div>
                    <p>{list.mediaCount} {list.mediaCount === 1 ? "mídia" : "mídias"}</p>
                    <div className="list-actions">
                      <button
                        className="text-button"
                        onClick={() => void openSharing(list)}
                        type="button"
                      >
                        Compartilhar
                      </button>
                      {!list.favorites && (
                        <>
                          <button
                            className="text-button"
                            onClick={() => {
                              setEditingListId(list.id);
                              setEditingName(list.name);
                            }}
                            type="button"
                          >
                            Renomear
                          </button>
                          <button
                            className="text-button destructive-button"
                            disabled={isSubmitting}
                            onClick={() => void deleteList(list)}
                            type="button"
                          >
                            Excluir
                          </button>
                        </>
                      )}
                      {list.shared && <span className="list-badge">Compartilhada</span>}
                    </div>
                  </>
                )}
              </div>
            </article>
          ))}
        </div>
      )}

      {pendingDelete && (
        <div className="dialog-backdrop">
          <section
            aria-labelledby="delete-list-heading"
            aria-modal="true"
            className="confirm-dialog"
            role="alertdialog"
          >
            <p className="eyebrow">AÇÃO DESTRUTIVA</p>
            <h3 id="delete-list-heading">Excluir “{pendingDelete.list.name}”?</h3>
            <p>
              {pendingDelete.count} {pendingDelete.count === 1 ? "mídia exclusiva será removida" : "mídias exclusivas serão removidas"} da sua biblioteca.
            </p>
            <div className="list-actions">
              <button
                className="primary-button"
                disabled={isSubmitting}
                onClick={() => void deleteList(pendingDelete.list, true)}
                type="button"
              >
                Confirmar exclusão
              </button>
              <button
                className="text-button"
                disabled={isSubmitting}
                onClick={() => setPendingDelete(null)}
                type="button"
              >
                Cancelar
              </button>
            </div>
          </section>
        </div>
      )}

      {sharingList && (
        <div className="dialog-backdrop">
          <section
            aria-labelledby="share-list-heading"
            aria-modal="true"
            className="confirm-dialog share-dialog"
            role="dialog"
          >
            <p className="eyebrow">LINK PÚBLICO · SOMENTE LEITURA</p>
            <h3 id="share-list-heading">Compartilhar “{sharingList.name}”</h3>
            {isLoadingShare ? <p role="status">Carregando compartilhamento...</p> : shareSettings ? (
              <>
                <p>
                  {shareSettings.shared
                    ? "Qualquer pessoa com o link pode ver as mídias desta lista, sem status ou notas."
                    : "Ative o link para permitir que visitantes vejam esta lista."}
                </p>
                {shareSettings.shared && shareSettings.url && (
                  <div className="share-link">
                    <a href={shareSettings.url}>{shareSettings.url}</a>
                    <button className="text-button" onClick={() => void copyShareLink()} type="button">
                      Copiar link
                    </button>
                  </div>
                )}
                <div className="list-actions">
                  <button
                    className="primary-button"
                    disabled={isSubmitting}
                    onClick={() => void updateSharing(!shareSettings.shared)}
                    type="button"
                  >
                    {shareSettings.shared ? "Desativar link" : "Ativar compartilhamento"}
                  </button>
                  <button
                    className="text-button"
                    disabled={isSubmitting}
                    onClick={() => {
                      setSharingList(null);
                      setShareSettings(null);
                    }}
                    type="button"
                  >
                    Fechar
                  </button>
                </div>
              </>
            ) : null}
          </section>
        </div>
      )}
    </section>
  );
}

export default ListsManager;
