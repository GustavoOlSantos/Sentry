import LOGO from '../../assets/full-logo.png';

function Footer() {

    return (
        <footer>
            <span>
                <img src={LOGO} className="footer-logo" alt="Logo" /> 
                <span className="footer-separator">/</span> 
                Gestão de Vulnerabilidades e Compliance
            </span>

            <span className="footer-environment">
            <span /> AMBIENTE ACADÊMICO · TCC @ 2026 </span>
        </footer>
    )
}

export default Footer;