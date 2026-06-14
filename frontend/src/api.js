import { getAccessToken } from "./auth";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "/api";

async function request(path, options = {}) {
  const token = getAccessToken();
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Request failed with status ${response.status}`);
  }
  return response.status === 204 ? null : response.json();
}

export const api = {
  register: (payload) =>
    request("/users", { method: "POST", body: JSON.stringify(payload) }),
  getUser: (userId) => request(`/users/${userId}`),
  getAccount: (accountId) => request(`/accounts/${accountId}`),
  createAccount: (payload) =>
    request("/accounts", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  createPayment: (payload) =>
    request("/payments", {
      method: "POST",
      headers: { "X-Command-Id": crypto.randomUUID() },
      body: JSON.stringify({
        ...payload,
        correlationId: crypto.randomUUID(),
        idempotencyKey: crypto.randomUUID(),
      }),
    }),
  listPayments: (accountId) =>
    request(
      `/payments?size=20${accountId ? `&accountId=${accountId}` : ""}`,
    ),
  getPayment: (paymentId) => request(`/payments/${paymentId}`),
  getTimeline: (paymentId) =>
    request(`/payments/${paymentId}/events?size=50`),
};
