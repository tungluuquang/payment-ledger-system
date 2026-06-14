import { AlertCircle, RefreshCw, Search } from "lucide-react";

export function PageLoader({ label = "Loading your workspace" }) {
  return (
    <div className="page-state">
      <RefreshCw className="spin" size={24} />
      <strong>{label}</strong>
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="page-state error-state">
      <AlertCircle size={25} />
      <strong>Something went wrong</strong>
      <p>{message}</p>
      {onRetry && <button className="secondary-button" onClick={onRetry}>Try again</button>}
    </div>
  );
}

export function EmptyState({ title, description, action, onAction }) {
  return (
    <div className="empty-state">
      <span><Search size={22} /></span>
      <h3>{title}</h3>
      <p>{description}</p>
      {action && <button className="secondary-button" onClick={onAction}>{action}</button>}
    </div>
  );
}
