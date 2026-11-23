# MediAgenda

Sistema de gestión de citas médicas para una IPS, construido con microservicios.
Proyecto final de **Profundización de Software** (Universidad de Cundinamarca).

| Microservicio | Puerto | Qué hace |
| --- | --- | --- |
| `ms-pacientes` | 8081 | Registro, consulta, actualización y desactivación de pacientes |
| `ms-citas` | 8082 | Médicos, franjas de agenda y citas; triage con IA (Gemini) y festivos de Colombia |
| `ms-farmacia` | 8083 | Medicamentos, fórmulas y dispensación con control de stock |
| `ms-notificaciones` | 8084 | Consume los eventos y envía correos (Mailpit en local, Amazon SES en AWS) |

Los 4 microservicios tienen pruebas unitarias (239 en total) con cobertura de líneas del 100 %.

**Tecnologías:** Java 21, Spring Boot 4.1, PostgreSQL 17, Flyway, Lombok, RabbitMQ, Docker Compose, Nginx,
AWS (EC2, SQS, SES), GitHub Actions, JUnit 5, Mockito, AssertJ, JaCoCo y Postman/Newman.

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

En Windows también se puede usar `probar-con-docker.bat`, que levanta todo y corre la prueba de punta a punta.

## Despliegue en AWS

EC2 (`m7i-flex.large`, `us-east-1`) con Docker Compose y PostgreSQL en contenedor:

```bash
cp .env.aws.example .env
docker compose -f docker-compose.aws.yml up -d --build
```

En AWS los servicios usan SQS y SES sin claves: los permisos vienen del rol de IAM de la instancia.

## Pruebas

| Tipo | Cómo se corre | Resultado |
| --- | --- | --- |
| Unitarias + cobertura | `ejecutar-todas-las-pruebas.bat` o `mvn verify -f <servicio>/pom.xml` | `resumen-pruebas.txt` y `<servicio>/target/site/jacoco/index.html` |
| Punta a punta (Docker) | `probar-con-docker.bat` | `pruebas/resultado-punta-a-punta.txt` |
| API con Postman/Newman | `pruebas/postman/correr-postman.bat` (agrega `AWS` para el entorno de AWS) | Reporte HTML en `pruebas/postman/` |

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
