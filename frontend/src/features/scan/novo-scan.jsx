import { useEffect, useRef, useState } from "react";
import '../../styles/novo-scan.css';

import api from '../../services/api';
import { SideBar } from "../../components/sidebar";

export function NovoScan() {
  const [endr, setEndr] = useState([1]);
  const refs = useRef([]);
  const [backendStatus, setBackendStatus] = useState('');
  const [response, setResponse] = useState('');
  const [responseStatus, setResponseStatus] = useState('');
  const [error, setError] = useState('');

  function adicionarCampo() {
    setEndr((prev) => [...prev, prev.length + 1]);
  }

  function removerCampo(index) {
    setEndr((prev) => prev.filter((_, i) => i !== index));
    refs.current.splice(index, 1);
  }

  function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setResponse('');

    console.log(backendStatus);

    if(backendStatus == "DOWN"){
        setError("Indisponibilidade detectada no serviço de varredura.");
        return;
    }

    const arrayEnderecos = refs.current.slice(0, endr.length)
    .map((ref) => { 
        const valor = ref?.value ?? "";
        return valor.replace(/,/g, ""); 
    });

    const temEnderecoVazio = arrayEnderecos.some((arr) => !arr || arr.trim() === "");
    if (temEnderecoVazio) {
        setError("Os endereços devem ser preenchidos");
        return;
    }

    let enderecos = arrayEnderecos.toString();

    api.post('/scan/exec', { targets: enderecos }).then(res => {
        setResponse(res.data?.message);
        setResponseStatus(res.data?.status);
    })
    .catch(err => {
        console.error("Erro enviar solicitação de scan:", err);

        const mensagemErro = err.response?.data?.message || err.message || "Erro interno ao iniciar scan.";
        setError(mensagemErro);
    });
  }

  useEffect(() => {
    function checarStatus() {
        api.get('/actuator/health')
        .then(res => {
            console.log("Resposta da API:", res.data);
            setBackendStatus(res?.data?.status)
        })
        .catch(() => setBackendStatus("DOWN"));
    }

    checarStatus();
    const intervalo = setInterval(checarStatus, 5000);
    return () => clearInterval(intervalo);
  }, []);


  return (
    <main className="hosts-page">
        
        <SideBar />

        <section className="host-container">
            <div className="hosts-heading">
                <div>
                    <h2>Hosts a serem escaneados</h2>
                    <p> Informe os endereços dos hosts autorizados para a análise da infraestrutura.</p>
                </div>
            </div>

            <section className="hosts-card">
                <div className="hosts-card-header">
                    <div>
                        <h3>Alvos da varredura</h3>
                        <p>Adicione um host por campo.</p>
                    </div>

                    <span className="hosts-counter">
                        <i className="fa-solid fa-server"></i>
                        {endr.length} {endr.length === 1 ? "host" : "hosts"}
                    </span>
                </div>

                <form onSubmit={handleSubmit}>
                    <div className="hosts-list">
                        {endr.map((campo, index) => (
                        <div className="host-row" key={campo}>
                            <span className="host-index">
                                {String(index + 1).padStart(2, "0")}
                            </span>

                            <div className="host-input-wrapper">
                                <i className="fa-solid fa-terminal"></i>

                                <input type="text" placeholder="Ex.: 192.168.1.10 ou servidor.exemplo.com"
                                    aria-label={`Host ${index + 1}`}
                                    ref={(elemento) => { refs.current[index] = elemento; }}
                                />
                            </div>

                            <button className="btn-remove" type="button" 
                                onClick={() => removerCampo(index)} aria-label={`Remover host ${index + 1}`} 
                                title="Remover host" disabled={endr.length === 1} >
                                <i className="fa-solid fa-trash-can"></i>
                            </button>
                        </div>
                        ))}
                    </div>

                    <div className="hosts-form-footer">
                        <button
                            className="btn-add-host"
                            type="button"
                            onClick={adicionarCampo}
                            >
                            <i className="fa-solid fa-plus"></i>
                            Adicionar host
                        </button>

                        <span className="hosts-hint">
                            <i className="fa-solid fa-circle-info"></i>
                            Informe apenas alvos autorizados.
                        </span>
                    </div>

                    <div className="hosts-divider"></div>

                    <div className="hosts-actions">
                        <span className="hosts-status">
                            <span className={`status-dot ${backendStatus !== 'UP' ? 'error' : ''}`}></span>
                            {backendStatus === 'UP' ? 'Pronto para configurar' : 'Indisponibilidade detectada'}
                        </span>

                        <button className="btn-submit-hosts" type="submit">
                            <i className="fa-solid fa-radar"></i>
                            Iniciar análise
                            <i className="fa-solid fa-arrow-right"></i>
                        </button>
                    </div>
                </form>
                
                {response && responseStatus == "ACCEPTED"? 
                    <div className="request-success-backdrop">
                        <p className="request-success"><i class="fa-solid fa-circle-notch fa-spin"></i> &nbsp;{response}</p> 
                    </div> : '' 
                }

                {response && responseStatus == "PARTIAL"? 
                    <div className="request-warning-backdrop">
                        <p className="request-warning"><i class="fa-solid fa-circle-notch fa-spin"></i> &nbsp;{response}</p> 
                    </div> : '' 
                }
                
                {error ? 
                    <div className="request-error-backdrop">
                        <p className="request-error"> <i className="fa-solid fa-circle-exclamation"></i> &nbsp;{error} </p> 
                    </div>  : '' }
            </section>

            <div className="hosts-security-note">
                <i className="fa-solid fa-shield-halved"></i>
                <span>
                Verifique se você tem autorização para analisar os hosts informados.
                </span>
            </div>
        </section>
    </main>
  );
}

export default NovoScan;