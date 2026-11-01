import { useState } from 'react';

export default function App() {
  const [paths, setPaths] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setResult(null);
    setLoading(true);
    try {
      const list = paths
        .split('\n')
        .map((p) => p.trim())
        .filter((p) => p.length > 0);
      const res = await fetch('/api/delete', {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ paths: list }),
      });
      if (!res.ok) {
        throw new Error(`Request failed: ${res.status}`);
      }
      setResult(await res.json());
    } catch (err) {
      setError(err.message || 'Request failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="container">
      <h1>Safe File Deleter</h1>
      <p className="hint">
        Enter one relative file path per line. Paths are resolved against the
        configured base directory.
      </p>
      <form onSubmit={handleSubmit}>
        <textarea
          rows={8}
          value={paths}
          onChange={(e) => setPaths(e.target.value)}
          placeholder="example.txt"
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Deleting…' : 'Delete files'}
        </button>
      </form>
      {error && <div className="error">{error}</div>}
      {result && (
        <div className="result">
          <div>
            <strong>Deleted:</strong>{' '}
            {result.deleted && result.deleted.length > 0
              ? result.deleted.join(', ')
              : 'none'}
          </div>
          <div>
            <strong>Failed:</strong>{' '}
            {result.failed && result.failed.length > 0
              ? result.failed.join(', ')
              : 'none'}
          </div>
        </div>
      )}
    </div>
  );
}
