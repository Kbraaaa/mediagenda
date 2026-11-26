# MediAgenda

Sistema de gestión de citas médicas para una IPS, construido con microservicios.
Proyecto final de **Profundización de Software** (Universidad de Cundinamarca).

| Microservicio | Puerto | Qué hace |
| --- | --- | --- |
| `ms-pacientes` | 8081 | Registro, consulta, actualización y desactivación de pacientes |
| `ms-citas` | 8082 | Médicos, franjas de agenda y citas; triage con IA (Gemini) y festivos de Colombia |
| `ms-farmacia` | 8083 | Medicamentos, fórmulas y dispensación con control de stock |
| `ms-notificaciones` | 8084 | Consume los eventos y envía correos (Mailpit en local, Amazon SES en AWS) y avisos por Telegram |

La interfaz web (React, carpeta `frontend/`) se sirve desde el mismo nginx que enruta las APIs: **http://3.139.58.88**.

Los 4 microservicios tienen pruebas unitarias (351 en total) con cobertura de líneas del 100 %.

**Tecnologías:** Java 21, Spring Boot 4.1, React 19 + Vite, PostgreSQL 17, Flyway, Lombok, RabbitMQ, Docker Compose, Nginx,
AWS (EC2, SQS, SES, RDS, ECR, Systems Manager), Telegram Bot API, GitHub Actions, k6, JUnit 5, Mockito, AssertJ, JaCoCo y Postman/Newman.

## Probar la API con Postman

| | |
| --- | --- |
| Colección (148 peticiones) | [`pruebas/postman/MediAgenda.postman_collection.json`](pruebas/postman/MediAgenda.postman_collection.json) |
| Entorno AWS (`http://3.139.58.88`) | [`pruebas/postman/MediAgenda-AWS.postman_environment.json`](pruebas/postman/MediAgenda-AWS.postman_environment.json) |
| Entorno local (Docker) | [`pruebas/postman/MediAgenda-Local.postman_environment.json`](pruebas/postman/MediAgenda-Local.postman_environment.json) |
| Guía completa | [`pruebas/postman/LEEME.md`](pruebas/postman/LEEME.md) |

1. Descarga la colección y el entorno (en cada archivo, botón **Download raw file**).
2. En Postman: **Import** → arrastra los dos archivos.
3. Arriba a la derecha elige el entorno **MediAgenda - AWS**.
4. Clic derecho en la colección → **Run collection** → **Run** (las peticiones van en orden: cada una usa los códigos que crea la anterior).

Última corrida contra AWS: **473 de 473 verificaciones**, promedio 109 ms, máximo 446 ms (todas bajo 500 ms).

## Operaciones de negocio (además del CRUD)

Cada microservicio tiene 5 operaciones de negocio distintas del CRUD, en `Operaciones…Controller`.

| Servicio | Operación | Ruta |
| --- | --- | --- |
| ms-pacientes | Reactivar paciente | `PATCH /api/v1/pacientes/{codigo}/reactivacion` |
| ms-pacientes | Registrar alergia | `POST /api/v1/pacientes/{codigo}/alergias` |
| ms-pacientes | Verificar alergia antes de formular | `GET /api/v1/pacientes/{codigo}/alergias/verificacion?sustancia=` |
| ms-pacientes | Cambiar TI por cédula (18 años) | `POST /api/v1/pacientes/{codigo}/cambio-documento` |
| ms-pacientes | Compatibilidad sanguínea ABO/Rh | `GET /api/v1/pacientes/{codigo}/compatibilidad-sanguinea?donante=` |
| ms-citas | Abrir agenda | `POST /api/v1/medicos/{codigo}/franjas` |
| ms-citas | Reprogramar · cancelar · atender | `PUT …/reprogramacion` · `PATCH …/cancelacion` · `PATCH …/atencion` |
| ms-citas | Volver a pedir el triage con IA | `POST /api/v1/citas/{codigo}/triage` |
| ms-farmacia | Dispensar · anular fórmula | `POST …/dispensacion` · `PATCH …/anulacion` |
| ms-farmacia | Reabastecer · dar de baja inventario | `PATCH /api/v1/medicamentos/{codigo}/stock` · `POST …/bajas` |
| ms-farmacia | Verificar disponibilidad de una fórmula | `GET /api/v1/formulas/{codigo}/disponibilidad` |
| ms-notificaciones | Reintentar · marcar leída | `POST …/reintento` · `PATCH …/lectura` |
| ms-notificaciones | Reenviar · corregir destinatario | `POST …/reenvio` · `PATCH …/destinatario` |
| ms-notificaciones | Reintentar en lote los fallidos | `POST /api/v1/notificaciones/reintentos` |

