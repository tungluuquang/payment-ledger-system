const AUTH_BASE =
  import.meta.env.VITE_AUTH_BASE_URL || "http://localhost:9000";
const TOKEN_URL =
  import.meta.env.VITE_AUTH_TOKEN_URL || "/oauth2/token";
const CLIENT_ID =
  import.meta.env.VITE_AUTH_CLIENT_ID || "payment-ledger-spa";
const REDIRECT_URI =
  import.meta.env.VITE_AUTH_REDIRECT_URI ||
  "http://localhost:5173/callback";
const SCOPES = [
  "openid",
  "profile",
  "payment.read",
  "payment.write",
  "account.read",
  "account.write",
  "user.read",
  "user.write",
].join(" ");

const TOKEN_KEY = "ledger-pay.tokens";
const VERIFIER_KEY = "ledger-pay.pkce-verifier";
const STATE_KEY = "ledger-pay.oauth-state";

function base64Url(bytes) {
  return btoa(String.fromCharCode(...bytes))
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replaceAll("=", "");
}

function randomValue(length = 32) {
  const bytes = new Uint8Array(length);
  crypto.getRandomValues(bytes);
  return base64Url(bytes);
}

async function challengeFor(verifier) {
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(verifier),
  );
  return base64Url(new Uint8Array(digest));
}

export async function beginLogin() {
  const verifier = randomValue(64);
  const state = randomValue();
  sessionStorage.setItem(VERIFIER_KEY, verifier);
  sessionStorage.setItem(STATE_KEY, state);

  const params = new URLSearchParams({
    response_type: "code",
    client_id: CLIENT_ID,
    scope: SCOPES,
    redirect_uri: REDIRECT_URI,
    code_challenge: await challengeFor(verifier),
    code_challenge_method: "S256",
    state,
  });
  window.location.assign(`${AUTH_BASE}/oauth2/authorize?${params}`);
}

export async function finishLogin(code, state) {
  if (!code || state !== sessionStorage.getItem(STATE_KEY)) {
    throw new Error("The login response could not be verified.");
  }
  const verifier = sessionStorage.getItem(VERIFIER_KEY);
  const response = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "authorization_code",
      client_id: CLIENT_ID,
      redirect_uri: REDIRECT_URI,
      code,
      code_verifier: verifier,
    }),
  });
  if (!response.ok) {
    throw new Error("Could not exchange the authorization code.");
  }
  const tokens = await response.json();
  saveTokens(tokens);
  sessionStorage.removeItem(VERIFIER_KEY);
  sessionStorage.removeItem(STATE_KEY);
  return tokens;
}

export function saveTokens(tokens) {
  const previous = getTokens() || {};
  localStorage.setItem(
    TOKEN_KEY,
    JSON.stringify({
      ...previous,
      ...tokens,
      expires_at: Date.now() + tokens.expires_in * 1000,
    }),
  );
}

export function getTokens() {
  try {
    return JSON.parse(localStorage.getItem(TOKEN_KEY));
  } catch {
    return null;
  }
}

export function getAccessToken() {
  const tokens = getTokens();
  return tokens?.access_token && tokens.expires_at > Date.now()
    ? tokens.access_token
    : null;
}

export function currentUser() {
  const token = getAccessToken();
  if (!token) return null;
  try {
    const encoded = token.split(".")[1]
      .replaceAll("-", "+")
      .replaceAll("_", "/");
    const padded = encoded.padEnd(
      encoded.length + ((4 - (encoded.length % 4)) % 4),
      "=",
    );
    const payload = JSON.parse(
      atob(padded),
    );
    return {
      id: payload.user_id,
      username: payload.sub,
      roles: payload.roles || [],
    };
  } catch {
    return null;
  }
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY);
  window.location.assign("/");
}
