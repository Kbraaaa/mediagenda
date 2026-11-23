# Pruebas de API con Postman

- `MediAgenda.postman_collection.json`: 64 peticiones en 5 carpetas (una por microservicio + flujo entre servicios).
- `MediAgenda-Local.postman_environment.json`: Docker en tu equipo (`http://localhost:8000`).
- `MediAgenda-AWS.postman_environment.json`: la instancia EC2 (cambiar `baseUrl` por la IP o dominio).
  Usa `success@simulator.amazonses.com` como correo de los pacientes: es el simulador de Amazon SES,
  que acepta envios reales sin salir del modo sandbox.
  `telefonoPaciente` va vacio para no enviar SMS a numeros sin verificar; pon tu celular verificado en SNS si quieres ver el SMS real.

## En Postman
1. Import -> arrastra los 3 archivos.
2. Arriba a la derecha elige el entorno.
3. Clic derecho en la coleccion -> Run collection -> Run. Debe correrse en orden y completa:
   cada peticion guarda los codigos (paciente, cita, formula...) que usa la siguiente.

## Con Newman (linea de comandos)
Doble clic en `correr-postman.bat` (Local) o `correr-postman.bat AWS`. Genera `reporte-<entorno>.html`.

Cada peticion comprueba el codigo HTTP, los datos de la respuesta y que llegue en menos de 500 ms.
