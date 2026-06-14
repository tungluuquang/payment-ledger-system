import { request } from "./http";

const json = (method, payload) => ({
  method,
  body: JSON.stringify(payload),
});

export const userApi = {
  register: (payload) => request("/users", json("POST", payload)),
  get: (userId) => request(`/users/${userId}`),
  update: (userId, payload) =>
    request(`/users/${userId}`, json("PATCH", payload)),
  changePassword: (userId, payload) =>
    request(`/users/${userId}/password`, json("POST", payload)),
};

export const accountApi = {
  list: (page = 0, size = 50) =>
    request(`/accounts?page=${page}&size=${size}`),
  get: (accountId) => request(`/accounts/${accountId}`),
  create: (payload) => request("/accounts", json("POST", payload)),
};

export const paymentApi = {
  list: ({ accountId, status, page = 0, size = 20 } = {}) => {
    const params = new URLSearchParams({ page, size });
    if (accountId) params.set("accountId", accountId);
    if (status) params.set("status", status);
    return request(`/payments?${params}`);
  },
  get: (paymentId) => request(`/payments/${paymentId}`),
  timeline: (paymentId) =>
    request(`/payments/${paymentId}/events?size=100`),
  create: (payload) =>
    request("/payments", {
      ...json("POST", {
        ...payload,
        correlationId: crypto.randomUUID(),
        idempotencyKey: crypto.randomUUID(),
      }),
      headers: { "X-Command-Id": crypto.randomUUID() },
    }),
};
