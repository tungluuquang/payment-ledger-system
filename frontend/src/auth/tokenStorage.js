const TOKEN_KEY = "ledger-pay.tokens";

export function getTokens() {
  try {
    return JSON.parse(localStorage.getItem(TOKEN_KEY));
  } catch {
    return null;
  }
}

export function saveTokens(tokens) {
  const previous = getTokens() || {};
  localStorage.setItem(
    TOKEN_KEY,
    JSON.stringify({
      ...previous,
      ...tokens,
      expires_at: Date.now() + Number(tokens.expires_in || 0) * 1000,
    }),
  );
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY);
}

export function decodeAccessToken(token) {
  if (!token) return null;
  try {
    const encoded = token.split(".")[1]
      .replaceAll("-", "+")
      .replaceAll("_", "/");
    const padded = encoded.padEnd(
      encoded.length + ((4 - (encoded.length % 4)) % 4),
      "=",
    );
    return JSON.parse(atob(padded));
  } catch {
    return null;
  }
}
