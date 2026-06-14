import {
  ArrowUpRight,
  Check,
  Clock3,
  Copy,
  Landmark,
  Plus,
} from "lucide-react";
import { useMemo } from "react";
import { useOutletContext } from "react-router-dom";
import { TransferTable } from "../components/payments/TransferTable";
import { ErrorState, PageLoader } from "../components/ui/States";
import { useAppData } from "../state/AppDataProvider";
import { formatMoney, shortId } from "../utils/format";

export function DashboardPage() {
  const {
    profile,
    selectedAccount,
    payments,
    loading,
    error,
    refresh,
  } = useAppData();
  const { openTransfer, openAccount } = useOutletContext();
  const totals = useMemo(() => ({
    completed: payments.filter(
      (payment) => payment.paymentStatus === "COMPLETED",
    ).length,
    processing: payments.filter((payment) => [
      "INITIATED", "PROCESSING", "COMPENSATING",
    ].includes(payment.paymentStatus)).length,
  }), [payments]);

  if (loading && !profile) return <PageLoader />;
  if (error && !profile) return <ErrorState message={error} onRetry={refresh} />;
  if (!selectedAccount) {
    return (
      <section className="connect-card">
        <span className="connect-icon"><Landmark size={24} /></span>
        <h2>Open your first account</h2>
        <p>Create an account to start sending and receiving transfers.</p>
        <button className="primary-button" onClick={openAccount}>
          <Plus size={16} /> Create account
        </button>
      </section>
    );
  }

  return (
    <>
      <div className="dashboard-greeting">
        <div>
          <h2>Good to see you, {profile?.fullName?.split(" ")[0]}.</h2>
          <p>Here is what is happening with your money.</p>
        </div>
      </div>
      <section className="summary-grid">
        <div className="balance-card">
          <div className="card-orbit card-orbit-one" />
          <div className="card-orbit card-orbit-two" />
          <div className="balance-top">
            <span>AVAILABLE BALANCE</span>
            <span className="balance-logo"><Landmark size={19} /></span>
          </div>
          <strong>{formatMoney(selectedAccount.balance, selectedAccount.currency)}</strong>
          <button
            className="account-id"
            onClick={() => navigator.clipboard.writeText(selectedAccount.accountId)}
          >
            {shortId(selectedAccount.accountId)} <Copy size={14} />
          </button>
          <div className="balance-actions">
            <button className="card-primary-action" onClick={openTransfer}>
              <ArrowUpRight size={17} /> Send money
            </button>
            <button onClick={openAccount}><Plus size={17} /> Add account</button>
          </div>
        </div>
        <Stat icon={Check} value={totals.completed} label="Completed transfers" note="Settled successfully" tone="success" />
        <Stat icon={Clock3} value={totals.processing} label="Transfers in progress" note="Being processed" tone="pending" />
      </section>
      <section className="panel">
        <div className="panel-heading">
          <div><h2>Recent activity</h2><p>Latest transfers for the selected account.</p></div>
        </div>
        <TransferTable
          payments={payments.slice(0, 8)}
          accountId={selectedAccount.accountId}
          emptyAction={openTransfer}
        />
      </section>
    </>
  );
}

function Stat({ icon: Icon, value, label, note, tone }) {
  return (
    <div className={`stat-card ${tone}`}>
      <div className="stat-head"><span className="stat-icon"><Icon size={18} /></span><span className="stat-trend">Live</span></div>
      <div className="stat-value"><strong>{value}</strong><span>{label}</span><small>{note}</small></div>
    </div>
  );
}
