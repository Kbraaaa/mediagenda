// Cliente de las APIs de MediAgenda. Las rutas son relativas: la interfaz y las APIs
// se sirven desde el mismo nginx, asi que no hace falta CORS.

export class ErrorApi extends Error {
  constructor(estado, codigo, mensaje) {
    super(mensaje)
    this.estado = estado
    this.codigo = codigo
  }
}

async function pedir(metodo, ruta, cuerpo) {
  let resp
  try {
    resp = await fetch(ruta, {
      method: metodo,
      headers: cuerpo !== undefined ? { 'Content-Type': 'application/json' } : undefined,
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    })
  } catch {
    throw new ErrorApi(0, 'SIN_CONEXION', 'No se pudo conectar con el servidor.')
  }
  if (resp.status === 204) return null
  const texto = await resp.text()
  let datos = null
  try { datos = texto ? JSON.parse(texto) : null } catch { datos = null }
  if (!resp.ok) {
    throw new ErrorApi(resp.status, datos?.codigo || `HTTP_${resp.status}`,
      datos?.mensaje || `El servidor respondió ${resp.status}.`)
  }
  return datos
}

const consulta = (params) => {
  const q = new URLSearchParams()
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') q.set(k, v)
  })
  const s = q.toString()
  return s ? `?${s}` : ''
}

export const api = {
  // ms-pacientes
  pacientes: (p) => pedir('GET', `/api/v1/pacientes${consulta(p)}`),
  paciente: (c) => pedir('GET', `/api/v1/pacientes/${c}`),
  registrarPaciente: (d) => pedir('POST', '/api/v1/pacientes', d),
  actualizarPaciente: (c, d) => pedir('PUT', `/api/v1/pacientes/${c}`, d),
  desactivarPaciente: (c) => pedir('DELETE', `/api/v1/pacientes/${c}`),

  // ms-citas
  medicos: (p) => pedir('GET', `/api/v1/medicos${consulta(p)}`),
  registrarMedico: (d) => pedir('POST', '/api/v1/medicos', d),
  abrirAgenda: (c, d) => pedir('POST', `/api/v1/medicos/${c}/franjas`, d),
  franjas: (p) => pedir('GET', `/api/v1/franjas${consulta(p)}`),
  citas: (p) => pedir('GET', `/api/v1/citas${consulta(p)}`),
  cita: (c) => pedir('GET', `/api/v1/citas/${c}`),
  agendarCita: (d) => pedir('POST', '/api/v1/citas', d),
  reprogramarCita: (c, idFranja) => pedir('PUT', `/api/v1/citas/${c}/reprogramacion`, { idFranja }),
  cancelarCita: (c) => pedir('PATCH', `/api/v1/citas/${c}/cancelacion`),
  atenderCita: (c) => pedir('PATCH', `/api/v1/citas/${c}/atencion`),

  // ms-farmacia
  medicamentos: (p) => pedir('GET', `/api/v1/medicamentos${consulta(p)}`),
  registrarMedicamento: (d) => pedir('POST', '/api/v1/medicamentos', d),
  reabastecer: (c, cantidad) => pedir('PATCH', `/api/v1/medicamentos/${c}/stock`, { cantidad }),
  formulas: (p) => pedir('GET', `/api/v1/formulas${consulta(p)}`),
  emitirFormula: (d) => pedir('POST', '/api/v1/formulas', d),
  dispensar: (c) => pedir('POST', `/api/v1/formulas/${c}/dispensacion`),
  anularFormula: (c) => pedir('PATCH', `/api/v1/formulas/${c}/anulacion`),

  // ms-notificaciones
  notificaciones: (p) => pedir('GET', `/api/v1/notificaciones${consulta(p)}`),
  reintentar: (c) => pedir('POST', `/api/v1/notificaciones/${c}/reintento`),
  marcarLeida: (c) => pedir('PATCH', `/api/v1/notificaciones/${c}/lectura`),
}

export const ESPECIALIDADES = ['MEDICINA_GENERAL', 'CARDIOLOGIA', 'DERMATOLOGIA', 'PEDIATRIA', 'GINECOLOGIA',
  'ORTOPEDIA', 'NEUROLOGIA', 'PSIQUIATRIA', 'OFTALMOLOGIA', 'OTORRINOLARINGOLOGIA', 'GASTROENTEROLOGIA', 'NEUMOLOGIA']

export const legible = (v) => (v ? v.charAt(0) + v.slice(1).toLowerCase().replaceAll('_', ' ') : '')

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']
export const fecha = (iso) => {
  if (!iso) return '—'
  const d = new Date(iso.length === 10 ? `${iso}T00:00:00` : iso)
  return `${d.getDate()} ${MESES[d.getMonth()]} ${d.getFullYear()}`
}
export const fechaHora = (iso) => {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('es-CO', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' })
}
export const hora = (h) => (h ? h.slice(0, 5) : '')
