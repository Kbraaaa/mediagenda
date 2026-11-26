# Pruebas de API con Postman

| Archivo | Qué es |
| --- | --- |
| `MediAgenda.postman_collection.json` | 117 peticiones en 5 carpetas: una por microservicio y una del flujo entre servicios |
| `MediAgenda-AWS.postman_environment.json` | API desplegada en AWS: `http://3.139.58.88` |
| `MediAgenda-Local.postman_environment.json` | Docker en tu equipo: `http://localhost:8000` |
| `correr-postman.bat` | Corre la colección con Newman (`correr-postman.bat AWS` para el entorno de AWS) |

Cada petición verifica el código HTTP, los datos de la respuesta y que tarde menos de 500 ms.
Última corrida contra AWS: 473 de 473 verificaciones, promedio 109 ms, máximo 446 ms.

## Abrirlas en Postman

1. **Import** → arrastra la colección y el entorno que vas a usar.
2. Arriba a la derecha, en el selector de entornos, elige **MediAgenda - AWS** (o Local).
   Si queda "No environment", las peticiones fallan con `ENOTFOUND {{baseUrl}}`.
3. Clic derecho en **MediAgenda - pruebas de API** → **Run collection** → **Run**.

Las peticiones se corren en orden: cada una guarda los códigos (paciente, médico, cita, fórmula…) que usan
las siguientes, y un sufijo aleatorio evita choques con datos de corridas anteriores. Las consultas (GET)
funcionan sueltas; las demás necesitan que antes se haya corrido la petición que crea el dato.

En la versión web de Postman hay que instalar el Postman Agent, porque la API usa http.

## Datos que usa el entorno de AWS

- `correoBase`: `success@simulator.amazonses.com`, el simulador de Amazon SES: acepta envíos reales sin salir
  del modo sandbox. `correoRechazado` es una dirección inexistente para probar el reintento de avisos fallidos.
- `telefonoPaciente`: celular de prueba; los avisos al celular salen por Telegram al chat configurado en el servidor.

Las peticiones crean datos reales en la base de AWS. Para dejarla limpia con datos de demostración:
`scripts\aws\reiniciar-datos-demo.bat`.

## Con Newman (línea de comandos)

```
npm install -g newman newman-reporter-htmlextra
newman run MediAgenda.postman_collection.json -e MediAgenda-AWS.postman_environment.json
```

O doble clic en `correr-postman.bat AWS`: genera `reporte-AWS.html` y `resultado-postman-AWS.txt`
(no se suben a GitHub).
