import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { SideBar } from "../../components/sidebar";
import PolicyFormModal from "./policy-form-modal";
import "../../styles/policies.css";

export default function NewPolicy() {
    const navigate = useNavigate();
    const [open, setOpen] = useState(true);

    const handleSaved = (saved) => {
        navigate(`/policies/${saved.id}`, { replace: true });
    };

    const handleClose = () => {
        setOpen(false);
        navigate("/policies");
    };

    return (
        <main className="policies-container">
            <SideBar />
            <section className="policies-content">
                <div className="policies-empty">
                    <i className="fa-solid fa-shield-halved" />
                    <strong>Nova política</strong>
                    <span>Configure os critérios de segurança que serão aplicados às varreduras.</span>
                </div>
            </section>

            {open && (
                <PolicyFormModal
                    policy={null}
                    onClose={handleClose}
                    onSaved={handleSaved}
                />
            )}
        </main>
    );
}
