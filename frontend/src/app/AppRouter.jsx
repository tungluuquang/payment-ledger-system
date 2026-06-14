import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "../auth/AuthProvider";
import { AppDataProvider } from "../state/AppDataProvider";
import { AppLayout } from "../components/layout/AppLayout";
import { LandingPage } from "../pages/LandingPage";
import { LoginPage } from "../pages/LoginPage";
import { AuthCallbackPage } from "../pages/AuthCallbackPage";
import { DashboardPage } from "../pages/DashboardPage";
import { AccountsPage } from "../pages/AccountsPage";
import { TransfersPage } from "../pages/TransfersPage";
import { TransferDetailPage } from "../pages/TransferDetailPage";
import { ProfilePage } from "../pages/ProfilePage";
import { NotFoundPage } from "../pages/NotFoundPage";

function ProtectedApp() {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/" replace />;
  return (
    <AppDataProvider>
      <AppLayout />
    </AppDataProvider>
  );
}

export function AppRouter() {
  const { isAuthenticated } = useAuth();
  return (
    <Routes>
      <Route
        path="/"
        element={
          isAuthenticated
            ? <Navigate to="/app" replace />
            : <LandingPage />
        }
      />
      <Route path="/callback" element={<AuthCallbackPage />} />
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/app" replace /> : <LoginPage />}
      />
      <Route path="/app" element={<ProtectedApp />}>
        <Route index element={<DashboardPage />} />
        <Route path="accounts" element={<AccountsPage />} />
        <Route path="transfers" element={<TransfersPage />} />
        <Route
          path="transfers/:paymentId"
          element={<TransferDetailPage />}
        />
        <Route path="profile" element={<ProfilePage />} />
      </Route>
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
