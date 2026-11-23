// PRUEBA DE ESTRES: sube los usuarios por escalones (25, 50, 100, 150, 200) hasta ver donde el
// sistema se degrada. No busca "aprobar": el objetivo es encontrar el limite de la EC2.
// Solo lecturas, para medir la capacidad sin llenar la base de datos.
import { check, sleep } from 'k6';
import exec from 'k6/execution';
import { get, resumen } from './comun.js';

// Cada escalon: 30 s subiendo + 60 s sostenido = 90 s
const ESCALONES = [25, 50, 100, 150, 200];

export const options = {
  scenarios: {
    escalones: {
      executor: 'ramping-vus',
      stages: ESCALONES.flatMap((n) => [{ duration: '30s', target: n }, { duration: '1m', target: n }])
        .concat([{ duration: '30s', target: 0 }]),
    },
  },
  thresholds: Object.fromEntries(
    ESCALONES.map((n) => [`http_req_duration{escalon:${n}}`, ['p(95)<500']])
      .concat([['http_req_failed', ['rate<0.05']]])),
};

const RUTAS = ['/api/v1/pacientes?size=10', '/api/v1/medicos', '/api/v1/franjas?size=20',
  '/api/v1/medicamentos?nombre=a', '/api/v1/citas?size=10', '/api/v1/formulas?size=10', '/api/v1/notificaciones?size=10'];

export default function () {
  const segundos = exec.instance.currentTestRunDuration / 1000;
  const escalon = String(ESCALONES[Math.min(Math.floor(segundos / 90), ESCALONES.length - 1)]);
  for (const ruta of RUTAS) {
    const r = get(ruta, ruta.split('?')[0], { escalon });
    check(r, { '200': (x) => x.status === 200 });
  }
  sleep(0.5);
}

export function handleSummary(data) {
  return resumen('estres', data);
}
