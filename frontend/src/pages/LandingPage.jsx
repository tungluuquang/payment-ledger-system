import {
  Activity,
  ArrowRight,
  Check,
  Landmark,
  LockKeyhole,
  ShieldCheck,
  Sparkles,
  WalletCards,
  X,
} from "lucide-react";
import { useState } from "react";
import { useAuth } from "../auth/AuthProvider";
import { userApi } from "../services/api";
import { Field } from "../components/ui/Modal";

export function LandingPage() {
  const { login } = useAuth();
  const [registering, setRegistering] = useState(false);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [form, setForm] = useState({
    fullName: "",
    username: "",
    email: "",
    password: "",
  });

  async function register(event) {
    event.preventDefault();
    setBusy(true);
    setNotice("");
    try {
      await userApi.register(form);
      setRegistering(false);
      setNotice("Account created. Sign in to continue.");
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="welcome">
      <div className="welcome-glow welcome-glow-one" />
      <div className="welcome-glow welcome-glow-two" />
      <header className="landing-header">
        <Brand />
        <div className="header-security"><LockKeyhole size={14} /> Bank-grade security</div>
      </header>
      <section className="welcome-copy">
        <div className="hero-pill"><Sparkles size={14} /> Built for modern money movement</div>
        <h1>Money moves fast.<br /><span>Your records stay clear.</span></h1>
        <p className="hero-text">Send, receive, and follow every transfer from one beautifully simple workspace. No hidden steps. No uncertain status.</p>
        <div className="feature-row">
          <Feature icon={ShieldCheck} text="Protected by OAuth 2.0" />
          <Feature icon={Activity} text="Real-time saga tracking" />
          <Feature icon={WalletCards} text="Auditable ledger history" />
        </div>
        <ProductPreview />
      </section>
      <section className="auth-card">
        {registering ? (
          <>
            <div className="card-heading">
              <div><span className="auth-kicker">GET STARTED</span><h2>Create account</h2></div>
              <button className="icon-button" onClick={() => setRegistering(false)}><X size={18} /></button>
            </div>
            <form onSubmit={register} className="stack">
              <Field label="Full name"><input required value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} placeholder="Nguyen Van An" /></Field>
              <div className="two-columns">
                <Field label="Username"><input required minLength={3} value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} /></Field>
                <Field label="Email"><input required type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
              </div>
              <Field label="Password" hint="12+ characters"><input required type="password" minLength={12} value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></Field>
              {notice && <div className="notice">{notice}</div>}
              <button className="primary-button" disabled={busy}>{busy ? "Creating..." : "Create account"}</button>
            </form>
          </>
        ) : (
          <>
            <span className="auth-kicker">SECURE ACCESS</span>
            <h2>Welcome back</h2>
            <p>Sign in to manage your accounts and transfers.</p>
            {notice && <div className="notice success">{notice}</div>}
            <button className="primary-button" onClick={() => login("/app")}>Sign in securely <ArrowRight size={17} /></button>
            <div className="auth-divider"><span>New to LedgerPay?</span></div>
            <button className="text-button" onClick={() => setRegistering(true)}>Create your free account</button>
            <div className="auth-footnote"><LockKeyhole size={13} /> Your credentials are encrypted and never shared.</div>
          </>
        )}
      </section>
    </main>
  );
}

function Brand() {
  return <div className="brand"><span className="brand-mark"><Landmark size={21} /></span><span>Ledger<span className="brand-accent">Pay</span></span></div>;
}
function Feature({ icon: Icon, text }) {
  return <div><Icon size={18} /><span>{text}</span></div>;
}
function ProductPreview() {
  return <div className="hero-preview"><div className="preview-card"><div className="preview-card-top"><div><span>Available balance</span><strong>$24,860.50</strong></div><span className="preview-chip"><Landmark size={18} /></span></div><div className="preview-number">•••• 4827</div><div className="preview-bottom"><span>LEDGER PAY</span><span>USD</span></div></div><div className="preview-activity"><span className="preview-success"><Check size={16} /></span><div><strong>Transfer completed</strong><small>Ledger entry recorded</small></div><b>+$1,250</b></div></div>;
}
