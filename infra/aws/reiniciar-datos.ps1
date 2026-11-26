# Deja las 4 bases de MediAgenda (Amazon RDS) sin los datos que generaron las pruebas y carga datos de demostracion.
# Uso: scripts\aws\reiniciar-datos-demo.bat     (borra TODO lo que hay en las tablas; la evidencia de las pruebas
# queda en pruebas\postman, pruebas\k6 y pruebas\cobertura-aws). Resultado: pruebas\resultado-reinicio-datos.txt
param([string]$Ip = "3.139.58.88")
$ErrorActionPreference = "Continue"
$raiz = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $raiz
$salida = Join-Path $raiz "pruebas\resultado-reinicio-datos.txt"
function Anotar($t) { $t | Out-File $salida -Append -Encoding utf8; Write-Host $t }
"Reinicio de datos - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" | Out-File $salida -Encoding utf8

$Clave = @("$env:USERPROFILE\.ssh\mediagenda-clave.pem", "$env:USERPROFILE\Downloads\mediagenda-clave.pem",
           "$env:USERPROFILE\Descargas\mediagenda-clave.pem", "$env:USERPROFILE\Documents\mediagenda-clave.pem") |
         Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $Clave) { Anotar "FALLO no encontre mediagenda-clave.pem"; exit 1 }
$ssh = @("-i", $Clave, "-o", "StrictHostKeyChecking=accept-new", "-o", "ServerAliveInterval=30")
$destino = "ec2-user@$Ip"
$envServidor = "$env:USERPROFILE\.ssh\mediagenda-servidor.env"
$pg = (Get-Content $envServidor | Where-Object { $_ -match '^DB_PASSWORD=' }) -replace '^DB_PASSWORD=', ''
$hostDb = (Get-Content $envServidor | Where-Object { $_ -match '^DB_HOST=' }) -replace '^DB_HOST=', ''
if (-not $hostDb -or $hostDb -eq 'postgres') { Anotar "FALLO el .env del servidor no apunta a RDS (DB_HOST)"; exit 1 }

