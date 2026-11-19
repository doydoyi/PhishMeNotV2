import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth/AuthContext.jsx';
import UserTable from '../components/UserTable.jsx';

export default function AdminUsers() {
  const { sessionExpired } = useAuth();
  const [query, setQuery] = useState('');
  const [submitted, setSubmitted] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const params = new URLSearchParams({ query: submitted, page: String(page), size: '10' });
    setError(null);
    api(`/api/admin/users?${params}`)
      .then(setData)
      .catch((e) => (e.status === 401 ? sessionExpired() : setError(e.message)));
  }, [submitted, page, sessionExpired]);

  function onSearch(e) {
    e.preventDefault();
    setPage(0);
    setSubmitted(query.trim());
  }

  return (
    <section>
      <h2>Users</h2>
      <p className="muted">
        All registered accounts. Roles are assigned directly in the database (SRS FR-046).
      </p>

      <form className="search" onSubmit={onSearch}>
        <input type="search" placeholder="Search by name or email" value={query}
          onChange={(e) => setQuery(e.target.value)} maxLength={255} aria-label="Search users" />
        <button className="btn" type="submit">Search</button>
      </form>

      {error && <div className="alert alert-error">{error}</div>}
      {!data && !error && <div className="muted">Loading users…</div>}
      {data && (
        <div className="card">
          <UserTable users={data.users} />
          <div className="pager">
            <button className="btn btn-outline" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
            <span className="muted">
              Page {data.totalPages === 0 ? 0 : data.page + 1} of {data.totalPages} · {data.totalElements} user(s)
            </span>
            <button className="btn btn-outline" disabled={data.page + 1 >= data.totalPages}
              onClick={() => setPage(page + 1)}>Next</button>
          </div>
        </div>
      )}
    </section>
  );
}
