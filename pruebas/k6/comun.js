// Utilidades compartidas por las pruebas de k6 de MediAgenda.
import http from 'k6/http';

export const BASE = __ENV.BASE_URL || 'http://localhost';
export const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

export function post(ruta, cuerpo, etiqueta) {
  return http.post(`${BASE}${ruta}`, JSON.stringify(cuerpo), { ...JSON_HEADERS, tags: { name: etiqueta || ruta } });
}

export function get(ruta, etiqueta, etiquetasExtra) {
  return http.get(`${BASE}${ruta}`, { tags: { name: etiqueta || ruta, ...(etiquetasExtra || {}) } });
}

/** Fecha futura (hoy + dias) en formato yyyy-MM-dd, saltando domingos. */
export function fechaFutura(dias) {
  const f = new Date(Date.now() + dias * 86400000);
  if (f.getUTCDay() === 0) f.setUTCDate(f.getUTCDate() + 1);
  return f.toISOString().slice(0, 10);
}

export function aleatorio(digitos) {
  return String(Math.floor(Math.random() * 10 ** digitos)).padStart(digitos, '0');
}

/** Resumen corto en texto + JSON completo para el articulo. */
export function resumen(nombre, data) {
  const m = data.metrics;
  const v = (metrica, campo) => (m[metrica] && m[metrica].values[campo] !== undefined ? m[metrica].values[campo] : NaN);
  const linea = [
    `Prueba: ${nombre}`,
    `Peticiones: ${v('http_reqs', 'count')} (${v('http_reqs', 'rate').toFixed(1)} por segundo)`,
    `Tiempo de respuesta: promedio ${v('http_req_duration', 'avg').toFixed(0)} ms, ` +
      `mediana ${v('http_req_duration', 'med').toFixed(0)} ms, p90 ${v('http_req_duration', 'p(90)').toFixed(0)} ms, ` +
      `p95 ${v('http_req_duration', 'p(95)').toFixed(0)} ms, maximo ${v('http_req_duration', 'max').toFixed(0)} ms`,
    `Errores HTTP inesperados: ${(v('http_req_failed', 'rate') * 100).toFixed(2)} %`,
    `Usuarios virtuales maximos: ${v('vus_max', 'max')}`,
  ];
  for (const [umbral, res] of Object.entries(data.metrics).flatMap(([k, met]) =>
    Object.entries(met.thresholds || {}).map(([t, r]) => [`${k} ${t}`, r]))) {
    linea.push(`Umbral ${umbral}: ${res.ok ? 'CUMPLE' : 'NO CUMPLE'}`);
  }
  return {
    [`/scripts/resultados/${nombre}.json`]: JSON.stringify(data, null, 2),
    [`/scripts/resultados/${nombre}.txt`]: linea.join('\n') + '\n',
    stdout: '\n' + linea.join('\n') + '\n',
  };
}
