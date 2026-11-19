import { useEffect, useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth/AuthContext.jsx';
import Avatar from '../components/Avatar.jsx';
import { formatDateTime } from '../format.js';

export default function UserDashboard() {
  const { sessionExpired } = useAuth();
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api('/api/user/dashboard')
      .then(setData)
      .catch((e) => (e.status === 401 ? sessionExpired() : setError(e.message)));
  }, [sessionExpired]);

  if (error) return <div className="alert alert-error">{error}</div>;
  if (!data) return <div className="muted">Loading your dashboard…</div>;

  const { account, daysSinceRegistration } = data;
  return (
    <section>
      <h2>Welcome, {account.name.split(' ')[0]}!</h2>
      <p className="muted">Here is your PhishMeNot account.</p>

      <div className="card profile-card">
        <Avatar user={account} size={72} />
        <div>
          <h3>{account.name}</h3>
          <p className="muted">{account.email}</p>
          <span className={`badge ${account.role === 'ADMIN' ? 'badge-admin' : ''}`}>
            {account.role === 'ADMIN' ? 'Administrator' : 'General User'}
          </span>
        </div>
      </div>

      <div className="stats">
        <div className="card stat">
          <span className="stat-label">Member since</span>
          <span className="stat-value small">{formatDateTime(account.createdAt)}</span>
          <span className="muted">{daysSinceRegistration} day(s) ago</span>
        </div>
        <div className="card stat">
          <span className="stat-label">Last sign-in</span>
          <span className="stat-value small">{formatDateTime(account.lastLoginAt)}</span>
        </div>
        <div className="card stat">
          <span className="stat-label">Sign-in method</span>
          <span className="stat-value small">Google account</span>
          <span className="muted">No password stored by PhishMeNot</span>
        </div>
      </div>
    </section>
  );
}
