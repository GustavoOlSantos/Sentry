import {useEffect, useState} from 'react';
import  {useNavigate} from 'react-router-dom';

import '../../styles/home.css';
import { SideBar } from '../../components/sidebar';

function Home() {
    const navigate = useNavigate();
    const [ativos_monitorados, setAtivosMonitorados] = useState(200);
    const [ativos_app, setAtivosApp] = useState(80);
    const [ativos_infr, setAtivosInfr] = useState(120);

    const [vuln_low, setVulLow] = useState(15);
    const [vuln_med, setVulMed] = useState(20);
    const [vuln_high, setVulHi] = useState(30);
    const [vuln_crit, setVulCrut] = useState(2);
    const [vuln_resolv, setVulRes] = useState(33);

    const [vuln_total_chart, setVulnTotalChart] = useState(0);
    const [vuln_gradient, setVulnGradient] = useState(
        'conic-gradient(transparent 0% 100%)'
    );

    useEffect(() => {
        const values = [
            vuln_low,
            vuln_med,
            vuln_high,
            vuln_crit,
            vuln_resolv
        ].map(value => Math.max(0, Number(value) || 0));

        const total = values.reduce((sum, value) => sum + value, 0);

        const colors = [
            'var(--severity-low)',
            'var(--severity-medium)',
            'var(--severity-high)',
            'var(--severity-critical)',
            'var(--severity-info)'
        ];

        let current = 0;

        const segments = values.map((value, index) => {
            const start = current;
            current += total > 0 ? (value / total) * 100 : 0;

            return `${colors[index]} ${start}% ${current}%`;
        });

        setVulnTotalChart(total);
        setVulnGradient(`conic-gradient(${segments.join(', ')})`);
    }, [vuln_low, vuln_med, vuln_high, vuln_crit, vuln_resolv]);

  return (
    <main>
        <section className="home">
            
            <SideBar />

            <article className="overview">
                <h3>Visão geral</h3>

                <div className="overview_content">
                    <article className="overview_stats">
                        <section className="home_cards section assets_card">
                            <p>Ativos monitorados</p>

                            <h3>{ativos_monitorados}</h3>

                            <div className="assets_progress">
                                <span className="assets_progress_infr" style={{ width: `${ativos_infr}%` }} />

                                <span className="assets_progress_app" style={{ width: `${ativos_app}%` }} />
                            </div>
                        </section>

                        <section className="home_cards section">
                            <h4>Distribuição de ativos</h4>

                            <ul>
                                <span>
                                    <li className="blue-bullet">Infraestrutura</li>
                                    <h4>{(ativos_infr/ativos_monitorados) * 100}%</h4>
                                </span>

                                <span>
                                    <li className="purple-bullet">Aplicações</li>
                                    <h4>{(ativos_app/ativos_monitorados) *  100}%</h4>
                                </span>
                            </ul>
                        </section>
                    </article>

                    <section className="home_cards pizza section">
                        <p>Gravidade das vulnerabilidades encontradas</p>

                        <div className="vulnerability_chart">
                            <div className="vulnerability_donut" style={{ background: vuln_gradient }} >
                                <div className="donut_center">
                                    <strong>{vuln_total_chart}</strong>
                                    <p>Vulnerabilidades</p>
                                </div>
                            </div>

                            <ul className="vulnerability_legend">
                                <li>
                                    <span className="legend_color low" />
                                    Baixa <strong>{vuln_low}</strong>
                                </li>

                                <li>
                                    <span className="legend_color medium" />
                                    Média <strong>{vuln_med}</strong>
                                </li>

                                <li>
                                    <span className="legend_color high" />
                                    Alta <strong>{vuln_high}</strong>
                                </li>
                                
                                <li>
                                    <span className="legend_color critical" />
                                    Crítica <strong>{vuln_crit}</strong>
                                </li>

                                <li>
                                    <span className="legend_color resolved" />
                                    Resolvidas <strong>{vuln_resolv}</strong>
                                </li>
                            </ul>
                        </div>
                    </section>
                </div>
                
                <section className='actions'>
                    <button onClick={() => navigate('novo-scan')}className='btn primary'>Nova varredura</button>
                    <button onClick={() => navigate('hosts')} className='btn secondary'>Visualizar ativos</button>
                </section>
            </article>
      </section>
    </main>
  );
}

export default Home;