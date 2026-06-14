import { Check, Copy, Landmark, Plus } from "lucide-react";
import { useOutletContext } from "react-router-dom";
import { ErrorState, PageLoader } from "../components/ui/States";
import { StatusBadge } from "../components/ui/StatusBadge";
import { useAppData } from "../state/AppDataProvider";
import { formatMoney, shortId } from "../utils/format";

export function AccountsPage() {
  const {
    accounts,
    selectedAccount,
    selectAccount,
    loading,
    error,
    refresh,
  } = useAppData();
  const { openAccount } = useOutletContext();
  if (loading && !accounts.length) return <PageLoader label="Loading accounts" />;
  if (error && !accounts.length) return <ErrorState message={error} onRetry={refresh} />;

  return (
    <>
      <section className="section-heading">
        <div><h2>Your accounts</h2><p>Balances and account identifiers across supported currencies.</p></div>
        <button className="primary-button compact" onClick={openAccount}><Plus size={16} /> New account</button>
      </section>
      <section className="account-grid">
        {accounts.map((account) => {
          const selected = account.accountId === selectedAccount?.accountId;
          return (
            <article className={`account-card ${selected ? "selected" : ""}`} key={account.accountId}>
              <div className="account-card-top">
                <span className="account-currency"><Landmark size={17} /> {account.currency}</span>
                <StatusBadge status={account.status} />
              </div>
              <span className="account-balance-label">Available balance</span>
              <strong>{formatMoney(account.balance, account.currency)}</strong>
              <div className="account-card-id">
                <span>{shortId(account.accountId, 12, 6)}</span>
                <button onClick={() => navigator.clipboard.writeText(account.accountId)}><Copy size={14} /></button>
              </div>
              <button
                className={selected ? "selected-account-button" : "secondary-button"}
                onClick={() => selectAccount(account.accountId)}
                disabled={selected}
              >
                {selected ? <><Check size={15} /> Active account</> : "Use this account"}
              </button>
            </article>
          );
        })}
        <button className="account-card add-account-card" onClick={openAccount}>
          <span><Plus size={22} /></span>
          <strong>Open another account</strong>
          <small>USD, EUR or VND</small>
        </button>
      </section>
    </>
  );
}
