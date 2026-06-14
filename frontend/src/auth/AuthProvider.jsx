import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
} from "react";
import { beginLogin } from "./oauth";
import {
  clearTokens,
  decodeAccessToken,
  getTokens,
} from "./tokenStorage";

const AuthContext = createContext(null);

function readUser() {
  const payload = decodeAccessToken(getTokens()?.access_token);
  if (!payload?.user_id) return null;
  return {
    id: payload.user_id,
    username: payload.sub,
    roles: payload.roles || [],
  };
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser);

  const syncSession = useCallback(() => {
    const nextUser = readUser();
    setUser(nextUser);
    return nextUser;
  }, []);

  const signOut = useCallback(() => {
    clearTokens();
    setUser(null);
    window.location.assign("/");
  }, []);

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      login: beginLogin,
      signOut,
      syncSession,
    }),
    [user, signOut, syncSession],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
