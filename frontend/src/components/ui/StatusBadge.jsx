const LABELS = {
  COMPLETED: "Completed",
  PROCESSING: "Processing",
  INITIATED: "Initiated",
  COMPENSATING: "Rolling back",
  COMPENSATED: "Rolled back",
  CANCELLED: "Cancelled",
  FAILED: "Failed",
  ACTIVE: "Active",
};

export function StatusBadge({ status }) {
  return (
    <span className={`status ${status?.toLowerCase()}`}>
      {LABELS[status] || status || "Unknown"}
    </span>
  );
}
