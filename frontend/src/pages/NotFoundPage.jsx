import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="center-page">
      <h1>404</h1>
      <p>This page does not exist.</p>
      <Link className="btn" to="/">Go home</Link>
    </div>
  );
}