## Arquitectura de cada microservicio (arquitectura limpia)

```
com.mediagenda.<servicio>
├── application/          Configuración: crea los casos de uso y les pasa sus gateways
├── domain/
│   ├── model/            Entidades del negocio y reglas de validación
│   │   ├── exception/    Excepciones del negocio (se convierten en 404, 409...)
│   │   └── gateway/      Interfaces (puertos) que el dominio necesita
│   └── usecase/          Un caso de uso por acción (responsabilidad única)
└── infrastructure/
    ├── entry_points/     Controladores REST, DTOs y manejador de errores
    ├── jpa_driver_adapter/ Entidades JPA, repositorios e implementación de los gateways
    ├── rest_client/      Llamadas a otros microservicios
    ├── eventos/          Publicación y consumo de eventos (RabbitMQ o SQS)
    └── mapper/           Conversión entre DTO, dominio y entidad
```

## Comunicación entre servicios

- **Síncrona (REST):** `ms-citas` consulta a `ms-pacientes`; `ms-farmacia` consulta a `ms-citas`.
- **Asíncrona (eventos):** citas y farmacia publican eventos que consume `ms-notificaciones`.
  - Local: RabbitMQ, exchange `mediagenda.eventos`, cola `notificaciones.q` y DLQ `notificaciones.dlq`.
  - AWS: Amazon SQS, cola `mediagenda-eventos` con DLQ `mediagenda-eventos-dlq` (3 intentos).
- El adaptador se elige por propiedad (`mediagenda.mensajeria=rabbit|sqs`, `notificaciones.correo=smtp|ses`); el dominio no cambia.
- **APIs externas:** Gemini para el triage (con respaldo por palabras clave) y Nager.Date para festivos (con respaldo de cálculo local, Ley 51 de 1983).

## Cómo correrlo en local

Necesita Docker Desktop.

```bash
cp .env.example .env        # y cambia las claves
docker compose up -d --build
curl http://localhost/api/v1/pacientes
```

Nginx expone todo en `http://localhost` y Mailpit muestra los correos en `http://localhost:8025`.

En Windows también se puede usar `scripts/local/probar-con-docker.bat`, que levanta todo y corre la prueba de punta a punta.

## Despliegue en AWS

EC2 (`m7i-flex.large`, `us-east-2`) con Docker Compose; las bases de datos están en Amazon RDS (PostgreSQL).
Los servicios usan SQS, SES y ECR sin claves: los permisos vienen del rol de IAM de la instancia.

- **Automático:** cada push a `main` corre las pruebas, compila la interfaz, publica las 5 imágenes (4 servicios + `frontend`) en ECR y despliega con Systems Manager (`.github/workflows/ci.yml`, acceso a AWS por OIDC).
- **Manual desde Windows:** `scripts/aws/desplegar-aws.bat`.

## Scripts (Windows)

