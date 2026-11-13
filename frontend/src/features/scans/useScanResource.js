import { useCallback, useEffect, useState } from "react";

export const POLL_INTERVAL_MS = 5000;

/**
 * Carrega um recurso da API e o mantém atualizado enquanto `shouldPoll(data)` for verdadeiro
 * (ex.: há varreduras em andamento). A atualização periódica é silenciosa: não volta para o
 * estado de "carregando" nem apaga os dados que já estão na tela.
 *
 * `load` precisa ser estável (useCallback): trocar a função recarrega o recurso.
 */
export function useScanResource(load, shouldPoll) {
    const [data, setData] = useState(null);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);
    const [reloadKey, setReloadKey] = useState(0);

    useEffect(() => {
        let cancelled = false;

        load()
            .then((result) => {
                if (cancelled) return;
                setData(result);
                setError(null);
            })
            .catch((err) => {
                if (!cancelled) setError(err);
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [load, reloadKey]);

    const polling = data !== null && shouldPoll(data);

    useEffect(() => {
        if (!polling) return;

        const timer = setInterval(() => setReloadKey((key) => key + 1), POLL_INTERVAL_MS);
        return () => clearInterval(timer);
    }, [polling]);

    const reload = useCallback(() => {
        setLoading(true);
        setReloadKey((key) => key + 1);
    }, []);

    return { data, error, loading, polling, reload };
}
