import { useNavigate } from 'react-router-dom';

import LOGO from '../../assets/full-logo.png';

function Header() {
    const navigate = useNavigate();

    return (
        <nav>
            <img src={LOGO} className="header-logo" alt="Logo" onClick={() => navigate("/")} />
        </nav>
    )
}

export default Header;