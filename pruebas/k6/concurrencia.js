// PRUEBA DE CONCURRENCIA: 50 pacientes intentan agendar LA MISMA franja al mismo tiempo.
// Regla de negocio: solo uno puede ganarla (201); los demas reciben 409 FRANJA_OCUPADA.
// Lo garantiza la base de datos (UPDATE atomico + indice unico), no un "if" en memoria.
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { post, aleatorio, fechaFutura, resumen } from './comun.js';

const ganadoras = new Counter('citas_ganadas');
const rechazadas = new Counter('citas_rechazadas_409');
const otras = new Counter('respuestas_inesperadas');
const USUARIOS = 50;

export const options = {
  scenarios: {
    misma_franja: { executor: 'per-vu-iterations', vus: USUARIOS, iterations: 1, maxDuration: '1m' },
  },
  thresholds: {
    citas_ganadas: ['count==1'],
    citas_rechazadas_409: [`count==${USUARIOS - 1}`],
    respuestas_inesperadas: ['count==0'],
  },
};

// Antes de la carrera: un medico con una franja y 50 pacientes distintos
export function setup() {
  const s = aleatorio(6);
  const medico = post('/api/v1/medicos', { nombre: `Concurrencia ${s}`, especialidad: 'MEDICINA_GENERAL',
    correo: `concurrencia.${s}@mediagenda.co` }, 'setup').json('codigo');
  let franjas = null;
  for (let d = 3; d < 12 && !franjas; d++) {
    const r = post(`/api/v1/medicos/${medico}/franjas`, { fecha: fechaFutura(d), horaInicio: '07:00',
      horaFin: '07:30', duracionMinutos: 30 }, 'setup');
    if (r.status === 201) franjas = r.json();
  }
  const pacientes = [];
  for (let i = 0; i < USUARIOS; i++) {
    const doc = '8' + s + String(i).padStart(3, '0');
    pacientes.push(post('/api/v1/pacientes', { tipoDocumento: 'CC', numeroDocumento: doc, nombres: 'Paciente',
      apellidos: `Concurrente ${i}`, correo: `concurrente.${doc}@correo.com`, fechaNacimiento: '1990-01-01' },
    'setup').json('codigo'));
  }
  return { idFranja: franjas[0].idFranja, pacientes };
}

export default function (datos) {
  const r = post('/api/v1/citas', { codigoPaciente: datos.pacientes[__VU - 1], idFranja: datos.idFranja,
    sintomas: 'Control general, prueba de concurrencia' }, 'agendar misma franja');
  if (r.status === 201) ganadoras.add(1);
  else if (r.status === 409 && r.json('codigo') === 'FRANJA_OCUPADA') rechazadas.add(1);
  else otras.add(1);
  check(r, { '201 o 409': (x) => x.status === 201 || x.status === 409 });
}

export function handleSummary(data) {
  const salida = resumen('concurrencia', data);
  const m = data.metrics;
  const n = (k) => (m[k] ? m[k].values.count : 0);
  const extra = `Ganaron la franja: ${n('citas_ganadas')} | Rechazadas con 409: ${n('citas_rechazadas_409')} | ` +
    `Respuestas inesperadas: ${n('respuestas_inesperadas')}\n`;
  salida['/scripts/resultados/concurrencia.txt'] += extra;
  salida.stdout += extra;
  return salida;
}
