export default function Avatar({ user, size = 40 }) {
  const initial = (user?.name || '?').trim().charAt(0).toUpperCase();
  if (user?.profilePictureUrl) {
    return (
      <img className="avatar" src={user.profilePictureUrl} alt="" width={size} height={size}
        referrerPolicy="no-referrer" />
    );
  }
  return (
    <span className="avatar avatar-fallback" style={{ width: size, height: size }} aria-hidden="true">
      {initial}
    </span>
  );
}
