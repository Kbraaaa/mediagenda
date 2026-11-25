# Interfaz web de MediAgenda

React 19 + Vite. Se sirve desde el mismo nginx que enruta las APIs (`nginx.conf`), así que las llamadas
son relativas (`/api/v1/...`) y no hace falta CORS.

| Pantalla | Microservicio | Qué permite |
| --- | --- | --- |
| Inicio | los 4 | Estado y tiempo de respuesta de cada servicio, pendientes del día |
| Pacientes | ms-pacientes | Registrar, buscar, editar y desactivar |
| Citas | ms-citas | Agendar (paciente → médico → franja → síntomas), ver el triage de Gemini, reprogramar, cancelar y atender |
| Médicos y agenda | ms-citas | Registrar médicos y abrir franjas (los festivos se rechazan) |
| Farmacia | ms-farmacia | Emitir, dispensar y anular fórmulas; inventario de medicamentos |
| Notificaciones | ms-notificaciones | Avisos por correo y Telegram, reintento de fallidos |

## Desarrollo

```bash
npm install
npm run dev                                  # usa la API local de Docker Compose (http://localhost:8000)
API_URL=http://3.139.58.88 npm run dev       # o la API desplegada en AWS
```

## Imagen

`Dockerfile` compila con Node y copia el resultado a `nginx:1.27-alpine`. GitHub Actions la publica en ECR
como `mediagenda/web` y la despliega junto con los microservicios.
