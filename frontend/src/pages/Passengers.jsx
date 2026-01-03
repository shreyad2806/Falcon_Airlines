import React, { useEffect, useState } from 'react'
import { get, post } from '../api'

export default function Passengers(){
  const [list,setList] = useState([])
  const [form,setForm] = useState({ passengerName:'', nationality:'', gender:'', passportNumber:'', phoneNumber:'', address:'' })

  useEffect(()=>{ fetchList() },[])
  async function fetchList(){ try{ const data = await get('/passengers'); setList(data||[]) }catch(e){console.error(e)} }

  async function submit(e){ e.preventDefault(); try{ await post('/passengers', form); setForm({ passengerName:'', nationality:'', gender:'', passportNumber:'', phoneNumber:'', address:'' }); fetchList() }catch(e){alert(e.message)} }

  return (
    <div>
      <h2>Passenger Management</h2>
      <form onSubmit={submit} className="demo-form">
        <input placeholder="Name" value={form.passengerName} onChange={e=>setForm({...form, passengerName:e.target.value})} />
        <input placeholder="Nationality" value={form.nationality} onChange={e=>setForm({...form, nationality:e.target.value})} />
        <input placeholder="Gender" value={form.gender} onChange={e=>setForm({...form, gender:e.target.value})} />
        <input placeholder="Passport" value={form.passportNumber} onChange={e=>setForm({...form, passportNumber:e.target.value})} />
        <input placeholder="Phone" value={form.phoneNumber} onChange={e=>setForm({...form, phoneNumber:e.target.value})} />
        <input placeholder="Address" value={form.address} onChange={e=>setForm({...form, address:e.target.value})} />
        <button type="submit">Save Passenger</button>
      </form>

      <h3>Passenger List</h3>
      <table className="data-table">
        <thead><tr><th>Name</th><th>Passport</th><th>Phone</th><th>Nationality</th></tr></thead>
        <tbody>
          {list.map(p=> <tr key={p.id||p.passportNumber}><td>{p.passengerName}</td><td>{p.passportNumber}</td><td>{p.phoneNumber}</td><td>{p.nationality}</td></tr>)}
        </tbody>
      </table>
    </div>
  )
}
