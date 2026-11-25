import { useState } from 'react'
import { api, fecha } from '../api'
import { Campo, Etiqueta, EstadoLista, Modal, Paginador, textoError, useAviso, useLista } from '../componentes/comunes'

const VACIO = {
  tipoDocumento: 'CC', numeroDocumento: '', nombres: '', apellidos: '', correo: '', telefono: '',
  fechaNacimiento: '', tipoSangre: 'O+', alergias: '',
}
const SANGRE = ['O+', 'O-', 'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-']

export default function Pacientes() {
  const avisar = useAviso()
  const [busqueda, setBusqueda] = useState('')
  const [nombre, setNombre] = useState('')
  const [editando, setEditando] = useState(null) // null | {codigo?, datos}
  const lista = useLista((page) => api.pacientes({ nombre, page, size: 10 }), [nombre])

  const desactivar = async (p) => {
    if (!window.confirm(`¿Desactivar a ${p.nombres} ${p.apellidos}?`)) return
    try {
      await api.desactivarPaciente(p.codigo)
      avisar(`${p.codigo} desactivado`)
      lista.recargar()
    } catch (e) { avisar(textoError(e), 'error') }
  }

  return (
    <>
      <header className="encabezado">
        <div>
          <h1>Pacientes</h1>
          <p>Registro, consulta, actualización y desactivación (ms-pacientes).</p>
        </div>
        <button className="boton" onClick={() => setEditando({ datos: VACIO })}>Registrar paciente</button>
      </header>

      <form className="filtros" onSubmit={(e) => { e.preventDefault(); setNombre(busqueda.trim()) }}>
        <input placeholder="Buscar por nombre o apellido" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
        <button className="boton boton-sutil">Buscar</button>
      </form>

      <div className="tabla-marco">
        <table>
          <thead>
            <tr><th>Código</th><th>Paciente</th><th>Documento</th><th>Contacto</th><th>Nacimiento</th><th>Estado</th><th /></tr>
          </thead>
          <tbody>
            <EstadoLista {...lista} vacio={lista.datos?.contenido.length === 0} columnas={7} />
            {!lista.cargando && !lista.error && lista.datos?.contenido.map((p) => (
              <tr key={p.codigo}>
                <td className="codigo">{p.codigo}</td>
                <td><strong>{p.nombres} {p.apellidos}</strong><small>Sangre {p.tipoSangre}{p.alergias ? ` · Alergias: ${p.alergias}` : ''}</small></td>
                <td>{p.tipoDocumento} {p.numeroDocumento}</td>
                <td>{p.correo}<small>{p.telefono}</small></td>
                <td>{fecha(p.fechaNacimiento)}</td>
                <td><Etiqueta valor={p.activo ? 'ACTIVO' : 'INACTIVO'} /></td>
                <td className="acciones">
                  <button className="boton boton-sutil" onClick={() => setEditando({ codigo: p.codigo, datos: { ...VACIO, ...p, alergias: p.alergias || '' } })}>Editar</button>
                  {p.activo && <button className="boton boton-peligro" onClick={() => desactivar(p)}>Desactivar</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Paginador pagina={lista.datos} setPagina={lista.setPagina} />

      {editando && (
        <FormularioPaciente inicial={editando} onCerrar={() => setEditando(null)}
          onGuardado={(p, nuevo) => { avisar(`${p.codigo} ${nuevo ? 'registrado' : 'actualizado'}`); setEditando(null); lista.recargar() }} />
      )}
    </>
  )
}

function FormularioPaciente({ inicial, onCerrar, onGuardado }) {
  const [d, setD] = useState(inicial.datos)
  const [error, setError] = useState(null)
  const [enviando, setEnviando] = useState(false)
  const cambiar = (k) => (e) => setD({ ...d, [k]: e.target.value })

  const guardar = async (e) => {
    e.preventDefault()
    setEnviando(true)
    const cuerpo = {
      tipoDocumento: d.tipoDocumento, numeroDocumento: d.numeroDocumento, nombres: d.nombres, apellidos: d.apellidos,
      correo: d.correo, telefono: d.telefono, fechaNacimiento: d.fechaNacimiento, tipoSangre: d.tipoSangre,
      alergias: d.alergias || null,
    }
    try {
      const p = inicial.codigo ? await api.actualizarPaciente(inicial.codigo, cuerpo) : await api.registrarPaciente(cuerpo)
      onGuardado(p, !inicial.codigo)
    } catch (err) { setError(err) } finally { setEnviando(false) }
  }

  return (
    <Modal titulo={inicial.codigo ? `Editar ${inicial.codigo}` : 'Registrar paciente'} onCerrar={onCerrar}>
      <form onSubmit={guardar}>
        <div className="rejilla">
          <Campo etiqueta="Tipo de documento">
            <select value={d.tipoDocumento} onChange={cambiar('tipoDocumento')}>
              <option value="CC">Cédula de ciudadanía</option>
              <option value="TI">Tarjeta de identidad</option>
              <option value="CE">Cédula de extranjería</option>
              <option value="PA">Pasaporte</option>
            </select>
          </Campo>
          <Campo etiqueta="Número de documento"><input required value={d.numeroDocumento} onChange={cambiar('numeroDocumento')} /></Campo>
          <Campo etiqueta="Nombres"><input required value={d.nombres} onChange={cambiar('nombres')} /></Campo>
          <Campo etiqueta="Apellidos"><input required value={d.apellidos} onChange={cambiar('apellidos')} /></Campo>
          <Campo etiqueta="Correo"><input required type="email" value={d.correo} onChange={cambiar('correo')} /></Campo>
          <Campo etiqueta="Celular"><input required value={d.telefono} onChange={cambiar('telefono')} placeholder="3101234567" /></Campo>
          <Campo etiqueta="Fecha de nacimiento"><input required type="date" value={d.fechaNacimiento} onChange={cambiar('fechaNacimiento')} /></Campo>
          <Campo etiqueta="Tipo de sangre">
            <select value={d.tipoSangre} onChange={cambiar('tipoSangre')}>{SANGRE.map((s) => <option key={s}>{s}</option>)}</select>
          </Campo>
          <Campo etiqueta="Alergias (opcional)" ancho><input value={d.alergias} onChange={cambiar('alergias')} /></Campo>
        </div>
        {error && <p className="error-formulario">{textoError(error)}</p>}
        <div className="pie-modal">
          <button type="button" className="boton boton-sutil" onClick={onCerrar}>Cancelar</button>
          <button className="boton" disabled={enviando}>{enviando ? 'Guardando…' : 'Guardar'}</button>
        </div>
      </form>
    </Modal>
  )
}
