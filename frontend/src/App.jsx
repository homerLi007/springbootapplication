import { useState } from 'react';

export default function App() {
  const [input, setInput] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setResult(null);
    setLoading(true);
    const paths = input
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean);
    try {
      const res = await fetch('/api/delete', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ paths })
      });
      const data = await res.json();
      if (!res.ok) {
        setError(data.error || 'Request failed');
      } else {
        setResult(data);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="app">
      <h1>File Deleter</h1>
      <p>Enter one file path per line. Files will be permanently deleted.</p>
      <form onSubmit={handleSubmit}>
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          rows={6}
          placeholder="/tmp/a.txt\n/tmp/b.txt"
        />
        <button type="submit" disabled={loading || !input.trim()}>
          {loading ? 'Deleting...' : 'Delete Files'}
        </button>
      </form>
      {error && <div className="error">Error: {error}</div>}
      {result && (
        <div className="result">
          <h2>Deleted ({result.deleted.length})</h2>
          <ul>
            {result.deleted.map((p) => (
              <li key={p}>{p}</li>
            ))}
          </ul>
          <h2>Failed ({result.failed.length})</h2>
          <ul>
            {result.failed.map((p, i) => (
              <li key={i}>{p}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
