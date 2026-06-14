import { ArrowDownLeft, ArrowRight, ArrowUpRight } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { formatDate, formatMoney, shortId } from "../../utils/format";
import { EmptyState } from "../ui/States";
import { StatusBadge } from "../ui/StatusBadge";

export function TransferTable({
  payments,
  accountId,
  emptyAction,
  compact = false,
}) {
  const navigate = useNavigate();
  if (!payments.length) {
    return (
      <EmptyState
        title="No transfers yet"
        description="Your payment activity will appear here."
        action="Send your first transfer"
        onAction={emptyAction}
      />
    );
  }
  return (
    <>
      {!compact && (
        <div className="transaction-header">
          <span>Transfer</span><span>Status</span><span>Amount</span><span />
        </div>
      )}
      <div className="transaction-list">
        {payments.map((payment) => {
          const outgoing = payment.sourceAccountId === accountId;
          const counterparty = outgoing
            ? payment.destinationAccountId
            : payment.sourceAccountId;
          return (
            <button
              className="transaction"
              key={payment.paymentId}
              onClick={() => navigate(`/app/transfers/${payment.paymentId}`)}
            >
              <span className={`transaction-icon ${outgoing ? "outgoing" : "incoming"}`}>
                {outgoing
                  ? <ArrowUpRight size={18} />
                  : <ArrowDownLeft size={18} />}
              </span>
              <span className="transaction-main">
                <strong>{outgoing ? "Transfer sent" : "Transfer received"}</strong>
                <small>{formatDate(payment.updatedAt || payment.initiatedAt)} · {shortId(counterparty)}</small>
              </span>
              <StatusBadge status={payment.paymentStatus} />
              <span className={`amount ${outgoing ? "" : "positive"}`}>
                {outgoing ? "-" : "+"}
                {formatMoney(payment.amount, payment.currency)}
              </span>
              <ArrowRight size={16} className="row-arrow" />
            </button>
          );
        })}
      </div>
    </>
  );
}
