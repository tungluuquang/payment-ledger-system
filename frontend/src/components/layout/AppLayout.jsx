import { useState } from "react";
import { Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";
import { TransferModal } from "../payments/TransferModal";
import { AccountModal } from "../accounts/AccountModal";
import { Toast } from "../ui/Toast";
import { useAppData } from "../../state/AppDataProvider";

export function AppLayout() {
  const [modal, setModal] = useState(null);
  const [toast, setToast] = useState("");
  const { selectedAccount, refreshPayments } = useAppData();

  return (
    <div className="app-shell">
      <Sidebar onNewTransfer={() => setModal("transfer")} />
      <main className="dashboard">
        <Topbar
          onNewTransfer={() => setModal("transfer")}
          onNewAccount={() => setModal("account")}
        />
        <Outlet context={{
          openTransfer: () => setModal("transfer"),
          openAccount: () => setModal("account"),
        }} />
      </main>
      {modal === "transfer" && (
        <TransferModal
          source={selectedAccount}
          onClose={() => setModal(null)}
          onCreated={async (paymentId) => {
            setModal(null);
            setToast(`Transfer ${paymentId.slice(0, 8)} was accepted.`);
            await refreshPayments();
          }}
        />
      )}
      {modal === "account" && (
        <AccountModal
          onClose={() => setModal(null)}
          onCreated={(account) => {
            setModal(null);
            setToast(`Account ${account.accountId.slice(0, 8)} created.`);
          }}
        />
      )}
      {toast && <Toast message={toast} onClose={() => setToast("")} />}
    </div>
  );
}
