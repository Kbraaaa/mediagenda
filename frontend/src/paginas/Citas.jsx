import { useEffect, useState } from 'react'
import { api, fecha, fechaHora, hora, legible } from '../api'
import { Etiqueta, EstadoLista, Modal, Paginador, textoError, useAviso, useLista } from '../componentes/comunes'

export default function Citas() {
  const avisar = useAviso()
  const [estado, setEstado] = useState('')
  const [agendar, setAgendar] = useState(false)
  const [detalle, setDetalle] = useState(null)
  const [reprogramar, setReprogramar] = useState(null)
  const lista = useLista((page) => api.citas({ estado, page, size: 10 }), [estado])

  const accion = async (fn, c, texto) => {
    try { await fn(c.codigo); avisar(`${c.codigo} ${texto}`); lista.recargar() } catch (e) { avisar(textoError(e), 'error') }
  }

  return (
    <>
      <header className="encabezado">
        <div>
          <h1>Citas</h1>
          <p>Agendar, reprogramar, cancelar y atender (ms-citas). Cada cambio publica un evento en Amazon SQS.</p>
        </div>
        <button className="boton" onClick={() => setAgendar(true)}>Agendar cita</button>
      </header>

      <div className="filtros">
        <select value={estado} onChange={(e) => setEstado(e.target.value)}>
          <option value="">Todos los estados</option>
          <option value="AGENDADA">Agendadas</option>
          <option value="ATENDIDA">Atendidas</option>
          <option value="CANCELADA">Canceladas</option>
        </select>
      </div>

      <div className="tabla-marco">
        <table>
          <thead><tr><th>Código</th><th>Paciente</th><th>Médico</th><th>Fecha</th><th>Triage</th><th>Estado</th><th /></tr></thead>
          <tbody>
            <EstadoLista {...lista} vacio={lista.datos?.contenido.length === 0} columnas={7} />
            {!lista.cargando && !lista.error && lista.datos?.contenido.map((c) => (
              <tr key={c.codigo}>
                <td className="codigo"><button className="enlace-texto" onClick={() => setDetalle(c.codigo)}>{c.codigo}</button></td>
                <td><strong>{c.nombrePaciente}</strong><small>{c.codigoPaciente}</small></td>
                <td>{c.franja?.medico}<small>{legible(c.franja?.especialidad)}</small></td>
                <td>{fecha(c.franja?.fecha)}<small>{hora(c.franja?.horaInicio)}</small></td>
                <td>{c.triage?.prioridad ? <Etiqueta valor={c.triage.prioridad} /> : <Etiqueta valor={c.triage?.estado} />}</td>
                <td><Etiqueta valor={c.estado} /></td>
                <td className="acciones">
                  {c.estado === 'AGENDADA' && <>
                    <button className="boton" onClick={() => accion(api.atenderCita, c, 'atendida')}>Atender</button>
                    <button className="boton boton-sutil" onClick={() => setReprogramar(c)}>Reprogramar</button>
                    <button className="boton boton-peligro" onClick={() => window.confirm(`¿Cancelar ${c.codigo}?`) && accion(api.cancelarCita, c, 'cancelada')}>Cancelar</button>
                  </>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Paginador pagina={lista.datos} setPagina={lista.setPagina} />

      {agendar && <AgendarCita onCerrar={() => { setAgendar(false); lista.recargar() }}
        onAgendada={(c) => { avisar(`${c.codigo} agendada`); setAgendar(false); lista.recargar(); setDetalle(c.codigo) }} />}
      {detalle && <DetalleCita codigo={detalle} onCerrar={() => { setDetalle(null); lista.recargar() }} />}
      {reprogramar && <Reprogramar cita={reprogramar} onCerrar={() => setReprogramar(null)}
        onHecho={() => { avisar(`${reprogramar.codigo} reprogramada`); setReprogramar(null); lista.recargar() }} />}
    </>
  )
}

/* Paso a paso: paciente -> medico -> franja -> sintomas */
function AgendarCita({ onCerrar, onAgendada }) {
  const [busqueda, setBusqueda] = useState('')
  const [pacientes, setPacientes] = useState([])
  const [paciente, setPaciente] = useState(null)
  const [medicos, setMedicos] = useState([])
  const [medico, setMedico] = useState('')
  const [franjas, setFranjas] = useState(null)
  const [franja, setFranja] = useState(null)
  const [sintomas, setSintomas] = useState('')
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => { api.medicos({ size: 50 }).then((d) => setMedicos(d.contenido.filter((m) => m.activo))).catch(setError) }, [])
  useEffect(() => {
    const t = setTimeout(() => {
      api.pacientes({ nombre: busqueda, size: 6 }).then((d) => setPacientes(d.contenido.filter((p) => p.activo))).catch(setError)
    }, 250)
    return () => clearTimeout(t)
  }, [busqueda])
  useEffect(() => {
    setFranja(null)
    if (!medico) { setFranjas(null); return }
    api.franjas({ medico, size: 40 }).then((d) => setFranjas(d.contenido)).catch(setError)
  }, [medico])

  const enviar = async (e) => {
    e.preventDefault()
    setEnviando(true); setError(null)
    try {
      onAgendada(await api.agendarCita({ codigoPaciente: paciente.codigo, idFranja: franja.idFranja, sintomas }))
    } catch (err) { setError(err) } finally { setEnviando(false) }
  }

  const porDia = (franjas || []).reduce((acc, f) => ((acc[f.fecha] ||= []).push(f), acc), {})

  return (
    <Modal titulo="Agendar cita" onCerrar={onCerrar}>
      <form onSubmit={enviar}>
        <h3 className="paso">1. Paciente</h3>
        {paciente ? (
          <div className="elegido">
            <span><strong>{paciente.nombres} {paciente.apellidos}</strong> · {paciente.codigo}</span>
            <button type="button" className="boton boton-sutil" onClick={() => setPaciente(null)}>Cambiar</button>
          </div>
        ) : (
          <>
            <input placeholder="Buscar paciente por nombre" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} autoFocus />
            <div className="opciones">
              {pacientes.map((p) => (
                <button type="button" key={p.codigo} className="opcion" onClick={() => setPaciente(p)}>
                  <strong>{p.nombres} {p.apellidos}</strong><small>{p.codigo} · {p.tipoDocumento} {p.numeroDocumento}</small>
                </button>
              ))}
              {pacientes.length === 0 && <p className="nota">Sin resultados.</p>}
            </div>
          </>
        )}

        <h3 className="paso">2. Médico y franja</h3>
        <select value={medico} onChange={(e) => setMedico(e.target.value)} required>
          <option value="">Elige un médico</option>
          {medicos.map((m) => <option key={m.codigo} value={m.codigo}>{m.nombre} · {legible(m.especialidad)}</option>)}
        </select>
        {franjas && franjas.length === 0 && <p className="nota">Este médico no tiene franjas disponibles. Ábrelas en «Médicos y agenda».</p>}
        {Object.entries(porDia).map(([dia, fs]) => (
          <div key={dia} className="dia">
            <span className="dia-titulo">{fecha(dia)}</span>
            <div className="horas">
              {fs.map((f) => (
                <button type="button" key={f.idFranja}
                  className={`hora ${franja?.idFranja === f.idFranja ? 'hora-elegida' : ''}`}
                  onClick={() => setFranja(f)}>{hora(f.horaInicio)}</button>
              ))}
            </div>
          </div>
        ))}

        <h3 className="paso">3. Síntomas</h3>
        <textarea rows={3} required value={sintomas} onChange={(e) => setSintomas(e.target.value)}
          placeholder="Ej.: dolor de pecho al subir escaleras desde hace dos días" />
        <p className="nota">Gemini analiza los síntomas en segundo plano y sugiere especialidad y prioridad.</p>

        {error && <p className="error-formulario">{textoError(error)}</p>}
        <div className="pie-modal">
          <button type="button" className="boton boton-sutil" onClick={onCerrar}>Cancelar</button>
          <button className="boton" disabled={!paciente || !franja || !sintomas.trim() || enviando}>
            {enviando ? 'Agendando…' : 'Agendar'}
          </button>
        </div>
      </form>
    </Modal>
  )
}

function DetalleCita({ codigo, onCerrar }) {
  const [c, setC] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => {
    let vivo = true
    let intentos = 0
    const cargar = () => api.cita(codigo).then((d) => {
      if (!vivo) return
      setC(d)
      // El triage corre en segundo plano: se consulta de nuevo mientras siga pendiente.
      if (d.triage?.estado === 'PENDIENTE' && intentos++ < 15) setTimeout(cargar, 2000)
    }).catch((e) => vivo && setError(e))
    cargar()
    return () => { vivo = false }
  }, [codigo])

  return (
    <Modal titulo={`Cita ${codigo}`} onCerrar={onCerrar}>
      {error && <p className="error-formulario">{textoError(error)}</p>}
      {!c && !error && <p className="nota">Cargando…</p>}
      {c && (
        <>
          <dl className="ficha">
            <dt>Estado</dt><dd><Etiqueta valor={c.estado} /></dd>
            <dt>Paciente</dt><dd>{c.nombrePaciente} · {c.codigoPaciente}</dd>
            <dt>Contacto</dt><dd>{c.correoPaciente}{c.telefonoPaciente ? ` · ${c.telefonoPaciente}` : ''}</dd>
            <dt>Médico</dt><dd>{c.franja?.medico} · {legible(c.franja?.especialidad)}</dd>
            <dt>Fecha</dt><dd>{fecha(c.franja?.fecha)}, {hora(c.franja?.horaInicio)} – {hora(c.franja?.horaFin)}</dd>
            <dt>Síntomas</dt><dd>{c.sintomas}</dd>
            <dt>Creada</dt><dd>{fechaHora(c.fechaCreacion)}</dd>
          </dl>
          <div className="triage">
            <div className="triage-titulo">
              <strong>Triage con IA</strong>
              <Etiqueta valor={c.triage?.estado} />
            </div>
            {c.triage?.estado === 'PENDIENTE' && <p className="nota">Gemini está analizando los síntomas…</p>}
            {c.triage?.especialidadSugerida && (
              <p>Especialidad sugerida: <strong>{legible(c.triage.especialidadSugerida)}</strong> · Prioridad <Etiqueta valor={c.triage.prioridad} /></p>
            )}
            {c.triage?.motivo && <p className="nota">{c.triage.motivo}</p>}
          </div>
        </>
      )}
    </Modal>
  )
}

function Reprogramar({ cita, onCerrar, onHecho }) {
  const [franjas, setFranjas] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => {
    api.franjas({ medico: cita.franja?.codigoMedico, size: 40 }).then((d) => setFranjas(d.contenido)).catch(setError)
  }, [cita])
  const elegir = async (f) => {
    try { await api.reprogramarCita(cita.codigo, f.idFranja); onHecho() } catch (e) { setError(e) }
  }
  return (
    <Modal titulo={`Reprogramar ${cita.codigo}`} onCerrar={onCerrar}>
      <p className="nota">Franjas libres de {cita.franja?.medico}:</p>
      {franjas?.length === 0 && <p className="nota">No hay franjas disponibles.</p>}
      <div className="horas">
        {franjas?.map((f) => (
          <button key={f.idFranja} className="hora" onClick={() => elegir(f)}>{fecha(f.fecha)} {hora(f.horaInicio)}</button>
        ))}
      </div>
      {error && <p className="error-formulario">{textoError(error)}</p>}
    </Modal>
  )
}
