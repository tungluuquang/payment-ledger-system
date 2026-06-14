import { KeyRound, Save, UserRound } from "lucide-react";
import { useEffect, useState } from "react";
import { userApi } from "../services/api";
import { useAppData } from "../state/AppDataProvider";
import { Field } from "../components/ui/Modal";

export function ProfilePage() {
  const { profile, setProfile } = useAppData();
  const [details, setDetails] = useState({ fullName: "", email: "" });
  const [password, setPassword] = useState({ currentPassword: "", newPassword: "" });
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (profile) setDetails({ fullName: profile.fullName, email: profile.email });
  }, [profile]);

  async function updateDetails(event) {
    event.preventDefault();
    setBusy(true);
    try {
      const updated = await userApi.update(profile.userId, details);
      setProfile(updated);
      setNotice("Profile updated.");
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy(false);
    }
  }

  async function changePassword(event) {
    event.preventDefault();
    setBusy(true);
    try {
      await userApi.changePassword(profile.userId, password);
      setPassword({ currentPassword: "", newPassword: "" });
      setNotice("Password changed.");
    } catch (error) {
      setNotice(error.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      {notice && <div className="notice profile-notice">{notice}</div>}
      <section className="profile-grid">
        <form className="panel profile-card" onSubmit={updateDetails}>
          <div className="profile-card-heading"><span><UserRound size={20} /></span><div><h2>Personal details</h2><p>Update your name and contact email.</p></div></div>
          <div className="stack">
            <Field label="Username"><div className="readonly-field">{profile?.username}</div></Field>
            <Field label="Full name"><input required value={details.fullName} onChange={(e) => setDetails({ ...details, fullName: e.target.value })} /></Field>
            <Field label="Email address"><input required type="email" value={details.email} onChange={(e) => setDetails({ ...details, email: e.target.value })} /></Field>
            <button className="primary-button" disabled={busy}><Save size={16} /> Save changes</button>
          </div>
        </form>
        <form className="panel profile-card" onSubmit={changePassword}>
          <div className="profile-card-heading"><span><KeyRound size={20} /></span><div><h2>Security</h2><p>Use a password with at least 12 characters.</p></div></div>
          <div className="stack">
            <Field label="Current password"><input required type="password" value={password.currentPassword} onChange={(e) => setPassword({ ...password, currentPassword: e.target.value })} /></Field>
            <Field label="New password"><input required type="password" minLength={12} value={password.newPassword} onChange={(e) => setPassword({ ...password, newPassword: e.target.value })} /></Field>
            <button className="secondary-button profile-submit" disabled={busy}>Change password</button>
          </div>
        </form>
      </section>
    </>
  );
}
