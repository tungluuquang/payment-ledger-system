import { Bell, ChevronDown, Plus, RefreshCw, Send } from "lucide-react";
import { useLocation } from "react-router-dom";
import { useAppData } from "../../state/AppDataProvider";

const pageInfo = {
  "/app": ["Financial overview", "Overview"],
  "/app/accounts": ["Manage your money", "Accounts"],
  "/app/transfers": ["Payment activity", "Transfers"],
  "/app/profile": ["Personal settings", "Profile"],
  "/app/operations": ["Platform operations", "Event analytics"],
};

export function Topbar({ onNewTransfer, onNewAccount }) {
  const location = useLocation();
  const { selectedAccount, accounts, selectAccount, refresh, loading } =
    useAppData();
  const [kicker, title] = location.pathname.startsWith("/app/transfers/")
    ? ["Payment activity", "Transfer detail"]
    : pageInfo[location.pathname] || pageInfo["/app"];
  const operations = location.pathname === "/app/operations";

  return (
    <header className="topbar">
      <div>
        <span className="page-kicker">{kicker}</span>
        <h1>{title}</h1>
      </div>
      <div className="topbar-actions">
        {!operations && accounts.length > 0 && (
          <label className="account-select">
            <LandmarkIcon />
            <select
              value={selectedAccount?.accountId || ""}
              onChange={(event) => selectAccount(event.target.value)}
            >
              {accounts.map((account) => (
                <option key={account.accountId} value={account.accountId}>
                  {account.currency} · {account.accountId.slice(0, 8)}
                </option>
              ))}
            </select>
            <ChevronDown size={14} />
          </label>
        )}
        {!operations && <button className="notification-button"><Bell size={18} /><i /></button>}
        {!operations && <button className="secondary-button icon-only" onClick={refresh}>
          <RefreshCw size={16} className={loading ? "spin" : ""} />
        </button>}
        {!operations && <button className="secondary-button" onClick={onNewAccount}>
          <Plus size={15} /> Account
        </button>}
        {!operations && <button className="primary-button compact" onClick={onNewTransfer}>
          <Send size={15} /> New transfer
        </button>}
      </div>
    </header>
  );
}

function LandmarkIcon() {
  return <span className="account-select-icon">A</span>;
}
