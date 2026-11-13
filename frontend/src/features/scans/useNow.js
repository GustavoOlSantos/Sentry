import { useEffect, useState } from "react";

/** Relógio de 1 segundo, ligado só quando `enabled` — usado no contador de varreduras em andamento. */
export function useNow(enabled) {
    const [now, setNow] = useState(() => Date.now());

    useEffect(() => {
        if (!enabled) return;

        const timer = setInterval(() => setNow(Date.now()), 1000);
        return () => clearInterval(timer);
    }, [enabled]);

    return now;
}
