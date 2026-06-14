import { useEffect, useState } from "react";
import { RefreshCw, X } from "lucide-react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";
import { finishLogin } from "../auth/oauth";

export function AuthCallbackPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { syncSession } = useAuth();
  const [error, setError] = useState("");

  useEffect(() => {
    finishLogin(params.get("code"), params.get("state"))
      .then((returnTo) => {
        syncSession();
        navigate(returnTo, { replace: true });
      })
      .catch((callbackError) => setError(callbackError.message));
  }, [navigate, params, syncSession]);

  return (
    <main className="centered">
      <div className="brand-mark">
        {error ? <X /> : <RefreshCw className="spin" />}
      </div>
      <h2>{error ? "Sign in failed" : "Securing your session"}</h2>
      <p>{error || "You will be redirected in a moment."}</p>
    </main>
  );
}
