import { ArrowRight, LockKeyhole, Sparkles, ShieldCheck } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";

export function LoginPage() {
  const { login } = useAuth();

  return (
    <main className="login-page">
      <div className="login-glow login-glow-one" />
      <div className="login-glow login-glow-two" />

      <section className="login-panel">
        <div className="login-badge"><Sparkles size={14} /> Instant ledger access</div>
        <h1>Sign in to <span>LedgerPay</span></h1>
        <p>Securely enter your finance workspace and manage transfers, accounts, and audit-ready history from one polished dashboard.</p>
        <div className="login-features">
          <div><ShieldCheck size={18} /><span>OAuth 2.0 protected</span></div>
          <div><LockKeyhole size={18} /><span>PKCE-secured session</span></div>
          <div><Sparkles size={18} /><span>Fast, modern money flow</span></div>
        </div>
      </section>

      <aside className="login-card">
        <span className="auth-kicker">SECURE LOGIN</span>
        <h2>Ready when you are</h2>
        <p>Use LedgerPay single sign-on to continue into your company finance workspace.</p>
        <button className="primary-button login-action" onClick={() => login("/app")}>Sign in securely <ArrowRight size={17} /></button>
        <div className="auth-divider"><span>Not ready yet?</span></div>
        <Link className="text-button" to="/">Return to landing page</Link>
        <div className="auth-footnote"><LockKeyhole size={13} /> Login is encrypted and never shared.</div>
      </aside>
    </main>
  );
}