| Script | Para qué |
| --- | --- |
| `subir-cambios.bat` | Guarda los cambios en git y los sube a GitHub (dispara el CI/CD) |
| `scripts/aws/desplegar-aws.bat` | Despliegue manual en EC2 |
| `scripts/aws/cobertura-aws.bat` | Cobertura con las APIs desplegadas (JaCoCo en el servidor + Postman) |
| `scripts/aws/correr-k6-aws.bat [concurrencia\|carga\|estres]` | Pruebas de carga, estrés y concurrencia en el servidor |
| `scripts/aws/migrar-a-rds.bat ENDPOINT` | Migración de la base del contenedor a RDS |
| `scripts/aws/configurar-telegram.bat` | Conecta el bot de Telegram |
| `scripts/aws/reiniciar-datos-demo.bat` | Borra los datos que dejaron las pruebas en RDS y carga datos de demostración |
| `scripts/local/ejecutar-todas-las-pruebas.bat` | Pruebas unitarias de los 4 servicios |
| `scripts/local/probar-con-docker.bat` | Levanta todo en local y corre la prueba de punta a punta |
| `scripts/local/probar-gemini.bat` | Prueba solo la clave de Gemini |
| `scripts/local/abrir-sustentacion.bat` | Abre reportes y pruebas para la sustentación |

## Pruebas

| Tipo | Cómo se corre | Resultado |
| --- | --- | --- |
| Unitarias + cobertura | `scripts/local/ejecutar-todas-las-pruebas.bat` o `mvn verify -f <servicio>/pom.xml` | `resumen-pruebas.txt` y `<servicio>/target/site/jacoco/index.html` |
| Punta a punta (Docker) | `scripts/local/probar-con-docker.bat` | `pruebas/resultado-punta-a-punta.txt` |
| API con Postman/Newman | `pruebas/postman/correr-postman.bat` (agrega `AWS` para el entorno de AWS) | Reporte HTML en `pruebas/postman/` |
| Cobertura desplegada | `scripts/aws/cobertura-aws.bat` | `pruebas/cobertura-aws/resumen.txt` |
| Carga, estrés y concurrencia | `scripts/aws/correr-k6-aws.bat` | `pruebas/k6/resultados/` |

**Si la cobertura de líneas o instrucciones de JaCoCo baja del 90 %, el build falla.**
GitHub Actions (`.github/workflows/ci.yml`) corre las pruebas de los 4 servicios en cada push a `main`.

## APIs

Base: `http://localhost/api/v1`

| Servicio | Rutas |
| --- | --- |
| Pacientes | `POST /pacientes` · `GET /pacientes/{codigo}` · `GET /pacientes?nombre=&page=&size=` · `PUT /pacientes/{codigo}` · `DELETE /pacientes/{codigo}` |
| Citas | `POST /citas` · `GET /citas/{codigo}` · `GET /citas` · `PUT /citas/{codigo}/reprogramacion` · `PATCH /citas/{codigo}/cancelacion` · `PATCH /citas/{codigo}/atencion` |
| Médicos y agenda | `POST /medicos` · `GET /medicos/{codigo}` · `GET /medicos` · `POST /medicos/{codigo}/franjas` · `GET /franjas` |
| Farmacia | `POST /medicamentos` · `GET /medicamentos/{codigo}` · `GET /medicamentos` · `PATCH /medicamentos/{codigo}/stock` · `POST /formulas` · `GET /formulas/{codigo}` · `GET /formulas` · `POST /formulas/{codigo}/dispensacion` · `PATCH /formulas/{codigo}/anulacion` |
| Notificaciones | `POST /notificaciones` · `GET /notificaciones/{codigo}` · `GET /notificaciones` · `POST /notificaciones/{codigo}/reintento` · `PATCH /notificaciones/{codigo}/lectura` |

Todas las peticiones, con ejemplos de cuerpo, están en la colección `pruebas/postman/MediAgenda.postman_collection.json`.

Ejemplo de cuerpo para registrar un paciente:

```json
{
  "tipoDocumento": "CC",
  "numeroDocumento": "1069123456",
  "nombres": "Laura Valentina",
  "apellidos": "Gomez Rojas",
  "correo": "laura.gomez@correo.com",
  "telefono": "3101234567",
  "fechaNacimiento": "1998-05-14",
  "tipoSangre": "O+",
  "alergias": "Penicilina"
}
```

**Errores:** `{"codigo": "PACIENTE_DUPLICADO", "mensaje": "..."}`.
400 = lo enviado está mal · 404 = no existe · 409 = lo enviado está bien, pero el estado actual no lo permite.

**Convención de ID:** UUID interno + código legible (`PAC-000123`), generado con una secuencia de PostgreSQL.