# Texto literal (@' '@): nada se expande en PowerShell; solo se reemplazan __CLAVE__ y __HOST__.
$remoto = @'
reiniciar() {
set -e
cd mediagenda
DC='docker compose -f docker-compose.aws.yml'
PSQL='docker run -i --rm -e PGPASSWORD=__CLAVE__ -e PGSSLMODE=require postgres:17 psql -h __HOST__ -U postgres -v ON_ERROR_STOP=1 -q'

echo "1/3 Deteniendo los 4 servicios para que nadie escriba mientras se limpia"
$DC stop ms-pacientes ms-citas ms-farmacia ms-notificaciones

echo "2/3 Vaciando tablas y reiniciando los codigos (PAC-000001, CIT-...-000001, ...)"
$PSQL -d pacientes_db -c "TRUNCATE pacientes; ALTER SEQUENCE paciente_codigo_seq RESTART WITH 1;"
$PSQL -d citas_db -c "TRUNCATE citas, franjas, medicos RESTART IDENTITY; ALTER SEQUENCE medico_codigo_seq RESTART WITH 1; ALTER SEQUENCE cita_codigo_seq RESTART WITH 1;"
$PSQL -d notificaciones_db -c "TRUNCATE notificaciones; ALTER SEQUENCE notificacion_codigo_seq RESTART WITH 1;"
$PSQL -d farmacia_db -c "TRUNCATE items_formula, formulas, medicamentos RESTART IDENTITY; ALTER SEQUENCE formula_codigo_seq RESTART WITH 1;
INSERT INTO medicamentos (id_medicamento, codigo, nombre, presentacion, stock, activo) VALUES
 (gen_random_uuid(),'MDT-0001','Acetaminofen','Tabletas 500 mg',500,TRUE),(gen_random_uuid(),'MDT-0002','Ibuprofeno','Tabletas 400 mg',300,TRUE),
 (gen_random_uuid(),'MDT-0003','Amoxicilina','Capsulas 500 mg',200,TRUE),(gen_random_uuid(),'MDT-0004','Loratadina','Tabletas 10 mg',150,TRUE),
 (gen_random_uuid(),'MDT-0005','Omeprazol','Capsulas 20 mg',250,TRUE),(gen_random_uuid(),'MDT-0006','Losartan','Tabletas 50 mg',180,TRUE),
 (gen_random_uuid(),'MDT-0007','Metformina','Tabletas 850 mg',220,TRUE),(gen_random_uuid(),'MDT-0008','Salbutamol','Inhalador 100 mcg',5,TRUE);
SELECT setval('medicamento_codigo_seq', 8);"
for db in pacientes_db citas_db farmacia_db notificaciones_db; do
  echo "   $db: $($PSQL -d $db -Atc "SELECT string_agg(relname || '=' || n_live_tup, ' ') FROM pg_stat_user_tables WHERE relname <> 'flyway_schema_history'")"
done

echo "   Arrancando los servicios..."
$DC start ms-pacientes ms-citas ms-farmacia ms-notificaciones
$DC restart nginx
for i in $(seq 1 60); do ok=0; for r in pacientes medicos medicamentos notificaciones; do curl -fs -o /dev/null http://localhost/api/v1/$r && ok=$((ok+1)); done; [ $ok -eq 4 ] && break; sleep 5; done
[ $ok -eq 4 ] || { echo "FALLO los servicios no respondieron"; exit 1; }

echo "3/3 Cargando datos de demostracion por las APIs"
python3 - <<'PY'
import json, urllib.request, urllib.error, datetime
B = "http://localhost/api/v1"
def api(metodo, ruta, cuerpo=None):
    req = urllib.request.Request(B + ruta, method=metodo, data=json.dumps(cuerpo).encode() if cuerpo is not None else None,
                                 headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r: return r.status, json.loads(r.read() or b"null")
    except urllib.error.HTTPError as e: return e.code, json.loads(e.read() or b"null")

medicos = [("Dra. Andrea Lopez", "MEDICINA_GENERAL"), ("Dr. Pedro Ruiz", "PEDIATRIA"),
           ("Dra. Camila Torres", "CARDIOLOGIA"), ("Dr. Andres Gomez", "DERMATOLOGIA")]
cod_med = {}
for nombre, esp in medicos:
    correo = nombre.split()[1].lower() + "." + nombre.split()[2].lower() + "@mediagenda.co"
    s, m = api("POST", "/medicos", {"nombre": nombre, "especialidad": esp, "correo": correo})
    print(f"   medico {m.get('codigo')} {nombre} ({s})"); cod_med[esp] = m["codigo"]

# Agenda: proximos 3 dias habiles de 8:00 a 12:00 (si la API dice que es festivo, pasa al siguiente dia)
for esp, cod in cod_med.items():
    d, abiertos = datetime.date.today(), 0
    while abiertos < 3:
        d += datetime.timedelta(days=1)
        if d.weekday() >= 5: continue
        s, r = api("POST", f"/medicos/{cod}/franjas", {"fecha": d.isoformat(), "horaInicio": "08:00", "horaFin": "12:00", "duracionMinutos": 30})
        if s == 201 or s == 200: abiertos += 1
        else: print(f"   {cod} {d}: {s} {r.get('mensaje') if r else ''}")
    print(f"   agenda de {cod}: 3 dias abiertos")

pacientes = [("CC","1069123456","Laura Valentina","Gomez Rojas","laura","3101234567","1998-05-14","O+","Penicilina"),
             ("TI","1012345678","Mateo","Ruiz Castro","mateo","3112345678","2014-03-09","A+",None),
             ("CC","52789456","Sofia","Martinez Diaz","sofia","3123456789","1985-11-22","B+",None),
             ("CC","80456123","Carlos Andres","Perez Lopez","carlos","3134567890","1972-07-30","O-","Ibuprofeno"),
             ("CE","456789","Valeria","Moreno","valeria","3145678901","2001-01-18","AB+",None)]
cod_pac = []
for t, n, no, ap, alias, tel, nac, sangre, alerg in pacientes:
    s, p = api("POST", "/pacientes", {"tipoDocumento": t, "numeroDocumento": n, "nombres": no, "apellidos": ap,
        "correo": f"success+{alias}@simulator.amazonses.com", "telefono": tel, "fechaNacimiento": nac, "tipoSangre": sangre, "alergias": alerg})
    print(f"   paciente {p.get('codigo')} {no} {ap} ({s})"); cod_pac.append(p["codigo"])

def franja(esp, n):
    s, f = api("GET", f"/franjas?medico={cod_med[esp]}&size=20"); return f["contenido"][n]["idFranja"]

citas = [(0, "CARDIOLOGIA", 0, "Dolor en el pecho al subir escaleras y cansancio desde hace una semana"),
         (1, "PEDIATRIA", 1, "Fiebre de 38.5 y tos seca desde hace dos dias"),
         (2, "MEDICINA_GENERAL", 2, "Dolor de cabeza frecuente en las tardes"),
         (3, "MEDICINA_GENERAL", 4, "Control de presion arterial")]
cod_cit = []
for ip, esp, nf, sint in citas:
    s, c = api("POST", "/citas", {"codigoPaciente": cod_pac[ip], "idFranja": franja(esp, nf), "sintomas": sint})
    print(f"   cita {c.get('codigo')} ({s})"); cod_cit.append(c.get("codigo"))

# La ultima cita queda atendida, con formula emitida y dispensada
s, _ = api("PATCH", f"/citas/{cod_cit[3]}/atencion"); print(f"   cita {cod_cit[3]} atendida ({s})")
s, f = api("POST", "/formulas", {"codigoCita": cod_cit[3], "items": [
    {"codigoMedicamento": "MDT-0006", "cantidad": 30, "indicaciones": "1 tableta cada 24 horas"},
    {"codigoMedicamento": "MDT-0001", "cantidad": 10, "indicaciones": "1 tableta cada 8 horas si hay dolor"}]})
print(f"   formula {f.get('codigo')} emitida ({s})")
s, _ = api("POST", f"/formulas/{f['codigo']}/dispensacion"); print(f"   formula {f['codigo']} dispensada ({s})")
print("DATOS DE DEMOSTRACION OK")
PY
}
reiniciar #
'@
$remoto = $remoto.Replace('__CLAVE__', $pg).Replace('__HOST__', $hostDb)
Anotar "Conectando con $destino (los servicios quedan detenidos 1 a 2 minutos)..."
$log = ($remoto -replace "`r", "") | & ssh @ssh $destino "bash -s" 2>&1
$log | ForEach-Object { Anotar "$_" }
if ($log -match "DATOS DE DEMOSTRACION OK") { Anotar "RESULTADO: OK. Abre http://$Ip" } else { Anotar "RESULTADO: revisar el detalle de arriba" }
