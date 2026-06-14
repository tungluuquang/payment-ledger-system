import { useEffect, useMemo, useState } from "react";
import {
  ArrowDownLeft,
  ArrowRight,
  ArrowUpRight,
  Check,
  CircleDollarSign,
  Clock3,
  Copy,
  CreditCard,
  Landmark,
  LogOut,
  Plus,
  RefreshCw,
  Search,
  Send,
  ShieldCheck,
  UserRound,
  WalletCards,
  X,
} from "lucide-react";
import { api } from "./api";
import {
  beginLogin,
  currentUser,
  finishLogin,
  getAccessToken,
  logout,
} from "./auth";

const STATUS_LABELS = {
  COMPLETED: "Completed",
  PROCESSING: "Processing",
  INITIATED: "Initiated",
  COMPENSATING: "Rolling back",
  COMPENSATED: "Rolled back",
  CANCELLED: "Cancelled",
  FAILED: "Failed",
};

function App() {
  const [user, setUser] = useState(currentUser());
  const [callbackState, setCallbackState] = useState(
    location.pathname === "/callback" ? "loading" : null,
  );

  useEffect(() => {
    if (location.pathname !== "/callback") return;
    const params = new URLSearchParams(location.search);
    finishLogin(params.get("code"), params.get("state"))
      .then(() => {
        history.replaceState({}, "", "/");
        setUser(currentUser());
        setCallbackState("done");
      })
      .catch(() => setCallbackState("error"));
  }, []);

  if (callbackState === "loading") {
    return <CenteredMessage title="Signing you in" spinning />;
  }
  if (callbackState === "error") {
    return (
      <CenteredMessage
        title="Login failed"
        detail="Please return to the home page and try again."
      />
    );
  }
  return user && getAccessToken() ? (
    <Dashboard user={user} />
  ) : (
    <Welcome onLoggedIn={() => setUser(currentUser())} />
  );
}

function Welcome() {
  const [registering, setRegistering] = useState(false);
  const [form, setForm] = useState({
    username: "",
    email: "",
    fullName: "",
    password: "",
  });
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);

  async function register(event) {
    event.preventDefault();
    setBusy(true);
    setNotice("");
    try {
      await api.register(form);
      setNotice("Account created. You can sign in now.");
      setRegistering(false);
    } catch (error) {
      setNotice(readableError(error));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="welcome">
      <section className="welcome-copy">
        <div className="brand">
          <span className="brand-mark"><Landmark size={22} /></span>
          <span>Ledger Pay</span>
        </div>
        <div className="eyebrow">Simple, reliable transfers</div>
        <h1>Move money with a clear record of every step.</h1>
        <p className="hero-text">
          A focused interface for accounts, transfers, and ledger history.
          Built so you always know what happened and what happens next.
        </p>
        <div className="feature-row">
          <Feature icon={ShieldCheck} text="Secure OAuth login" />
          <Feature icon={Clock3} text="Live transfer status" />
          <Feature icon={WalletCards} text="Full account history" />
        </div>
      </section>

      <section className="auth-card">
        {registering ? (
          <>
            <div className="card-heading">
              <div>
                <span className="eyebrow">Get started</span>
                <h2>Create an account</h2>
              </div>
              <button className="icon-button" onClick={() => setRegistering(false)}>
                <X size={18} />
              </button>
            </div>
            <form onSubmit={register} className="stack">
              <Field label="Full name">
                <input required value={form.fullName} onChange={(e) =>
                  setForm({ ...form, fullName: e.target.value })
                } placeholder="Nguyen Van An" />
              </Field>
              <div className="two-columns">
                <Field label="Username">
                  <input required minLength={3} value={form.username}
                    onChange={(e) => setForm({ ...form, username: e.target.value })}
                    placeholder="nguyenvanan" />
                </Field>
                <Field label="Email">
                  <input required type="email" value={form.email}
                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                    placeholder="an@example.com" />
                </Field>
              </div>
              <Field label="Password" hint="At least 12 characters">
                <input required type="password" minLength={12} value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  placeholder="Your secure password" />
              </Field>
              {notice && <div className="notice">{notice}</div>}
              <button className="primary-button" disabled={busy}>
                {busy ? "Creating..." : "Create account"}
              </button>
            </form>
          </>
        ) : (
          <>
            <div className="auth-icon"><UserRound size={25} /></div>
            <span className="eyebrow">Welcome back</span>
            <h2>Sign in to your wallet</h2>
            <p>Use your Ledger Pay username and password to continue.</p>
            {notice && <div className="notice success">{notice}</div>}
            <button className="primary-button" onClick={beginLogin}>
              Continue to sign in <ArrowRight size={17} />
            </button>
            <button className="text-button" onClick={() => setRegistering(true)}>
              New here? Create an account
            </button>
          </>
        )}
      </section>
    </main>
  );
}

