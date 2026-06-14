import { ArrowLeft, ArrowRight, Check, Clock3, Copy } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { paymentApi } from "../services/api";
import { ErrorState, PageLoader } from "../components/ui/States";
import { StatusBadge } from "../components/ui/StatusBadge";
import { eventLabel, formatDate, formatMoney, shortId } from "../utils/format";

export function TransferDetailPage() {
  const { paymentId } = useParams();
  const [payment, setPayment] = useState(null);
  const [events, setEvents] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([
      paymentApi.get(paymentId),
      paymentApi.timeline(paymentId),
    ]).then(([nextPayment, timeline]) => {
      setPayment(nextPayment);
      setEvents(timeline.content || []);
    }).catch((loadError) => setError(loadError.message));
  }, [paymentId]);

  if (error) return <ErrorState message={error} />;
  if (!payment) return <PageLoader label="Loading transfer detail" />;
  const steps = [
    ["Fraud check", payment.fraudStatus],
    ["Debit source", payment.debitStatus],
    ["Credit destination", payment.creditStatus],
    ["Record ledger", payment.ledgerStatus],
  ];

  return (
    <>
      <Link className="back-link" to="/app/transfers"><ArrowLeft size={15} /> Back to transfers</Link>
      <section className="detail-grid">
        <div className="panel transfer-summary">
          <div className="detail-title-row">
            <div><span className="eyebrow">TRANSFER AMOUNT</span><h2>{formatMoney(payment.amount, payment.currency)}</h2></div>
            <StatusBadge status={payment.paymentStatus} />
          </div>
          <div className="route-box">
            <div><span>From account</span><strong>{shortId(payment.sourceAccountId, 10, 6)}</strong></div>
            <ArrowRight size={18} />
            <div><span>To account</span><strong>{shortId(payment.destinationAccountId, 10, 6)}</strong></div>
          </div>
          <dl className="detail-list">
            <div><dt>Payment ID</dt><dd>{shortId(payment.paymentId, 14, 8)}<button onClick={() => navigator.clipboard.writeText(payment.paymentId)}><Copy size={13} /></button></dd></div>
            <div><dt>Correlation ID</dt><dd>{shortId(payment.correlationId, 14, 8)}</dd></div>
            <div><dt>Last updated</dt><dd>{formatDate(payment.updatedAt || payment.lastEventAt)}</dd></div>
            <div><dt>Last event</dt><dd>{eventLabel(payment.lastEventType)}</dd></div>
          </dl>
          {payment.lastError && <div className="notice">{payment.lastError}</div>}
        </div>
        <div className="panel">
          <div className="panel-heading"><div><h2>Processing steps</h2><p>Saga execution status.</p></div></div>
          <div className="steps">
            {steps.map(([label, status]) => (
              <div className="step" key={label}>
                <span className={`step-dot ${status?.toLowerCase()}`}>{status === "COMPLETED" ? <Check size={13} /> : <Clock3 size={13} />}</span>
                <div><strong>{label}</strong><span>{status?.replaceAll("_", " ")}</span></div>
              </div>
            ))}
          </div>
        </div>
      </section>
      <section className="panel timeline-panel">
        <div className="panel-heading"><div><h2>Event timeline</h2><p>Immutable events observed by the projection service.</p></div></div>
        <div className="event-timeline">
          {events.map((event) => (
            <article key={event.eventId}>
              <span className="event-marker" />
              <div><strong>{eventLabel(event.eventType)}</strong><small>{formatDate(event.occurredAt)}</small></div>
              <code>{shortId(event.eventId, 10, 5)}</code>
            </article>
          ))}
        </div>
      </section>
    </>
  );
}
