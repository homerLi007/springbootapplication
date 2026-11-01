import { useState } from 'react';

function readCookie(name) {
  const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
  return match ? decodeURIComponent(match[1]) : null;
}

export default function App() {
  const [input, setInput] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [authenticated, setAuthenticated] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  async function handleLogin(e) {
    e.preventDefault();
    setError(null);
    const csrf = readCookie('XSRF-TOKEN');
    const body = new URLSearchParams({ username, password });
    try {
      const res = await fetch('/api/login', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          ...(csrf ? { 'X-XSRF-TOKEN': csrf } : {})
        },
        body
      });
      if (!res.ok) throw new Error('Login failed: ' + res.status);
      setAuthenticated(true);
      setPassword('');
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleLogout() {
    const csrf = readCookie('XSRF-TOKEN');
    await fetch('/api/logout', {
      method: 'POST',
      credentials: 'include',
      headers: csrf ? { 'X-XSRF-TOKEN': csrf } : {}
    });
    setAuthenticated(false);
    setResult(null);
  }

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
      const csrf = readCookie('XSRF-TOKEN');
      const res = await fetch('/api/delete', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          ...(csrf ? { 'X-XSRF-TOKEN': csrf } : {})
        },
        body: JSON.stringify({ paths })
      });
      if (!res.ok) throw new Error('Request failed: ' + res.status);
      setResult(await res.json());
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  if (!authenticated) {
    return (
      <div className="app">
        <h1>File Deleter</h1>
        <form onSubmit={handleLogin}>
          <input
            type="text"
            placeholder="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
          />
          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
          />
          <button type="submit">Log in</button>
        </form>
        {error && <p className="error">Error: {error}</p>}
      </div>
    );
  }

  return (
    <div className="app">
      <h1>File Deleter</h1>
      <button type="button" onClick={handleLogout}>Log out</button>
      <form onSubmit={handleSubmit}>
        <textarea
          rows={8}
          placeholder="One file path per line"
          value={input}
          onChange={(e) => setInput(e.target.value)}
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Deleting...' : 'Delete Files'}
        </button>
      </form>
      {error && <p className="error">Error: {error}</p>}
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
            {result.failed.map((p) => (
              <li key={p}>{p}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
