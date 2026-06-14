import { Check, X } from "lucide-react";
import { useEffect } from "react";

export function Toast({ message, onClose }) {
  useEffect(() => {
    const timer = setTimeout(onClose, 4000);
    return () => clearTimeout(timer);
  }, [onClose]);
  return (
    <div className="toast">
      <span><Check size={15} /></span>
      <strong>{message}</strong>
      <button onClick={onClose}><X size={15} /></button>
    </div>
  );
}
