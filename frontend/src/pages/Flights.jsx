import React, { useEffect, useState } from 'react'
import { get, post } from '../api'

export default function Flights(){
  const [flights,setFlights] = useState([])
  const [form,setForm] = useState({ flightCode:'', source:'', destination:'', flightDate:'', seats:1 })
  const [loading,setLoading] = useState(false)

  useEffect(()=>{ fetchList() },[])
  async function fetchList(){
    try{ const data = await get('/flights'); setFlights(data || []) }catch(e){ console.error(e) }
  }

  async function handleSubmit(e){
    e.preventDefault(); setLoading(true)
    try{ await post('/flights', form); setForm({ flightCode:'', source:'', destination:'', flightDate:'', seats:1 }); fetchList() }catch(e){ alert(e.message) }
    finally{ setLoading(false) }
  }

  return (
    <div>
      <h2>Flight Management</h2>
      <form onSubmit={handleSubmit} className="demo-form">
        <input placeholder="Flight Code" value={form.flightCode} onChange={e=>setForm({...form, flightCode:e.target.value})} />
        <input placeholder="Source" value={form.source} onChange={e=>setForm({...form, source:e.target.value})} />
        <input placeholder="Destination" value={form.destination} onChange={e=>setForm({...form, destination:e.target.value})} />
        <input type="date" value={form.flightDate} onChange={e=>setForm({...form, flightDate:e.target.value})} />
        <input type="number" min="1" value={form.seats} onChange={e=>setForm({...form, seats:Number(e.target.value)})} />
        <button type="submit" disabled={loading}>{loading? 'Saving...':'Save Flight'}</button>
      </form>

      <h3>Flight List</h3>
      <table className="data-table">
        <thead><tr><th>Code</th><th>Source</th><th>Dest</th><th>Date</th><th>Seats</th></tr></thead>
        <tbody>
          {flights.map(f=> (
            <tr key={f.id||f.flightCode}><td>{f.flightCode}</td><td>{f.source}</td><td>{f.destination}</td><td>{f.flightDate}</td><td>{f.seats}</td></tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
