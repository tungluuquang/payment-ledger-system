import {
  ArrowLeftRight,
  LayoutDashboard,
  Landmark,
  LogOut,
  Send,
  Settings,
  ShieldCheck,
  WalletCards,
} from "lucide-react";
import { NavLink } from "react-router-dom";
import { useAuth } from "../../auth/AuthProvider";
import { useAppData } from "../../state/AppDataProvider";

const links = [
  { to: "/app", label: "Overview", icon: LayoutDashboard, end: true },
  { to: "/app/accounts", label: "Accounts", icon: WalletCards },
  { to: "/app/transfers", label: "Transfers", icon: ArrowLeftRight },
  { to: "/app/profile", label: "Profile", icon: Settings },
];

export function Sidebar({ onNewTransfer }) {
  const { signOut, user } = useAuth();
  const { profile } = useAppData();
  const displayName = profile?.fullName || user.username;
  return (
    <aside className="sidebar">
      <div className="brand">
        <span className="brand-mark"><Landmark size={21} /></span>
        <span>Ledger<span className="brand-accent">Pay</span></span>
      </div>
      <button className="sidebar-transfer" onClick={onNewTransfer}>
        <Send size={16} /> New transfer
      </button>
      <span className="nav-label">Workspace</span>
      <nav>
        {links.map(({ to, label, icon: Icon, end }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `nav-item ${isActive ? "active" : ""}`
            }
          >
            <Icon size={18} /> {label}
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-help">
        <span><ShieldCheck size={17} /></span>
        <div><strong>Protected workspace</strong><small>OAuth 2.0 secured</small></div>
      </div>
      <div className="sidebar-user">
        <div className="avatar">{displayName?.[0]?.toUpperCase()}</div>
        <div><strong>{displayName}</strong><span>{profile?.email || user.username}</span></div>
        <button className="icon-button" onClick={signOut} title="Sign out">
          <LogOut size={17} />
        </button>
      </div>
    </aside>
  );
}
