export function formatMoney(value, currency = "USD") {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency,
    maximumFractionDigits: currency === "VND" ? 0 : 2,
  }).format(Number(value || 0));
}

export function formatDate(value) {
  if (!value) return "Not available";
  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function shortId(value = "", start = 8, end = 5) {
  return value.length > start + end
    ? `${value.slice(0, start)}...${value.slice(-end)}`
    : value;
}

export function eventLabel(value = "") {
  return value.replace(/([a-z])([A-Z])/g, "$1 $2");
}
