import { Send } from "lucide-react";
import { useState } from "react";
import { paymentApi } from "../../services/api";
import { shortId } from "../../utils/format";
import { Field, Modal } from "../ui/Modal";

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function TransferModal({ source, onClose, onCreated }) {
  const [form, setForm] = useState({
    destinationAccountId: "",
    amount: "",
    description: "",
  });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    if (!source) {
      setError("Create or select a source account first.");
      return;
    }
    const destinationAccountId = form.destinationAccountId.trim();
    if (!UUID_PATTERN.test(destinationAccountId)) {
      setError(
        "Paste the full destination account UUID, not the shortened account label.",
      );
      return;
    }
    setBusy(true);
    setError("");
    try {
      const response = await paymentApi.create({
        sourceAccountId: source.accountId,
        destinationAccountId,
        amount: form.amount,
        currency: source.currency,
        description: form.description.trim(),
      });
      onCreated(response.paymentId);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      title="Send money"
      subtitle="Create a secure transfer"
      onClose={onClose}
    >
      <form onSubmit={submit} className="stack">
        <Field label="Source account">
          <div className="readonly-field">
            {source
              ? `${source.currency} · ${shortId(source.accountId)}`
              : "No account selected"}
          </div>
        </Field>
        <Field label="Destination account" hint="Full UUID">
          <input
            required
            pattern="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
            title="Paste the full destination account UUID"
            value={form.destinationAccountId}
            onChange={(event) => setForm({
              ...form,
              destinationAccountId: event.target.value,
            })}
            placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
          />
        </Field>
        <div className="two-columns">
          <Field label="Amount">
            <input
              required
              type="number"
              min="0.01"
              step="0.01"
              value={form.amount}
              onChange={(event) => setForm({
                ...form,
                amount: event.target.value,
              })}
              placeholder="0.00"
            />
          </Field>
          <Field label="Currency">
            <div className="readonly-field">{source?.currency || "--"}</div>
          </Field>
        </div>
        <Field label="Description" hint="Optional">
          <input
            maxLength={200}
            value={form.description}
            onChange={(event) => setForm({
              ...form,
              description: event.target.value,
            })}
            placeholder="Transfer note"
          />
        </Field>
        {error && <div className="notice">{error}</div>}
        <button className="primary-button" disabled={busy || !source}>
          {busy ? "Submitting transfer..." : "Review and send"}
          <Send size={16} />
        </button>
      </form>
    </Modal>
  );
}
