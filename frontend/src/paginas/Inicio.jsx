import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'

const SERVICIOS = [
  { nombre: 'ms-pacientes', clave: 'pacientes', texto: 'Pacientes registrados', ruta: '/pacientes', cargar: () => api.pacientes({ size: 1 }) },
  { nombre: 'ms-citas', clave: 'citas', texto: 'Citas agendadas', ruta: '/citas', cargar: () => api.citas({ estado: 'AGENDADA', size: 1 }) },
  { nombre: 'ms-farmacia', clave: 'formulas', texto: 'Fórmulas por entregar', ruta: '/farmacia', cargar: () => api.formulas({ estado: 'EMITIDA', size: 1 }) },
  { nombre: 'ms-notificaciones', clave: 'avisos', texto: 'Avisos fallidos', ruta: '/notificaciones', cargar: () => api.notificaciones({ estado: 'FALLIDA', size: 1 }) },
]

export default function Inicio() {
  const [res, setRes] = useState({})
  useEffect(() => {
    SERVICIOS.forEach((s) => {
      const t0 = performance.now()
      s.cargar()
        .then((d) => setRes((r) => ({ ...r, [s.clave]: { total: d.totalElementos, ms: Math.round(performance.now() - t0) } })))
        .catch(() => setRes((r) => ({ ...r, [s.clave]: { caido: true } })))
    })
  }, [])

  return (
    <>
      <header className="encabezado">
        <div>
          <h1>Panel general</h1>
          <p>Estado de los cuatro microservicios y lo pendiente del día.</p>
        </div>
      </header>

      <section className="tarjetas">
        {SERVICIOS.map((s) => {
          const r = res[s.clave]
          return (
            <Link key={s.clave} to={s.ruta} className="tarjeta">
              <span className="tarjeta-servicio">
                <i className={`punto ${!r ? '' : r.caido ? 'punto-rojo' : 'punto-verde'}`} />
                {s.nombre}
              </span>
              <strong className="tarjeta-numero">{!r ? '…' : r.caido ? '—' : r.total}</strong>
              <span className="tarjeta-texto">{s.texto}</span>
              {r && !r.caido && <span className="tarjeta-pie">Respondió en {r.ms} ms</span>}
              {r?.caido && <span className="tarjeta-pie error">Sin respuesta</span>}
            </Link>
          )
        })}
      </section>

      <section className="panel">
        <h2>Flujo de una atención</h2>
        <ol className="pasos">
          <li><Link to="/pacientes">Registrar al paciente</Link> con su correo y celular.</li>
          <li><Link to="/medicos">Abrir la agenda</Link> de un médico para un día hábil (los festivos se rechazan).</li>
          <li><Link to="/citas">Agendar la cita</Link> con los síntomas: Gemini sugiere especialidad y prioridad.</li>
          <li>El paciente recibe <Link to="/notificaciones">correo y aviso por Telegram</Link> a través de Amazon SQS.</li>
          <li>Marcar la cita como atendida y <Link to="/farmacia">emitir y dispensar la fórmula</Link>.</li>
        </ol>
      </section>
    </>
  )
}
