import { useEffect, useState, type FormEvent } from "react";
import { BrowserRouter, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { ACCESS_TOKEN_KEY, ApiError, postJson, TOKEN_EXPIRATION_KEY } from "./api";
import AttributionNotice from "./AttributionNotice";
import ListsManager from "./ListsManager";
import MediaSearch from "./MediaSearch";
import LibraryManager from "./LibraryManager";
import RankingManager from "./RankingManager";
import SharedListPage from "./SharedListPage";

type AuthView = "login" | "register" | "registered" | "home";
type LibrarySection = "Favoritos" | "Minhas listas" | "Pesquisa" | "Criar lista" | "Todas as mídias" | "Ranking";

const LIBRARY_SECTIONS: LibrarySection[] = [
  "Favoritos",
  "Minhas listas",
  "Pesquisa",
  "Criar lista",
  "Todas as mídias",
  "Ranking",
];

interface RegisterResponse {
  id: number;
  name: string;
  email: string;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

async function registerAccount(payload: {
  name: string;
  email: string;
  password: string;
  passwordConfirmation: string;
}): Promise<RegisterResponse> {
  const body = await postJson("/api/v1/auth/register", payload);
  if (
    !isRecord(body) ||
    typeof body.id !== "number" ||
    typeof body.name !== "string" ||
    typeof body.email !== "string"
  ) {
    throw new Error("A resposta de cadastro da API está fora do contrato.");
  }

  return { id: body.id, name: body.name, email: body.email };
}

interface LoginResponse {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
}

async function loginAccount(payload: { email: string; password: string }): Promise<LoginResponse> {
  const body = await postJson("/api/v1/auth/login", payload);
  if (
    !isRecord(body) ||
    typeof body.accessToken !== "string" ||
    body.tokenType !== "Bearer" ||
    typeof body.expiresIn !== "number" ||
    body.expiresIn <= 0
  ) {
    throw new Error("A resposta de login da API está fora do contrato.");
  }

  return {
    accessToken: body.accessToken,
    tokenType: "Bearer",
    expiresIn: body.expiresIn,
  };
}

function hasValidSession(): boolean {
  const token = sessionStorage.getItem(ACCESS_TOKEN_KEY);
  const expiresAt = Number(sessionStorage.getItem(TOKEN_EXPIRATION_KEY));
  if (!token || !Number.isFinite(expiresAt) || expiresAt <= Date.now()) {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(TOKEN_EXPIRATION_KEY);
    return false;
  }
  return true;
}

const SECTION_PATHS: Record<LibrarySection, string> = {
  Favoritos: "/",
  "Minhas listas": "/lists",
  Pesquisa: "/search",
  "Criar lista": "/lists/new",
  "Todas as mídias": "/library",
  Ranking: "/rankings",
};

function sectionForPath(path: string): LibrarySection {
  const match = Object.entries(SECTION_PATHS).find(([, route]) => route === path);
  return match ? match[0] as LibrarySection : "Favoritos";
}

function AppContent() {
  const location = useLocation();
  const navigate = useNavigate();
  const [view, setView] = useState<AuthView>(() => hasValidSession() ? "home" : "login");
  const [registeredEmail, setRegisteredEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (view !== "home") return undefined;
    const expiresAt = Number(sessionStorage.getItem(TOKEN_EXPIRATION_KEY));
    const timeout = window.setTimeout(() => {
      sessionStorage.removeItem(ACCESS_TOKEN_KEY);
      sessionStorage.removeItem(TOKEN_EXPIRATION_KEY);
      setView("login");
      setError("Sua sessão expirou. Entre novamente para continuar.");
    }, Math.max(0, expiresAt - Date.now()));

    function handleUnauthorized() {
      sessionStorage.removeItem(ACCESS_TOKEN_KEY);
      sessionStorage.removeItem(TOKEN_EXPIRATION_KEY);
      setView("login");
      setError("Sua sessão expirou. Entre novamente para continuar.");
    }

    window.addEventListener("library:unauthorized", handleUnauthorized);
    return () => {
      window.clearTimeout(timeout);
      window.removeEventListener("library:unauthorized", handleUnauthorized);
    };
  }, [view]);

  async function handleRegister(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const name = String(form.get("name") ?? "").trim();
    const email = String(form.get("email") ?? "").trim();
    const password = String(form.get("password") ?? "");
    const passwordConfirmation = String(form.get("passwordConfirmation") ?? "");

    setError("");
    setFieldErrors({});
    if (password !== passwordConfirmation) {
      setFieldErrors({ passwordConfirmation: "A confirmação deve ser igual à senha." });
      return;
    }

    setIsSubmitting(true);
    try {
      const account = await registerAccount({ name, email, password, passwordConfirmation });
      setRegisteredEmail(account.email);
      setView("registered");
    } catch (caught: unknown) {
      if (caught instanceof ApiError) {
        setError(caught.message);
        setFieldErrors(caught.fieldErrors);
      } else if (caught instanceof Error) {
        setError(caught.message);
      } else {
        setError("Ocorreu um erro inesperado ao criar sua conta.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleLogin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "").trim();
    const password = String(form.get("password") ?? "");

    setError("");
    setFieldErrors({});
    setIsSubmitting(true);
    try {
      const result = await loginAccount({ email, password });
      sessionStorage.setItem(ACCESS_TOKEN_KEY, result.accessToken);
      sessionStorage.setItem(
        TOKEN_EXPIRATION_KEY,
        String(Date.now() + result.expiresIn * 1000),
      );
      setView("home");
      navigate("/");
    } catch (caught: unknown) {
      if (caught instanceof ApiError) {
        setError(caught.message);
        setFieldErrors(caught.fieldErrors);
      } else if (caught instanceof Error) {
        setError(caught.message);
      } else {
        setError("Ocorreu um erro inesperado ao entrar.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  function logOut() {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(TOKEN_EXPIRATION_KEY);
    setView("login");
    navigate("/");
  }

  if (view === "home") {
    const activeSection = sectionForPath(location.pathname);
    return (
      <div className="app-shell">
        <header className="app-header">
          <a className="brand app-brand" href="/" aria-label="Personal Media Library">
            <span className="brand-mark" aria-hidden="true">P</span>
            <span>Personal Media Library</span>
          </a>
          <button className="text-button logout-button" onClick={logOut} type="button">
            Sair
          </button>
        </header>
        <nav className="main-navigation" aria-label="Navegação principal">
          {LIBRARY_SECTIONS.map((section) => (
            <button
              aria-current={activeSection === section ? "page" : undefined}
              className={activeSection === section ? "nav-item active" : "nav-item"}
              key={section}
              onClick={() => navigate(SECTION_PATHS[section])}
              type="button"
            >
              {section}
            </button>
          ))}
        </nav>
        <main className="workspace">
          {activeSection === "Minhas listas" || activeSection === "Criar lista" ? (
            <ListsManager
              initialCreate={activeSection === "Criar lista"}
              key={activeSection}
            />
          ) : activeSection === "Pesquisa" ? (
            <MediaSearch />
          ) : activeSection === "Favoritos" ? (
            <LibraryManager favoritesOnly />
          ) : activeSection === "Todas as mídias" ? (
            <LibraryManager />
          ) : activeSection === "Ranking" ? (
            <RankingManager />
          ) : (
            <>
              <p className="eyebrow">SUA BIBLIOTECA, DO SEU JEITO</p>
              <h1>{activeSection}</h1>
              <p className="description">
                {activeSection === "Favoritos"
                  ? "Sua lista Favoritos está pronta para receber as histórias que você quer guardar."
                  : "Esta área será conectada à sua biblioteca nesta próxima tarefa."}
              </p>
            </>
          )}
        </main>
        <AttributionNotice />
      </div>
    );
  }

  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="auth-title">
        <a className="brand" href="/" aria-label="Personal Media Library">
          <span className="brand-mark" aria-hidden="true">P</span>
          <span>Personal Media Library</span>
        </a>

        {view === "registered" ? (
          <div className="success-panel" role="status">
            <span className="success-icon" aria-hidden="true">✓</span>
            <p className="eyebrow">TUDO PRONTO</p>
            <h1 id="auth-title">Conta criada</h1>
            <p>
              Sua lista Favoritos já está pronta. Entre com <strong>{registeredEmail}</strong> para
              começar a organizar sua biblioteca.
            </p>
            <button className="primary-button" onClick={() => setView("login")} type="button">
              Entrar
            </button>
          </div>
        ) : (
          <>
            <p className="eyebrow">{view === "register" ? "UM LUGAR PARA CADA HISTÓRIA" : "BEM-VINDA DE VOLTA"}</p>
            <h1 id="auth-title">
              {view === "register" ? "Crie sua conta" : "Sua biblioteca, do seu jeito."}
            </h1>
            <p className="description">
              {view === "register"
                ? "Guarde livros, filmes e séries que fazem parte da sua história."
                : "Entre para continuar cuidando das histórias que você ama."}
            </p>

            <div className="media-types" aria-label="Tipos de mídia">
              <span>📚 Livros</span>
              <span>🎬 Filmes</span>
              <span>📺 Séries</span>
            </div>

            {error && <p className="form-error" role="alert">{error}</p>}

            <form
              className="auth-form"
              onSubmit={view === "register" ? handleRegister : handleLogin}
            >
              {view === "register" && (
                <label>
                  Nome
                  <input autoComplete="name" maxLength={100} minLength={1} name="name" required />
                  {fieldErrors.name && <span className="field-error">{fieldErrors.name}</span>}
                </label>
              )}
              <label>
                E-mail
                <input
                  autoComplete="email"
                  defaultValue={registeredEmail}
                  maxLength={255}
                  name="email"
                  required
                  type="email"
                />
                {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
              </label>
              <label>
                Senha
                <input
                  autoComplete={view === "register" ? "new-password" : "current-password"}
                  maxLength={72}
                  minLength={8}
                  name="password"
                  required
                  type="password"
                />
                {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
              </label>
              {view === "register" && (
                <label>
                  Confirme a senha
                  <input
                    autoComplete="new-password"
                    maxLength={72}
                    minLength={8}
                    name="passwordConfirmation"
                    required
                    type="password"
                  />
                  {fieldErrors.passwordConfirmation && (
                    <span className="field-error">{fieldErrors.passwordConfirmation}</span>
                  )}
                </label>
              )}
              <button className="primary-button" disabled={isSubmitting} type="submit">
                {isSubmitting ? "Enviando..." : view === "register" ? "Criar minha conta" : "Entrar"}
              </button>
            </form>

            <p className="auth-switch">
              {view === "register" ? "Já tem uma conta?" : "Ainda não tem uma conta?"}{" "}
              <button
                className="text-button"
                onClick={() => {
                  setError("");
                  setFieldErrors({});
                  setView(view === "register" ? "login" : "register");
                }}
                type="button"
              >
                {view === "register" ? "Entrar" : "Criar conta"}
              </button>
            </p>
          </>
        )}
      </section>
    </main>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/shared/:token" element={<SharedListPage />} />
        <Route path="*" element={<AppContent />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
