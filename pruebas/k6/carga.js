// PRUEBA DE CARGA: 30 usuarios simultaneos durante 3 minutos, con el uso tipico de recepcion
// (consultar y listar pacientes, medicos, agenda, medicamentos, formulas y avisos; registrar pacientes).
// Meta del proyecto: p95 < 500 ms y menos de 1 % de errores.
// No agenda citas para no gastar la cuota de Gemini ni enviar correos reales.
import { check, sleep, group } from 'k6';
import { get, post, aleatorio, resumen } from './comun.js';

export const options = {
  scenarios: {
    recepcion: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 30 },
        { duration: '3m', target: 30 },
        { duration: '20s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  group('consultas', () => {
    check(get('/api/v1/pacientes?nombre=laura&size=10', 'listar pacientes'), { '200': (r) => r.status === 200 });
    check(get('/api/v1/medicos?especialidad=MEDICINA_GENERAL', 'listar medicos'), { '200': (r) => r.status === 200 });
    check(get('/api/v1/franjas?size=20', 'listar franjas'), { '200': (r) => r.status === 200 });
    check(get('/api/v1/medicamentos?nombre=a', 'buscar medicamentos'), { '200': (r) => r.status === 200 });
    check(get('/api/v1/medicamentos/MDT-0001', 'consultar medicamento'), { '200': (r) => r.status === 200 });
    check(get('/api/v1/formulas?size=10', 'listar formulas'), { '200': (r) => r.status === 200 });
    check(get('/api/v1/notificaciones?size=10', 'listar avisos'), { '200': (r) => r.status === 200 });
  });
  group('registro', () => {
    const doc = '7' + aleatorio(9);
    const r = post('/api/v1/pacientes', {
      tipoDocumento: 'CC', numeroDocumento: doc, nombres: 'Carga', apellidos: 'K6 Prueba',
      correo: `carga.${doc}@correo.com`, fechaNacimiento: '1990-01-01',
    }, 'registrar paciente');
    check(r, { '201': (x) => x.status === 201 });
    if (r.status === 201) {
      check(get(`/api/v1/pacientes/${r.json('codigo')}`, 'consultar paciente'), { '200': (x) => x.status === 200 });
    }
  });
  sleep(1);
}

export function handleSummary(data) {
  return resumen('carga', data);
}
