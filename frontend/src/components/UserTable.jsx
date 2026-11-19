import Avatar from './Avatar.jsx';
import { formatDateTime } from '../format.js';

export default function UserTable({ users }) {
  if (!users?.length) return <p className="muted">No users found.</p>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr><th>User</th><th>Email</th><th>Role</th><th>Registered</th><th>Last sign-in</th></tr>
        </thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id}>
              <td className="user-cell"><Avatar user={u} size={28} /> {u.name}</td>
              <td>{u.email}</td>
              <td><span className={`badge ${u.role === 'ADMIN' ? 'badge-admin' : ''}`}>{u.role}</span></td>
              <td>{formatDateTime(u.createdAt)}</td>
              <td>{formatDateTime(u.lastLoginAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
