import { useEffect, useState } from 'react'
import { api, fecha, fechaHora } from '../api'
import { Campo, Etiqueta, EstadoLista, Modal, Paginador, textoError, useAviso, useLista } from '../componentes/comunes'

export default function Farmacia() {
  const [pestana, setPestana] = useState('formulas')
  return (
    <>
      <header className="encabezado">
        <div>
          <h1>Farmacia</h1>
          <p>Fórmulas e inventario (ms-farmacia). Solo se formula a citas atendidas y la dispensación descuenta el stock.</p>
        </div>
      </header>
      <div className="pestanas" role="tablist">
        <button role="tab" aria-selected={pestana === 'formulas'} className={pestana === 'formulas' ? 'activa' : ''} onClick={() => setPestana('formulas')}>Fórmulas</button>
        <button role="tab" aria-selected={pestana === 'medicamentos'} className={pestana === 'medicamentos' ? 'activa' : ''} onClick={() => setPestana('medicamentos')}>Medicamentos</button>
      </div>
      {pestana === 'formulas' ? <Formulas /> : <Medicamentos />}
    </>
  )
}

function Formulas() {
  const avisar = useAviso()
  const [estado, setEstado] = useState('')
  const [emitir, setEmitir] = useState(false)
  const lista = useLista((page) => api.formulas({ estado, page, size: 10 }), [estado])

  const accion = async (fn, f, texto) => {
    try { await fn(f.codigo); avisar(`${f.codigo} ${texto}`); lista.recargar() } catch (e) { avisar(textoError(e), 'error') }
  }

  return (
    <>
      <div className="filtros">
        <select value={estado} onChange={(e) => setEstado(e.target.value)}>
          <option value="">Todos los estados</option>
          <option value="EMITIDA">Por entregar</option>
          <option value="DISPENSADA">Dispensadas</option>
          <option value="ANULADA">Anuladas</option>
        </select>
        <span className="espacio" />
        <button className="boton" onClick={() => setEmitir(true)}>Emitir fórmula</button>
      </div>
      <div className="tabla-marco">
        <table>
          <thead><tr><th>Código</th><th>Paciente</th><th>Cita</th><th>Medicamentos</th><th>Emitida</th><th>Estado</th><th /></tr></thead>
          <tbody>
            <EstadoLista {...lista} vacio={lista.datos?.contenido.length === 0} columnas={7} />
            {!lista.cargando && !lista.error && lista.datos?.contenido.map((f) => (
              <tr key={f.codigo}>
                <td className="codigo">{f.codigo}</td>
                <td><strong>{f.nombrePaciente}</strong><small>{f.codigoPaciente}</small></td>
                <td className="codigo">{f.codigoCita}</td>
                <td>{f.items.map((i) => <div key={i.codigoMedicamento}>{i.cantidad} × {i.medicamento}<small>{i.indicaciones}</small></div>)}</td>
                <td>{fecha(f.fechaEmision)}{f.fechaDispensacion && <small>Entregada {fechaHora(f.fechaDispensacion)}</small>}</td>
                <td><Etiqueta valor={f.estado} /></td>
                <td className="acciones">
                  {f.estado === 'EMITIDA' && <>
                    <button className="boton" onClick={() => accion(api.dispensar, f, 'dispensada')}>Dispensar</button>
                    <button className="boton boton-peligro" onClick={() => window.confirm(`¿Anular ${f.codigo}?`) && accion(api.anularFormula, f, 'anulada')}>Anular</button>
                  </>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Paginador pagina={lista.datos} setPagina={lista.setPagina} />
      {emitir && <EmitirFormula onCerrar={() => setEmitir(false)}
        onEmitida={(f) => { avisar(`${f.codigo} emitida`); setEmitir(false); lista.recargar() }} />}
    </>
  )
}

function EmitirFormula({ onCerrar, onEmitida }) {
  const [citas, setCitas] = useState([])
  const [medicamentos, setMedicamentos] = useState([])
  const [codigoCita, setCodigoCita] = useState('')
  const [items, setItems] = useState([{ codigoMedicamento: '', cantidad: 1, indicaciones: '' }])
  const [error, setError] = useState(null)

  useEffect(() => {
    api.citas({ estado: 'ATENDIDA', size: 30 }).then((d) => setCitas(d.contenido)).catch(setError)
    api.medicamentos({ size: 100 }).then((d) => setMedicamentos(d.contenido.filter((m) => m.activo))).catch(setError)
  }, [])

  const cambiar = (i, k, v) => setItems(items.map((it, j) => (j === i ? { ...it, [k]: v } : it)))
  const enviar = async (e) => {
    e.preventDefault()
    try {
      onEmitida(await api.emitirFormula({ codigoCita, items: items.map((i) => ({ ...i, cantidad: Number(i.cantidad) })) }))
    } catch (err) { setError(err) }
  }

  return (
    <Modal titulo="Emitir fórmula" onCerrar={onCerrar}>
      <form onSubmit={enviar}>
        <Campo etiqueta="Cita atendida" ancho>
          <select required value={codigoCita} onChange={(e) => setCodigoCita(e.target.value)}>
            <option value="">Elige la cita</option>
            {citas.map((c) => <option key={c.codigo} value={c.codigo}>{c.codigo} · {c.nombrePaciente} · {fecha(c.franja?.fecha)}</option>)}
          </select>
        </Campo>
        {citas.length === 0 && <p className="nota">No hay citas atendidas. Marca una como atendida en «Citas».</p>}
        <h3 className="paso">Medicamentos</h3>
        {items.map((it, i) => (
          <div key={i} className="item-formula">
            <select required value={it.codigoMedicamento} onChange={(e) => cambiar(i, 'codigoMedicamento', e.target.value)}>
              <option value="">Medicamento</option>
              {medicamentos.map((m) => <option key={m.codigo} value={m.codigo}>{m.nombre} · {m.presentacion} (stock {m.stock})</option>)}
            </select>
            <input type="number" min="1" required value={it.cantidad} onChange={(e) => cambiar(i, 'cantidad', e.target.value)} aria-label="Cantidad" />
            <input placeholder="Indicaciones" required value={it.indicaciones} onChange={(e) => cambiar(i, 'indicaciones', e.target.value)} />
            {items.length > 1 && <button type="button" className="boton-icono" aria-label="Quitar" onClick={() => setItems(items.filter((_, j) => j !== i))}>×</button>}
          </div>
        ))}
        <button type="button" className="boton boton-sutil" onClick={() => setItems([...items, { codigoMedicamento: '', cantidad: 1, indicaciones: '' }])}>Agregar medicamento</button>
        {error && <p className="error-formulario">{textoError(error)}</p>}
        <div className="pie-modal">
          <button type="button" className="boton boton-sutil" onClick={onCerrar}>Cancelar</button>
          <button className="boton">Emitir</button>
        </div>
      </form>
    </Modal>
  )
}

function Medicamentos() {
  const avisar = useAviso()
  const [busqueda, setBusqueda] = useState('')
  const [nombre, setNombre] = useState('')
  const [nuevo, setNuevo] = useState(false)
  const lista = useLista((page) => api.medicamentos({ nombre, page, size: 10 }), [nombre])

  const reabastecer = async (m) => {
    const cantidad = Number(window.prompt(`¿Cuántas unidades de ${m.nombre} ingresan?`, '50'))
    if (!cantidad) return
    try { await api.reabastecer(m.codigo, cantidad); avisar(`${m.nombre}: +${cantidad} unidades`); lista.recargar() }
    catch (e) { avisar(textoError(e), 'error') }
  }

  return (
    <>
      <form className="filtros" onSubmit={(e) => { e.preventDefault(); setNombre(busqueda.trim()) }}>
        <input placeholder="Buscar medicamento" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
        <button className="boton boton-sutil">Buscar</button>
        <span className="espacio" />
        <button type="button" className="boton" onClick={() => setNuevo(true)}>Registrar medicamento</button>
      </form>
      <div className="tabla-marco">
        <table>
          <thead><tr><th>Código</th><th>Medicamento</th><th>Presentación</th><th className="num">Stock</th><th /></tr></thead>
          <tbody>
            <EstadoLista {...lista} vacio={lista.datos?.contenido.length === 0} columnas={5} />
            {!lista.cargando && !lista.error && lista.datos?.contenido.map((m) => (
              <tr key={m.codigo}>
                <td className="codigo">{m.codigo}</td>
                <td><strong>{m.nombre}</strong></td>
                <td>{m.presentacion}</td>
                <td className={`num ${m.stock < 20 ? 'stock-bajo' : ''}`}>{m.stock}</td>
                <td className="acciones"><button className="boton boton-sutil" onClick={() => reabastecer(m)}>Reabastecer</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Paginador pagina={lista.datos} setPagina={lista.setPagina} />
      {nuevo && <NuevoMedicamento onCerrar={() => setNuevo(false)}
        onGuardado={(m) => { avisar(`${m.codigo} registrado`); setNuevo(false); lista.recargar() }} />}
    </>
  )
}

function NuevoMedicamento({ onCerrar, onGuardado }) {
  const [d, setD] = useState({ nombre: '', presentacion: '', stock: 100 })
  const [error, setError] = useState(null)
  const guardar = async (e) => {
    e.preventDefault()
    try { onGuardado(await api.registrarMedicamento({ ...d, stock: Number(d.stock) })) } catch (err) { setError(err) }
  }
  return (
    <Modal titulo="Registrar medicamento" onCerrar={onCerrar}>
      <form onSubmit={guardar}>
        <div className="rejilla">
          <Campo etiqueta="Nombre"><input required value={d.nombre} onChange={(e) => setD({ ...d, nombre: e.target.value })} /></Campo>
          <Campo etiqueta="Presentación"><input required placeholder="Tabletas 500 mg" value={d.presentacion} onChange={(e) => setD({ ...d, presentacion: e.target.value })} /></Campo>
          <Campo etiqueta="Stock inicial"><input required type="number" min="0" value={d.stock} onChange={(e) => setD({ ...d, stock: e.target.value })} /></Campo>
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
