function App() {
  return (
    <main className="page">
      <section className="welcome-card" aria-labelledby="welcome-title">
        <p className="eyebrow">SUA BIBLIOTECA, DO SEU JEITO</p>
        <h1 id="welcome-title">Tudo o que você ama em um só lugar.</h1>
        <p className="description">
          Organize livros, filmes e séries em listas pessoais. Esta é a base
          visual do projeto; os fluxos serão construídos nas próximas etapas.
        </p>
        <div className="media-types" aria-label="Tipos de mídia">
          <span>📚 Livros</span>
          <span>🎬 Filmes</span>
          <span>📺 Séries</span>
        </div>
      </section>
    </main>
  );
}

export default App;
