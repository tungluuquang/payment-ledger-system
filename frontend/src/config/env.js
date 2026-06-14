export const env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || "/api",
  authBaseUrl:
    import.meta.env.VITE_AUTH_BASE_URL || "http://localhost:9000",
  tokenUrl: import.meta.env.VITE_AUTH_TOKEN_URL || "/oauth2/token",
  clientId:
    import.meta.env.VITE_AUTH_CLIENT_ID || "payment-ledger-spa",
  redirectUri:
    import.meta.env.VITE_AUTH_REDIRECT_URI ||
    "http://localhost:5173/callback",
};
