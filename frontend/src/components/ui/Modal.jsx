import { X } from "lucide-react";

export function Modal({ title, subtitle, onClose, children }) {
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <section className="modal" onMouseDown={(event) => event.stopPropagation()}>
        <div className="card-heading">
          <div><span className="eyebrow">{subtitle}</span><h2>{title}</h2></div>
          <button className="icon-button" onClick={onClose}><X size={18} /></button>
        </div>
        {children}
      </section>
    </div>
  );
}

export function Field({ label, hint, children }) {
  return (
    <label className="field">
      <span>{label}<small>{hint}</small></span>
      {children}
    </label>
  );
}
