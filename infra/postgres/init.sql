-- Crea una base de datos por microservicio (solo en local; en AWS se crean en RDS una vez).
-- Las tablas NO se crean aqui: cada microservicio las crea con Flyway al arrancar.
CREATE DATABASE pacientes_db;
CREATE DATABASE citas_db;
CREATE DATABASE farmacia_db;
CREATE DATABASE notificaciones_db;
