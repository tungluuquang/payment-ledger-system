import { env } from "../config/env";
import {
  clearTokens,
  getTokens,
  saveTokens,
} from "./tokenStorage";

const VERIFIER_KEY = "ledger-pay.pkce-verifier";
const STATE_KEY = "ledger-pay.oauth-state";
const RETURN_TO_KEY = "ledger-pay.return-to";
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

export async function beginLogin(returnTo = "/app") {
  const verifier = randomValue(64);
  const state = randomValue();
  sessionStorage.setItem(VERIFIER_KEY, verifier);
  sessionStorage.setItem(STATE_KEY, state);
  sessionStorage.setItem(RETURN_TO_KEY, returnTo);
  const params = new URLSearchParams({
    response_type: "code",
    client_id: env.clientId,
    scope: SCOPES,
    redirect_uri: env.redirectUri,
    code_challenge: await challengeFor(verifier),
    code_challenge_method: "S256",
    state,
  });
  window.location.assign(
    `${env.authBaseUrl}/oauth2/authorize?${params}`,
  );
}

async function tokenRequest(params) {
  const response = await fetch(env.tokenUrl, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      client_id: env.clientId,
      ...params,
    }),
  });
  if (!response.ok) {
    throw new Error("Your session could not be created.");
  }
  const tokens = await response.json();
  saveTokens(tokens);
  return tokens;
}

export async function finishLogin(code, state) {
  if (!code || state !== sessionStorage.getItem(STATE_KEY)) {
    throw new Error("The login response could not be verified.");
  }
  await tokenRequest({
    grant_type: "authorization_code",
    redirect_uri: env.redirectUri,
    code,
    code_verifier: sessionStorage.getItem(VERIFIER_KEY),
  });
  const returnTo = sessionStorage.getItem(RETURN_TO_KEY) || "/app";
  sessionStorage.removeItem(VERIFIER_KEY);
  sessionStorage.removeItem(STATE_KEY);
  sessionStorage.removeItem(RETURN_TO_KEY);
  return returnTo;
}

export async function refreshAccessToken() {
  const refreshToken = getTokens()?.refresh_token;
  if (!refreshToken) return null;
  try {
    return await tokenRequest({
      grant_type: "refresh_token",
      refresh_token: refreshToken,
    });
  } catch {
    clearTokens();
    return null;
  }
}
