import { useState } from 'react'

const SRI_API = import.meta.env.VITE_SRI_API || 'http://localhost:8081/api/sri'
const ANT_API = import.meta.env.VITE_ANT_API || 'http://localhost:8082/api/ant'

async function pedir(url) {
  const res = await fetch(url)
  const data = await res.json().catch(() => ({}))
  if (!res.ok) throw new Error(data.mensaje || `Error ${res.status}`)
  return data
}

function Tabla({ datos }) {
  const filas = Object.entries(datos).filter(([, v]) => v !== null && v !== '' && typeof v !== 'object')
  return (
    <table>
      <tbody>
        {filas.map(([k, v]) => (
          <tr key={k}>
            <th>{k}</th>
            <td>{String(v)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

export default function App() {
  const [email, setEmail] = useState('')
  const [ruc, setRuc] = useState('')
  const [placa, setPlaca] = useState('')

  const [persona, setPersona] = useState(null)
  const [vehiculo, setVehiculo] = useState(null)
  const [puntos, setPuntos] = useState(null)

  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState('')
  const [errorAnt, setErrorAnt] = useState('')

  async function verificar(e) {
    e.preventDefault()
    setError(''); setPersona(null); setVehiculo(null); setPuntos(null)
    setCargando(true)
    try {
      setPersona(await pedir(`${SRI_API}/contribuyente/${ruc.trim()}`))
    } catch (err) {
      setError(err.message)
    } finally {
      setCargando(false)
    }
  }

  async function consultarVehiculo(e) {
    e.preventDefault()
    setError(''); setErrorAnt(''); setVehiculo(null); setPuntos(null)
    setCargando(true)
    const cedula = ruc.trim().substring(0, 10)
    const [v, p] = await Promise.allSettled([
      pedir(`${SRI_API}/vehiculo/${encodeURIComponent(placa.trim())}`),
      pedir(`${ANT_API}/puntos/${cedula}?placa=${encodeURIComponent(placa.trim())}`),
    ])
    if (v.status === 'fulfilled') setVehiculo(v.value); else setError(v.reason.message)
    if (p.status === 'fulfilled') setPuntos(p.value); else setErrorAnt(p.reason.message)
    setCargando(false)
  }

  return (
    <main>
      <h1>Consulta SRI + ANT</h1>

      <form onSubmit={verificar} className="card">
        <h2>1. Datos del contribuyente</h2>
        <label>
          Correo electrónico
          <input type="email" required value={email} onChange={e => setEmail(e.target.value)} placeholder="correo@ejemplo.com" />
        </label>
        <label>
          RUC (persona natural)
          <input required pattern="\d{13}" maxLength={13} value={ruc} onChange={e => setRuc(e.target.value)} placeholder="1712345678001" />
        </label>
        <button disabled={cargando}>Verificar</button>
      </form>

      {error && <p className="error">{error}</p>}

      {persona && (
        <section className="card">
          <h2>Información de la persona</h2>
          <p className="ok">Es contribuyente del SRI y es persona natural</p>
          <Tabla datos={persona} />
        </section>
      )}

      {persona && (
        <form onSubmit={consultarVehiculo} className="card">
          <h2>2. Matrícula de su vehículo</h2>
          <label>
            Placa
            <input required value={placa} onChange={e => setPlaca(e.target.value.toUpperCase())} placeholder="PBA1234" />
          </label>
          <button disabled={cargando}>Consultar</button>
        </form>
      )}

      {cargando && <p>Consultando...</p>}

      {vehiculo && (
        <section className="card">
          <h2>Información del vehículo</h2>
          <Tabla datos={vehiculo} />
        </section>
      )}

      {errorAnt && <p className="error">ANT: {errorAnt}</p>}

      {puntos && (
        <section className="card">
          <h2>Puntos de licencia (ANT)</h2>
          <p className="puntos">{puntos.puntos} puntos</p>
          <p>
            Fuente: <span className={`badge ${puntos.fuente}`}>{puntos.fuente}</span>
            {' '}· consultado: {new Date(puntos.fechaConsulta).toLocaleString()}
          </p>
          {puntos.citaciones?.length > 0 && (
            <table>
              <tbody>
                {puntos.citaciones.map((fila, i) => (
                  <tr key={i}>{fila.map((c, j) => <td key={j}>{c}</td>)}</tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      )}
    </main>
  )
}
