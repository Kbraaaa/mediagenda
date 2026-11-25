import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { legible } from '../api'

/* ---------- Avisos (toasts) ---------- */
const ContextoAvisos = createContext(() => {})
export const useAviso = () => useContext(ContextoAvisos)

export function ProveedorAvisos({ children }) {
  const [avisos, setAvisos] = useState([])
  const avisar = useCallback((texto, tipo = 'ok') => {
    const id = Math.random()
    setAvisos((a) => [...a, { id, texto, tipo }])
    setTimeout(() => setAvisos((a) => a.filter((x) => x.id !== id)), tipo === 'error' ? 7000 : 4000)
  }, [])
  return (
    <ContextoAvisos.Provider value={avisar}>
      {children}
      <div className="avisos" role="status" aria-live="polite">
        {avisos.map((a) => (
          <div key={a.id} className={`aviso aviso-${a.tipo}`}>{a.texto}</div>
        ))}
      </div>
    </ContextoAvisos.Provider>
  )
}

/** Convierte un ErrorApi en un texto con el codigo HTTP, que es lo que interesa mostrar. */
export const textoError = (e) => (e?.estado ? `${e.estado} · ${e.message}` : e?.message || 'Error inesperado')

/* ---------- Estado con color ---------- */
const COLORES = {
  AGENDADA: 'azul', DISPONIBLE: 'verde', RESERVADA: 'gris', ATENDIDA: 'verde', CANCELADA: 'gris',
  EMITIDA: 'azul', DISPENSADA: 'verde', ANULADA: 'gris',
  PENDIENTE: 'ambar', ENVIADA: 'verde', FALLIDA: 'rojo', LEIDA: 'gris', COMPLETADO: 'verde', FALLIDO: 'rojo',
  ALTA: 'rojo', MEDIA: 'ambar', BAJA: 'verde', ACTIVO: 'verde', INACTIVO: 'gris',
}
export function Etiqueta({ valor }) {
  if (!valor) return null
  return <span className={`etiqueta etiqueta-${COLORES[valor] || 'gris'}`}>{legible(valor)}</span>
}

/* ---------- Paginador ---------- */
export function Paginador({ pagina, setPagina }) {
  if (!pagina || pagina.totalPaginas <= 1) return null
  return (
    <div className="paginador">
      <button className="boton boton-sutil" disabled={pagina.pagina === 0} onClick={() => setPagina(pagina.pagina - 1)}>
        Anterior
      </button>
      <span>Página {pagina.pagina + 1} de {pagina.totalPaginas} · {pagina.totalElementos} registros</span>
      <button className="boton boton-sutil" disabled={pagina.pagina + 1 >= pagina.totalPaginas}
        onClick={() => setPagina(pagina.pagina + 1)}>
        Siguiente
      </button>
    </div>
  )
}

/* ---------- Ventana modal ---------- */
export function Modal({ titulo, onCerrar, children }) {
  useEffect(() => {
    const esc = (e) => e.key === 'Escape' && onCerrar()
    window.addEventListener('keydown', esc)
    return () => window.removeEventListener('keydown', esc)
  }, [onCerrar])
  return (
    <div className="fondo-modal" onMouseDown={(e) => e.target === e.currentTarget && onCerrar()}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={titulo}>
        <div className="modal-encabezado">
          <h2>{titulo}</h2>
          <button className="boton-icono" onClick={onCerrar} aria-label="Cerrar">×</button>
        </div>
        {children}
      </div>
    </div>
  )
}

/* ---------- Campo de formulario ---------- */
export function Campo({ etiqueta, children, ancho }) {
  return (
    <label className={`campo ${ancho ? 'campo-ancho' : ''}`}>
      <span>{etiqueta}</span>
      {children}
    </label>
  )
}

/* ---------- Carga de listas paginadas ---------- */
export function useLista(cargar, deps) {
  const [pagina, setPagina] = useState(0)
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const [cargando, setCargando] = useState(true)
  const [version, setVersion] = useState(0)
  useEffect(() => {
    let vivo = true
    setCargando(true)
    cargar(pagina)
      .then((d) => { if (vivo) { setDatos(d); setError(null) } })
      .catch((e) => { if (vivo) setError(e) })
      .finally(() => vivo && setCargando(false))
    return () => { vivo = false }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagina, version, ...deps])
  useEffect(() => setPagina(0), deps) // eslint-disable-line react-hooks/exhaustive-deps
  return { datos, error, cargando, setPagina, recargar: () => setVersion((v) => v + 1) }
}

export function EstadoLista({ cargando, error, vacio, columnas }) {
  if (error) return <tr><td colSpan={columnas} className="celda-info error">{textoError(error)}</td></tr>
  if (cargando) return <tr><td colSpan={columnas} className="celda-info">Cargando…</td></tr>
  if (vacio) return <tr><td colSpan={columnas} className="celda-info">No hay registros.</td></tr>
  return null
}
