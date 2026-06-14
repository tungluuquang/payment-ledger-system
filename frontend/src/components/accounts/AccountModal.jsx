import { Plus } from "lucide-react";
import { useState } from "react";
import { useAppData } from "../../state/AppDataProvider";
import { Field, Modal } from "../ui/Modal";

export function AccountModal({ onClose, onCreated }) {
  const { createAccount } = useAppData();
  const [form, setForm] = useState({
    initialBalance: "0",
    currency: "USD",
  });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const account = await createAccount(form);
      onCreated(account);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      title="Create account"
      subtitle="Open a new ledger account"
      onClose={onClose}
    >
      <form onSubmit={submit} className="stack">
        <Field label="Initial balance" hint="Can be zero">
          <input
            required
            type="number"
            min="0"
            step="0.01"
            value={form.initialBalance}
            onChange={(event) => setForm({
              ...form,
              initialBalance: event.target.value,
            })}
          />
        </Field>
        <Field label="Currency">
          <select
            value={form.currency}
            onChange={(event) => setForm({
              ...form,
              currency: event.target.value,
            })}
          >
            <option value="USD">USD - US Dollar</option>
            <option value="EUR">EUR - Euro</option>
            <option value="VND">VND - Vietnamese Dong</option>
          </select>
        </Field>
        {error && <div className="notice">{error}</div>}
        <button className="primary-button" disabled={busy}>
          {busy ? "Creating account..." : "Create account"}
          <Plus size={16} />
        </button>
      </form>
    </Modal>
  );
}
