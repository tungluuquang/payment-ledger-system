import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  Clock3,
  RefreshCw,
  Workflow,
  XCircle,
} from "lucide-react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ErrorState, PageLoader } from "../components/ui/States";
import { analyticsApi } from "../services/api";
import { eventLabel, formatDate, formatMoney, shortId } from "../utils/format";

const windows = [
  { label: "24 hours", value: 24 },
  { label: "7 days", value: 168 },
  { label: "30 days", value: 720 },
];

export function OperationsPage() {
  const [hours, setHours] = useState(24);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async (quiet = false) => {
    if (!quiet) setLoading(true);
    setError("");
    try {
      setData(await analyticsApi.overview(hours));
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  }, [hours]);

  useEffect(() => {
    load();
    const timer = window.setInterval(() => load(true), 30_000);
    return () => window.clearInterval(timer);
  }, [load]);

  const maxTrend = useMemo(
    () => Math.max(
      1,
      ...(data?.trend || []).map((point) => Math.max(
        point.initiated,
        point.completed,
        point.failed + point.cancelled,
      )),
    ),
    [data],
  );

  if (loading && !data) return <PageLoader />;
  if (error && !data) return <ErrorState message={error} onRetry={load} />;

  return (
    <div className="operations-page">
      <section className="operations-toolbar">
        <div>
          <span className="live-indicator"><i /> Event stream active</span>
          <p>Updated {formatDate(data.generatedAt)} · refreshes every 30 seconds</p>
        </div>
        <div className="operations-controls">
          <select value={hours} onChange={(event) => setHours(Number(event.target.value))}>
            {windows.map((window) => (
              <option key={window.value} value={window.value}>{window.label}</option>
            ))}
          </select>
          <button className="secondary-button icon-only" onClick={() => load()}>
            <RefreshCw size={16} className={loading ? "spin" : ""} />
          </button>
        </div>
      </section>

      {error && <div className="notice operations-notice">{error}</div>}

      <section className="operations-kpis">
        <Metric
          icon={Activity}
          label="Payments initiated"
          value={data.initiatedPayments}
          note={`${data.consumedEvents} events consumed`}
          tone="blue"
        />
        <Metric
          icon={CheckCircle2}
          label="Success rate"
          value={`${data.successRate}%`}
          note={`${data.completedPayments} completed`}
          tone="green"
        />
        <Metric
          icon={Clock3}
          label="Average completion"
          value={formatDuration(data.averageCompletionMillis)}
          note={`${data.processingPayments} currently processing`}
          tone="amber"
        />
        <Metric
          icon={XCircle}
          label="Terminal failures"
          value={data.failedPayments + data.cancelledPayments}
          note={`${data.failedPayments} failed · ${data.cancelledPayments} cancelled`}
          tone="red"
        />
      </section>

      <section className="operations-grid">
        <div className="panel operations-trend">
          <div className="panel-heading">
            <div><h2>Payment throughput</h2><p>Initiated, completed, and failed payments by UTC bucket.</p></div>
            <span className="chart-legend"><i className="started" /> Started <i className="settled" /> Settled <i className="failed" /> Failed</span>
          </div>
          <div className="trend-chart">
            {(data.trend || []).map((point) => (
              <div className="trend-column" key={point.bucketStart}>
                <div className="trend-bars">
                  <i className="started" style={{ height: barHeight(point.initiated, maxTrend) }} />
                  <i className="settled" style={{ height: barHeight(point.completed, maxTrend) }} />
                  <i className="failed" style={{ height: barHeight(point.failed + point.cancelled, maxTrend) }} />
                </div>
                <span>{bucketLabel(point.bucketStart, hours)}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="panel operations-volume">
          <div className="panel-heading">
            <div><h2>Initiated volume</h2><p>Gross payment value by currency.</p></div>
            <Workflow size={19} />
          </div>
          <div className="volume-list">
            {Object.entries(data.initiatedVolumeByCurrency || {}).map(([currency, value]) => (
              <div key={currency}>
                <span>{currency}</span>
                <strong>{formatMoney(value, currency)}</strong>
              </div>
            ))}
            {!Object.keys(data.initiatedVolumeByCurrency || {}).length && (
              <p className="muted">No payment volume in this window.</p>
            )}
          </div>
        </div>
      </section>

      <section className="operations-grid lower">
        <div className="panel failure-panel">
          <div className="panel-heading">
            <div><h2>Failure concentration</h2><p>Top failing stages and error codes from the event stream.</p></div>
            <AlertTriangle size={19} />
          </div>
          <div className="failure-breakdown">
            {(data.failureBreakdown || []).map((failure) => (
              <div key={`${failure.stage}-${failure.errorCode}`}>
                <span className="failure-stage">{failure.stage}</span>
                <code>{failure.errorCode}</code>
                <strong>{failure.count}</strong>
              </div>
            ))}
            {!data.failureBreakdown?.length && (
              <p className="muted">No failure events in this window.</p>
            )}
          </div>
        </div>

        <div className="panel incident-panel">
          <div className="panel-heading">
            <div><h2>Recent incidents</h2><p>Latest failure events requiring operational attention.</p></div>
          </div>
          <div className="incident-list">
            {(data.recentFailures || []).map((failure) => (
              <Link to={`/app/transfers/${failure.paymentId}`} key={failure.eventId}>
                <span className="incident-icon"><AlertTriangle size={15} /></span>
                <div>
                  <strong>{eventLabel(failure.eventType)}</strong>
                  <small>{failure.reason || failure.errorCode || "Unspecified failure"}</small>
                  <code>{shortId(failure.paymentId)}</code>
                </div>
                <time>{formatDate(failure.occurredAt)}</time>
              </Link>
            ))}
            {!data.recentFailures?.length && (
              <p className="muted">No recent incidents. The stream is quiet.</p>
            )}
          </div>
        </div>
      </section>
    </div>
  );
}

function Metric({ icon: Icon, label, value, note, tone }) {
  return (
    <article className={`operations-metric ${tone}`}>
      <span><Icon size={19} /></span>
      <div><small>{label}</small><strong>{value}</strong><p>{note}</p></div>
    </article>
  );
}

function formatDuration(milliseconds) {
  if (milliseconds == null) return "N/A";
  if (milliseconds < 1000) return `${milliseconds} ms`;
  if (milliseconds < 60_000) return `${(milliseconds / 1000).toFixed(1)} s`;
  return `${(milliseconds / 60_000).toFixed(1)} min`;
}

function barHeight(value, max) {
  if (!value) return "3px";
  return `${Math.max(8, (value / max) * 150)}px`;
}

function bucketLabel(value, hours) {
  const date = new Date(value);
  return hours > 72
    ? new Intl.DateTimeFormat("en", { month: "short", day: "numeric" }).format(date)
    : new Intl.DateTimeFormat("en", { hour: "numeric" }).format(date);
}
