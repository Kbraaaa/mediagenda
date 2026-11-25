import { Fragment, useState } from 'react'
import { api, fechaHora, legible } from '../api'
import { Etiqueta, EstadoLista, Paginador, textoError, useAviso, useLista } from '../componentes/comunes'

export default function Notificaciones() {
  const avisar = useAviso()
  const [estado, setEstado] = useState('')
  const [abierta, setAbierta] = useState(null)
  const lista = useLista((page) => api.notificaciones({ estado, page, size: 12 }), [estado])

  const accion = async (fn, n, texto) => {
    try { const r = await fn(n.codigo); avisar(`${n.codigo} ${texto}: ${legible(r.estado)}`); lista.recargar() }
    catch (e) { avisar(textoError(e), 'error') }
  }

  return (
    <>
      <header className="encabezado">
        <div>
          <h1>Notificaciones</h1>
          <p>Avisos que ms-notificaciones envía al consumir los eventos de SQS: correo por Amazon SES y mensaje por Telegram.</p>
        </div>
        <button className="boton boton-sutil" onClick={lista.recargar}>Actualizar</button>
      </header>

      <div className="filtros">
        <select value={estado} onChange={(e) => setEstado(e.target.value)}>
          <option value="">Todos los estados</option>
          <option value="PENDIENTE">Pendientes</option>
          <option value="ENVIADA">Enviadas</option>
          <option value="FALLIDA">Fallidas</option>
          <option value="LEIDA">Leídas</option>
        </select>
      </div>

      <div className="tabla-marco">
        <table>
          <thead><tr><th>Código</th><th>Canal</th><th>Destinatario</th><th>Asunto</th><th>Fecha</th><th>Estado</th><th /></tr></thead>
          <tbody>
            <EstadoLista {...lista} vacio={lista.datos?.contenido.length === 0} columnas={7} />
            {!lista.cargando && !lista.error && lista.datos?.contenido.map((n) => (
              <Fragment key={n.codigo}>
                <tr className="fila-clic" onClick={() => setAbierta(abierta === n.codigo ? null : n.codigo)}>
                  <td className="codigo">{n.codigo}</td>
                  <td>{n.canal === 'SMS' ? 'Celular' : 'Correo'}<small>{legible(n.tipo)}</small></td>
                  <td>{n.destinatario}<small>{n.codigoPaciente}</small></td>
                  <td>{n.asunto}</td>
                  <td>{fechaHora(n.fechaEnvio || n.fechaCreacion)}<small>{n.intentos} intento{n.intentos === 1 ? '' : 's'}</small></td>
                  <td><Etiqueta valor={n.estado} /></td>
                  <td className="acciones" onClick={(e) => e.stopPropagation()}>
                    {n.estado === 'FALLIDA' && <button className="boton" onClick={() => accion(api.reintentar, n, 'reintentada')}>Reintentar</button>}
                    {n.estado === 'ENVIADA' && <button className="boton boton-sutil" onClick={() => accion(api.marcarLeida, n, 'marcada')}>Marcar leída</button>}
                  </td>
                </tr>
                {abierta === n.codigo && (
                  <tr className="fila-detalle">
                    <td colSpan={7}>
                      <pre className="cuerpo">{n.cuerpo}</pre>
                      {n.ultimoError && <p className="error">Último error: {n.ultimoError}</p>}
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      </div>
      <Paginador pagina={lista.datos} setPagina={lista.setPagina} />
    </>
  )
}
