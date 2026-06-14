import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <main className="centered">
      <span className="auth-kicker">404</span>
      <h2>Page not found</h2>
      <p>The page you requested does not exist.</p>
      <Link className="primary-button" to="/">Return home</Link>
    </main>
  );
}
