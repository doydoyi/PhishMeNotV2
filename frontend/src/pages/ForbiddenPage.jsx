import { Link } from 'react-router-dom';
import { homePathFor, useAuth } from '../auth/AuthContext.jsx';

export default function ForbiddenPage() {
  const { user } = useAuth();
  return (
    <div className="center-page">
      <h1>403</h1>
      <p>You do not have permission to view this page.</p>
      <Link className="btn" to={homePathFor(user)}>Go to my dashboard</Link>
    </div>
  );
}
