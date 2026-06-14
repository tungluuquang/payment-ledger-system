import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { useAuth } from "../auth/AuthProvider";
import { accountApi, paymentApi, userApi } from "../services/api";

const AppDataContext = createContext(null);
const ACCOUNT_KEY = "ledger-pay.selected-account";

export function AppDataProvider({ children }) {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [accounts, setAccounts] = useState([]);
  const [selectedAccountId, setSelectedAccountId] = useState(
    localStorage.getItem(ACCOUNT_KEY) || "",
  );
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const selectedAccount = useMemo(
    () => accounts.find(
      (account) => account.accountId === selectedAccountId,
    ) || accounts[0] || null,
    [accounts, selectedAccountId],
  );

  const loadPayments = useCallback(async (accountId) => {
    if (!accountId) {
      setPayments([]);
      return;
    }
    const page = await paymentApi.list({ accountId, size: 50 });
    setPayments(page.content || []);
  }, []);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const [nextProfile, accountPage] = await Promise.all([
        userApi.get(user.id),
        accountApi.list(),
      ]);
      const nextAccounts = accountPage.content || [];
      const preferred = nextAccounts.some(
        (account) => account.accountId === selectedAccountId,
      )
        ? selectedAccountId
        : nextAccounts[0]?.accountId || "";
      setProfile(nextProfile);
      setAccounts(nextAccounts);
      setSelectedAccountId(preferred);
      if (preferred) localStorage.setItem(ACCOUNT_KEY, preferred);
      await loadPayments(preferred);
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setLoading(false);
    }
  }, [loadPayments, selectedAccountId, user.id]);

  useEffect(() => {
    refresh();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const selectAccount = useCallback(async (accountId) => {
    setSelectedAccountId(accountId);
    localStorage.setItem(ACCOUNT_KEY, accountId);
    setLoading(true);
    try {
      await loadPayments(accountId);
    } finally {
      setLoading(false);
    }
  }, [loadPayments]);

  const createAccount = useCallback(async (payload) => {
    const account = await accountApi.create(payload);
    setAccounts((current) => [account, ...current]);
    await selectAccount(account.accountId);
    return account;
  }, [selectAccount]);

  const value = useMemo(() => ({
    profile,
    setProfile,
    accounts,
    selectedAccount,
    selectedAccountId,
    selectAccount,
    payments,
    loading,
    error,
    setError,
    refresh,
    createAccount,
    refreshPayments: () => loadPayments(selectedAccount?.accountId),
  }), [
    accounts,
    createAccount,
    error,
    loadPayments,
    loading,
    payments,
    profile,
    refresh,
    selectAccount,
    selectedAccount,
    selectedAccountId,
  ]);

  return (
    <AppDataContext.Provider value={value}>
      {children}
    </AppDataContext.Provider>
  );
}

export function useAppData() {
  const context = useContext(AppDataContext);
  if (!context) {
    throw new Error("useAppData must be used within AppDataProvider");
  }
  return context;
}
