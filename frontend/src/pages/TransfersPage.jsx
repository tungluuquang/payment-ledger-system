import { Filter, Search, Send } from "lucide-react";
import { useMemo, useState } from "react";
import { useOutletContext } from "react-router-dom";
import { TransferTable } from "../components/payments/TransferTable";
import { ErrorState, PageLoader } from "../components/ui/States";
import { useAppData } from "../state/AppDataProvider";

export function TransfersPage() {
  const { payments, selectedAccount, loading, error, refresh } = useAppData();
  const { openTransfer } = useOutletContext();
  const [status, setStatus] = useState("");
  const [search, setSearch] = useState("");
  const filtered = useMemo(() => payments.filter((payment) => {
    const statusMatch = !status || payment.paymentStatus === status;
    const query = search.trim().toLowerCase();
    const searchMatch = !query || [
      payment.paymentId,
      payment.sourceAccountId,
      payment.destinationAccountId,
      payment.correlationId,
    ].some((value) => value?.toLowerCase().includes(query));
    return statusMatch && searchMatch;
  }), [payments, search, status]);

  if (loading && !payments.length) return <PageLoader label="Loading transfers" />;
  if (error && !payments.length) return <ErrorState message={error} onRetry={refresh} />;

  return (
    <section className="panel transfers-panel">
      <div className="panel-heading transfer-page-heading">
        <div><h2>Transfer history</h2><p>Search, filter, and inspect every payment state.</p></div>
        <button className="primary-button compact" onClick={openTransfer}><Send size={15} /> New transfer</button>
      </div>
      <div className="filter-bar">
        <label className="search-box"><Search size={16} /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search payment or account ID" /></label>
        <label className="filter-select"><Filter size={15} /><select value={status} onChange={(e) => setStatus(e.target.value)}><option value="">All statuses</option><option value="COMPLETED">Completed</option><option value="PROCESSING">Processing</option><option value="FAILED">Failed</option><option value="CANCELLED">Cancelled</option><option value="COMPENSATING">Rolling back</option></select></label>
      </div>
      <div className="result-count">{filtered.length} transfer{filtered.length === 1 ? "" : "s"}</div>
      <TransferTable payments={filtered} accountId={selectedAccount?.accountId} emptyAction={openTransfer} />
    </section>
  );
}
