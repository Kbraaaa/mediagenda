import { useState } from 'react'
import { api, ESPECIALIDADES, fecha, hora, legible } from '../api'
import { Campo, Etiqueta, EstadoLista, Modal, Paginador, textoError, useAviso, useLista } from '../componentes/comunes'

const manana = () => {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  if (d.getDay() === 0) d.setDate(d.getDate() + 1) // domingo -> lunes
  return d.toISOString().slice(0, 10)
}

export default function Medicos() {
  const avisar = useAviso()
  const [especialidad, setEspecialidad] = useState('')
  const [nuevo, setNuevo] = useState(false)
  const [agenda, setAgenda] = useState(null)
  const [franjasDe, setFranjasDe] = useState(null)
  const lista = useLista((page) => api.medicos({ especialidad, page, size: 10 }), [especialidad])

  return (
    <>
      <header className="encabezado">
        <div>
          <h1>Médicos y agenda</h1>
          <p>Registrar médicos y abrir franjas de atención (ms-citas). Los días festivos de Colombia se rechazan.</p>
        </div>
        <button className="boton" onClick={() => setNuevo(true)}>Registrar médico</button>
      </header>

      <div className="filtros">
        <select value={especialidad} onChange={(e) => setEspecialidad(e.target.value)}>
          <option value="">Todas las especialidades</option>
          {ESPECIALIDADES.map((e) => <option key={e} value={e}>{legible(e)}</option>)}
        </select>
      </div>

      <div className="tabla-marco">
        <table>
          <thead><tr><th>Código</th><th>Médico</th><th>Especialidad</th><th>Correo</th><th>Estado</th><th /></tr></thead>
          <tbody>
            <EstadoLista {...lista} vacio={lista.datos?.contenido.length === 0} columnas={6} />
            {!lista.cargando && !lista.error && lista.datos?.contenido.map((m) => (
              <tr key={m.codigo}>
                <td className="codigo">{m.codigo}</td>
                <td><strong>{m.nombre}</strong></td>
                <td>{legible(m.especialidad)}</td>
                <td>{m.correo}</td>
                <td><Etiqueta valor={m.activo ? 'ACTIVO' : 'INACTIVO'} /></td>
                <td className="acciones">
                  <button className="boton boton-sutil" onClick={() => setFranjasDe(m)}>Ver franjas</button>
                  <button className="boton" onClick={() => setAgenda(m)}>Abrir agenda</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Paginador pagina={lista.datos} setPagina={lista.setPagina} />

      {nuevo && <NuevoMedico onCerrar={() => setNuevo(false)}
        onGuardado={(m) => { avisar(`${m.codigo} registrado`); setNuevo(false); lista.recargar() }} />}
      {agenda && <AbrirAgenda medico={agenda} onCerrar={() => setAgenda(null)}
        onGuardado={(n) => { avisar(`Se abrieron ${n} franjas para ${agenda.nombre}`); setAgenda(null) }} />}
      {franjasDe && <VerFranjas medico={franjasDe} onCerrar={() => setFranjasDe(null)} />}
    </>
  )
}

function NuevoMedico({ onCerrar, onGuardado }) {
  const [d, setD] = useState({ nombre: '', especialidad: 'MEDICINA_GENERAL', correo: '' })
  const [error, setError] = useState(null)
  const guardar = async (e) => {
    e.preventDefault()
    try { onGuardado(await api.registrarMedico(d)) } catch (err) { setError(err) }
  }
  return (
    <Modal titulo="Registrar médico" onCerrar={onCerrar}>
      <form onSubmit={guardar}>
        <div className="rejilla">
          <Campo etiqueta="Nombre completo" ancho><input required value={d.nombre} onChange={(e) => setD({ ...d, nombre: e.target.value })} /></Campo>
          <Campo etiqueta="Especialidad">
            <select value={d.especialidad} onChange={(e) => setD({ ...d, especialidad: e.target.value })}>
              {ESPECIALIDADES.map((x) => <option key={x} value={x}>{legible(x)}</option>)}
            </select>
          </Campo>
          <Campo etiqueta="Correo"><input required type="email" value={d.correo} onChange={(e) => setD({ ...d, correo: e.target.value })} /></Campo>
        </div>
        {error && <p className="error-formulario">{textoError(error)}</p>}
        <div className="pie-modal">
          <button type="button" className="boton boton-sutil" onClick={onCerrar}>Cancelar</button>
          <button className="boton">Guardar</button>
        </div>
      </form>
    </Modal>
  )
}

function AbrirAgenda({ medico, onCerrar, onGuardado }) {
  const [d, setD] = useState({ fecha: manana(), horaInicio: '08:00', horaFin: '12:00', duracionMinutos: 30 })
  const [error, setError] = useState(null)
  const guardar = async (e) => {
    e.preventDefault()
    try {
      const franjas = await api.abrirAgenda(medico.codigo, { ...d, duracionMinutos: Number(d.duracionMinutos) })
      onGuardado(franjas.length)
    } catch (err) { setError(err) }
  }
  return (
    <Modal titulo={`Abrir agenda · ${medico.nombre}`} onCerrar={onCerrar}>
      <form onSubmit={guardar}>
        <div className="rejilla">
          <Campo etiqueta="Fecha"><input required type="date" value={d.fecha} onChange={(e) => setD({ ...d, fecha: e.target.value })} /></Campo>
          <Campo etiqueta="Duración de cada cita">
            <select value={d.duracionMinutos} onChange={(e) => setD({ ...d, duracionMinutos: e.target.value })}>
              {[15, 20, 30, 45, 60].map((m) => <option key={m} value={m}>{m} minutos</option>)}
            </select>
          </Campo>
          <Campo etiqueta="Desde"><input required type="time" value={d.horaInicio} onChange={(e) => setD({ ...d, horaInicio: e.target.value })} /></Campo>
          <Campo etiqueta="Hasta"><input required type="time" value={d.horaFin} onChange={(e) => setD({ ...d, horaFin: e.target.value })} /></Campo>
        </div>
        <p className="nota">ms-citas consulta Nager.Date: si la fecha es festivo en Colombia, la agenda no se abre.</p>
        {error && <p className="error-formulario">{textoError(error)}</p>}
        <div className="pie-modal">
          <button type="button" className="boton boton-sutil" onClick={onCerrar}>Cancelar</button>
          <button className="boton">Abrir agenda</button>
        </div>
      </form>
    </Modal>
  )
}

function VerFranjas({ medico, onCerrar }) {
  const lista = useLista((page) => api.franjas({ medico: medico.codigo, page, size: 12 }), [])
  return (
    <Modal titulo={`Franjas disponibles · ${medico.nombre}`} onCerrar={onCerrar}>
      <table>
        <thead><tr><th>Fecha</th><th>Hora</th><th>Estado</th></tr></thead>
        <tbody>
          <EstadoLista {...lista} vacio={lista.datos?.contenido.length === 0} columnas={3} />
          {!lista.cargando && !lista.error && lista.datos?.contenido.map((f) => (
            <tr key={f.idFranja}><td>{fecha(f.fecha)}</td><td>{hora(f.horaInicio)} – {hora(f.horaFin)}</td><td><Etiqueta valor={f.estado} /></td></tr>
          ))}
        </tbody>
      </table>
      <Paginador pagina={lista.datos} setPagina={lista.setPagina} />
    </Modal>
  )
}
