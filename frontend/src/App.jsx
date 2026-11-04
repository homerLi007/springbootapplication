import { useState } from 'react';

export default function App() {
  const [input, setInput] = useState('');
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  async function handleDelete(e) {
    e.preventDefault();
    setError(null);
    setResults([]);
    const paths = input.split('\n').map(s => s.trim()).filter(Boolean);
    if (paths.length === 0) {
      setError('Enter at least one path');
      return;
    }
    setLoading(true);
    try {
      const res = await fetch('/api/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ paths })
      });
      if (!res.ok) throw new Error('HTTP ' + res.status);
      const data = await res.json();
      setResults(data.results || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="container">
      <h1>Safe Delete</h1>
      <p className="hint">One relative path per line (resolved under ~/safe-delete-root).</p>
      <form onSubmit={handleDelete}>
        <textarea
          value={input}
          onChange={e => setInput(e.target.value)}
          rows={6}
          placeholder="e.g. tmp/file.txt"
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Deleting...' : 'Delete'}
        </button>
      </form>
      {error && <div className="error">{error}</div>}
      {results.length > 0 && (
        <ul className="results">
          {results.map((r, i) => (
            <li key={i} className={r.deleted ? 'ok' : 'fail'}>
              <code>{r.path}</code> — {r.deleted ? 'deleted' : 'failed: ' + r.error}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
