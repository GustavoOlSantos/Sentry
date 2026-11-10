import { useNavigate } from 'react-router-dom';

import LOGO from '../../assets/full-logo.png';
import UserMenu from '../user-menu';

function Header() {
    const navigate = useNavigate();

    return (
        <nav>
            <img src={LOGO} className="header-logo" alt="Logo" onClick={() => navigate("/")} />

            <UserMenu />
        </nav>
    )
}

export default Header;