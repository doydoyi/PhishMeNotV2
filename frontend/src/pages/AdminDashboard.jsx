import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../auth/AuthContext.jsx';
import UserTable from '../components/UserTable.jsx';

export default function AdminDashboard() {
  const { sessionExpired } = useAuth();
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api('/api/admin/dashboard')
      .then(setData)
      .catch((e) => (e.status === 401 ? sessionExpired() : setError(e.message)));
  }, [sessionExpired]);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!data) return <div className="muted">Loading admin dashboard…</div>;

  return (
    <section>
      <h2>Administrator Dashboard</h2>
      <p className="muted">Overview of PhishMeNot accounts.</p>

      <div className="stats">
        <Stat label="Total users" value={data.totalUsers} />
        <Stat label="General users" value={data.generalUsers} />
        <Stat label="Administrators" value={data.administrators} />
        <Stat label="New in last 7 days" value={data.newUsersLast7Days} />
      </div>

      <div className="card">
        <div className="card-head">
          <h3>Newest accounts</h3>
          <Link to="/admin/users">View all users →</Link>
        </div>
        <UserTable users={data.recentUsers} />
      </div>
    </section>
  );
}

function Stat({ label, value }) {
  return (
    <div className="card stat">
      <span className="stat-label">{label}</span>
      <span className="stat-value">{value}</span>
    </div>
  );
}