function Dashboard({ user }) {
  const [accountId, setAccountId] = useState(
    localStorage.getItem("ledger-pay.account-id") || "",
  );
  const [account, setAccount] = useState(null);
  const [profile, setProfile] = useState(null);
  const [payments, setPayments] = useState([]);
  const [selected, setSelected] = useState(null);
  const [timeline, setTimeline] = useState([]);
  const [modal, setModal] = useState(null);
  const [notice, setNotice] = useState("");
  const [loading, setLoading] = useState(false);

  async function load(targetId = accountId) {
    setLoading(true);
    setNotice("");
    try {
      const profileRequest = profile ? Promise.resolve(profile) : api.getUser(user.id);
      const [nextProfile, nextAccount, paymentPage] = await Promise.all([
        profileRequest,
        targetId ? api.getAccount(targetId) : Promise.resolve(null),
        api.listPayments(targetId || null),
      ]);
      setProfile(nextProfile);
      setAccount(nextAccount);
      setPayments(paymentPage.content || []);
    } catch (error) {
      setNotice(readableError(error));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  async function connectAccount(event) {
    event.preventDefault();
    localStorage.setItem("ledger-pay.account-id", accountId.trim());
    await load(accountId.trim());
  }

  async function viewPayment(payment) {
    setSelected(payment);
    setTimeline([]);
    try {
      const events = await api.getTimeline(payment.paymentId);
      setTimeline(events.content || []);
    } catch (error) {
      setNotice(readableError(error));
    }
  }

  const totals = useMemo(() => {
    const completed = payments.filter((item) => item.paymentStatus === "COMPLETED");
    return {
      completed: completed.length,
      processing: payments.filter((item) =>
        ["INITIATED", "PROCESSING", "COMPENSATING"].includes(item.paymentStatus),
      ).length,
    };
  }, [payments]);

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark"><Landmark size={21} /></span>
          <span>Ledger Pay</span>
        </div>
        <nav>
          <button className="nav-item active"><CircleDollarSign size={19} /> Overview</button>
          <button className="nav-item" onClick={() => setModal("transfer")}>
            <Send size={19} /> Send money
          </button>
          <button className="nav-item" onClick={() => setModal("account")}>
            <CreditCard size={19} /> New account
          </button>
        </nav>
        <div className="sidebar-user">
          <div className="avatar">{(profile?.fullName || user.username || "U")[0]}</div>
          <div>
            <strong>{profile?.fullName || user.username}</strong>
            <span>{profile?.email || "Signed in"}</span>
          </div>
          <button className="icon-button" onClick={logout} title="Sign out">
            <LogOut size={17} />
          </button>
        </div>
      </aside>

      <main className="dashboard">
        <header className="topbar">
          <div>
            <span className="eyebrow">Overview</span>
            <h1>Good to see you, {(profile?.fullName || user.username).split(" ")[0]}.</h1>
          </div>
          <button className="secondary-button" onClick={() => load()} disabled={loading}>
            <RefreshCw size={16} className={loading ? "spin" : ""} /> Refresh
          </button>
        </header>

        {notice && <div className="notice"><span>{notice}</span><button onClick={() => setNotice("")}><X size={16} /></button></div>}

        {!account ? (
          <section className="connect-card">
            <div className="auth-icon"><WalletCards size={24} /></div>
            <h2>Connect your account</h2>
            <p>Enter an account ID you own, or create a new account.</p>
            <form onSubmit={connectAccount} className="inline-form">
              <input value={accountId} onChange={(e) => setAccountId(e.target.value)}
                placeholder="Account UUID" required />
              <button className="primary-button">Connect</button>
            </form>
            <button className="text-button" onClick={() => setModal("account")}>
              Create a new account instead
            </button>
          </section>
        ) : (
          <>
            <section className="summary-grid">
              <div className="balance-card">
                <div className="balance-top">
                  <span>Available balance</span>
                  <Landmark size={21} />
                </div>
                <strong>{money(account.balance, account.currency)}</strong>
                <button className="account-id" onClick={() => navigator.clipboard.writeText(account.accountId)}>
                  {shortId(account.accountId)} <Copy size={14} />
                </button>
                <div className="balance-actions">
                  <button onClick={() => setModal("transfer")}><ArrowUpRight size={17} /> Send</button>
                  <button onClick={() => setModal("account")}><Plus size={17} /> New account</button>
                </div>
              </div>
              <Stat icon={Check} label="Completed transfers" value={totals.completed} />
              <Stat icon={Clock3} label="In progress" value={totals.processing} />
            </section>

            <section className="panel">
              <div className="panel-heading">
                <div><span className="eyebrow">Activity</span><h2>Recent transfers</h2></div>
                <span className="muted">{payments.length} records</span>
              </div>
              {payments.length ? (
                <div className="transaction-list">
                  {payments.map((payment) => {
                    const outgoing = payment.sourceAccountId === account.accountId;
                    return (
                      <button className="transaction" key={payment.paymentId}
                        onClick={() => viewPayment(payment)}>
                        <span className={`transaction-icon ${outgoing ? "outgoing" : "incoming"}`}>
                          {outgoing ? <ArrowUpRight size={18} /> : <ArrowDownLeft size={18} />}
                        </span>
                        <span className="transaction-main">
                          <strong>{outgoing ? "Transfer sent" : "Transfer received"}</strong>
                          <small>{shortId(outgoing ? payment.destinationAccountId : payment.sourceAccountId)}</small>
                        </span>
                        <StatusBadge status={payment.paymentStatus} />
                        <span className={`amount ${outgoing ? "negative" : "positive"}`}>
                          {outgoing ? "-" : "+"}{money(payment.amount, payment.currency)}
                        </span>
                        <ArrowRight size={16} className="row-arrow" />
                      </button>
                    );
                  })}
                </div>
              ) : (
                <EmptyState onAction={() => setModal("transfer")} />
              )}
            </section>
          </>
        )}
      </main>

      {modal === "transfer" && (
        <TransferModal source={account} onClose={() => setModal(null)}
          onCreated={async (paymentId) => {
            setModal(null);
            setNotice(`Transfer accepted: ${shortId(paymentId)}`);
            await load();
          }} />
      )}
      {modal === "account" && (
        <AccountModal onClose={() => setModal(null)} onCreated={async (created) => {
          setAccountId(created.accountId);
          localStorage.setItem("ledger-pay.account-id", created.accountId);
          setModal(null);
          await load(created.accountId);
        }} />
      )}
      {selected && (
        <PaymentDrawer payment={selected} timeline={timeline}
          onClose={() => setSelected(null)} />
      )}
    </div>
  );
}

function TransferModal({ source, onClose, onCreated }) {
  const [form, setForm] = useState({
    destinationAccountId: "",
    amount: "",
    description: "",
  });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function submit(event) {
    event.preventDefault();
    if (!source) return setError("Connect a source account first.");
    setBusy(true);
    try {
      const response = await api.createPayment({
        sourceAccountId: source.accountId,
        destinationAccountId: form.destinationAccountId,
        amount: form.amount,
        currency: source.currency,
        description: form.description,
      });
      onCreated(response.paymentId);
    } catch (requestError) {
      setError(readableError(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="Send money" subtitle="Create a new account transfer." onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <Field label="From account"><div className="readonly-field">{source ? shortId(source.accountId) : "No account connected"}</div></Field>
        <Field label="Destination account">
          <input required value={form.destinationAccountId}
            onChange={(e) => setForm({ ...form, destinationAccountId: e.target.value })}
            placeholder="Destination account UUID" />
        </Field>
        <div className="two-columns">
          <Field label="Amount">
            <input required type="number" min="0.01" step="0.01" value={form.amount}
              onChange={(e) => setForm({ ...form, amount: e.target.value })}
              placeholder="0.00" />
          </Field>
          <Field label="Currency"><div className="readonly-field">{source?.currency || "USD"}</div></Field>
        </div>
        <Field label="Description" hint="Optional">
          <input value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            placeholder="What is this transfer for?" />
        </Field>
        {error && <div className="notice">{error}</div>}
        <button className="primary-button" disabled={busy || !source}>
          {busy ? "Sending..." : "Send transfer"} <Send size={16} />
        </button>
      </form>
    </Modal>
  );
}

function AccountModal({ onClose, onCreated }) {
  const [form, setForm] = useState({ initialBalance: "0", currency: "USD" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    try {
      onCreated(await api.createAccount(form));
    } catch (requestError) {
      setError(readableError(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal title="Create account" subtitle="Open a new ledger account." onClose={onClose}>
      <form onSubmit={submit} className="stack">
        <Field label="Initial balance">
          <input required type="number" min="0" step="0.01" value={form.initialBalance}
            onChange={(e) => setForm({ ...form, initialBalance: e.target.value })} />
        </Field>
        <Field label="Currency">
          <select value={form.currency} onChange={(e) => setForm({ ...form, currency: e.target.value })}>
            <option>USD</option><option>EUR</option><option>VND</option>
          </select>
        </Field>
        {error && <div className="notice">{error}</div>}
        <button className="primary-button" disabled={busy}>
          {busy ? "Creating..." : "Create account"} <Plus size={16} />
        </button>
      </form>
    </Modal>
  );
}

function PaymentDrawer({ payment, timeline, onClose }) {
  const steps = [
    ["Fraud check", payment.fraudStatus],
    ["Debit source", payment.debitStatus],
    ["Credit destination", payment.creditStatus],
    ["Record ledger", payment.ledgerStatus],
  ];
  return (
    <div className="drawer-backdrop" onMouseDown={onClose}>
      <aside className="drawer" onMouseDown={(e) => e.stopPropagation()}>
        <div className="card-heading">
          <div><span className="eyebrow">Transfer details</span><h2>{shortId(payment.paymentId)}</h2></div>
          <button className="icon-button" onClick={onClose}><X size={18} /></button>
        </div>
        <div className="drawer-amount">{money(payment.amount, payment.currency)}</div>
        <StatusBadge status={payment.paymentStatus} />
        <div className="route-box">
          <div><span>From</span><strong>{shortId(payment.sourceAccountId)}</strong></div>
          <ArrowRight size={18} />
          <div><span>To</span><strong>{shortId(payment.destinationAccountId)}</strong></div>
        </div>
        <h3>Processing steps</h3>
        <div className="steps">
          {steps.map(([label, status]) => (
            <div className="step" key={label}>
              <span className={`step-dot ${status?.toLowerCase()}`}>
                {status === "COMPLETED" ? <Check size={13} /> : <Clock3 size={13} />}
              </span>
              <div><strong>{label}</strong><span>{titleCase(status)}</span></div>
            </div>
          ))}
        </div>
        <h3>Event timeline</h3>
        <div className="timeline">
          {timeline.map((event) => (
            <div key={event.eventId}>
              <span className="timeline-dot" />
              <div><strong>{splitEventName(event.eventType)}</strong><small>{formatDate(event.occurredAt)}</small></div>
            </div>
          ))}
          {!timeline.length && <p className="muted">Loading timeline...</p>}
        </div>
        {payment.lastError && <div className="notice">{payment.lastError}</div>}
      </aside>
    </div>
  );
}

function Modal({ title, subtitle, onClose, children }) {
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <section className="modal" onMouseDown={(e) => e.stopPropagation()}>
        <div className="card-heading">
          <div><span className="eyebrow">{subtitle}</span><h2>{title}</h2></div>
          <button className="icon-button" onClick={onClose}><X size={18} /></button>
        </div>
        {children}
      </section>
    </div>
  );
}

function Field({ label, hint, children }) {
  return <label className="field"><span>{label}<small>{hint}</small></span>{children}</label>;
}
function Feature({ icon: Icon, text }) {
  return <div><Icon size={18} /><span>{text}</span></div>;
}
function Stat({ icon: Icon, label, value }) {
  return <div className="stat-card"><span className="stat-icon"><Icon size={19} /></span><div><strong>{value}</strong><span>{label}</span></div></div>;
}
function StatusBadge({ status }) {
  return <span className={`status ${status?.toLowerCase()}`}>{STATUS_LABELS[status] || titleCase(status)}</span>;
}
function EmptyState({ onAction }) {
  return <div className="empty-state"><span><Search size={22} /></span><h3>No transfers yet</h3><p>Your latest activity will appear here.</p><button className="secondary-button" onClick={onAction}>Make a transfer</button></div>;
}
function CenteredMessage({ title, detail, spinning }) {
  return <main className="centered"><div className="brand-mark">{spinning ? <RefreshCw className="spin" /> : <X />}</div><h2>{title}</h2>{detail && <p>{detail}</p>}</main>;
}

function money(value, currency = "USD") {
  return new Intl.NumberFormat("en-US", { style: "currency", currency }).format(Number(value || 0));
}
function shortId(value = "") {
  return value.length > 16 ? `${value.slice(0, 8)}...${value.slice(-5)}` : value;
}
function titleCase(value = "") {
  return value.toLowerCase().replaceAll("_", " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}
function splitEventName(value = "") {
  return value.replace(/([a-z])([A-Z])/g, "$1 $2");
}
function formatDate(value) {
  return value ? new Intl.DateTimeFormat("en", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "";
}
function readableError(error) {
  try {
    const parsed = JSON.parse(error.message);
    return parsed.message || parsed.detail || "Something went wrong.";
  } catch {
    return error.message || "Something went wrong.";
  }
}

export default App;
