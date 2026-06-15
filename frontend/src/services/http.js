import { env } from "../config/env";
import { refreshAccessToken } from "../auth/oauth";
import {
  clearTokens,
  getTokens,
} from "../auth/tokenStorage";

export class ApiError extends Error {
  constructor(message, status, details) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

async function parseBody(response) {
  if (response.status === 204) return null;
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function getErrorMessage(body) {
  if (typeof body === "string" && body.trim()) return body;
  if (!body || typeof body !== "object") return "Request failed";
  if (typeof body.message === "string" && body.message.trim()) {
    return body.message;
  }
  if (typeof body.detail === "string" && body.detail.trim()) {
    return body.detail;
  }
  if (typeof body.error === "string" && body.error.trim()) {
    return body.error;
  }
  if (body.fieldErrors && typeof body.fieldErrors === "object") {
    const messages = Object.values(body.fieldErrors)
      .filter((message) => typeof message === "string" && message.trim());
    if (messages.length) return messages.join(". ");
  }
  return "Request failed";
}

export async function request(path, options = {}, retry = true) {
  const token = getTokens()?.access_token;
  const response = await fetch(`${env.apiBaseUrl}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (response.status === 401 && retry) {
    const refreshed = await refreshAccessToken();
    if (refreshed) return request(path, options, false);
    clearTokens();
    window.location.assign("/");
  }

  const body = await parseBody(response);
  if (!response.ok) {
    throw new ApiError(
      getErrorMessage(body),
      response.status,
      body,
    );
  }
  return body;
}
