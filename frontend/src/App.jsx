import { NavLink, Route, Routes } from 'react-router-dom'
import { ProveedorAvisos } from './componentes/comunes'
import Inicio from './paginas/Inicio'
import Pacientes from './paginas/Pacientes'
import Citas from './paginas/Citas'
import Medicos from './paginas/Medicos'
import Farmacia from './paginas/Farmacia'
import Notificaciones from './paginas/Notificaciones'

const MENU = [
  { ruta: '/', texto: 'Inicio', fin: true },
  { ruta: '/pacientes', texto: 'Pacientes' },
  { ruta: '/citas', texto: 'Citas' },
  { ruta: '/medicos', texto: 'Médicos y agenda' },
  { ruta: '/farmacia', texto: 'Farmacia' },
  { ruta: '/notificaciones', texto: 'Notificaciones' },
]

export default function App() {
  return (
    <ProveedorAvisos>
      <div className="marco">
        <aside className="lateral">
          <div className="marca">
            <img src="/favicon.svg" alt="" width="28" height="28" />
            <div>
              <strong>MediAgenda</strong>
              <small>Citas médicas</small>
            </div>
          </div>
          <nav>
            {MENU.map((m) => (
              <NavLink key={m.ruta} to={m.ruta} end={m.fin}
                className={({ isActive }) => `enlace ${isActive ? 'activo' : ''}`}>
                {m.texto}
              </NavLink>
            ))}
          </nav>
          <p className="pie-lateral">4 microservicios · AWS</p>
        </aside>
        <main className="contenido">
          <Routes>
            <Route path="/" element={<Inicio />} />
            <Route path="/pacientes" element={<Pacientes />} />
            <Route path="/citas" element={<Citas />} />
            <Route path="/medicos" element={<Medicos />} />
            <Route path="/farmacia" element={<Farmacia />} />
            <Route path="/notificaciones" element={<Notificaciones />} />
            <Route path="*" element={<Inicio />} />
          </Routes>
        </main>
      </div>
    </ProveedorAvisos>
  )
}
