import React, { useEffect, useState } from 'react'
import { get, post } from '../api'

export default function Cancellations(){
  const [tickets,setTickets] = useState([])
  const [list,setList] = useState([])
  const [form,setForm] = useState({ ticketId:'', cancellationDate:'' })

  useEffect(()=>{ fetchAll() },[])
  async function fetchAll(){ try{ setTickets(await get('/bookings')||[]); setList(await get('/cancellations')||[]) }catch(e){console.error(e)} }

  async function submit(e){ e.preventDefault(); try{ await post('/cancellations', form); setForm({ ticketId:'', cancellationDate:'' }); fetchAll() }catch(e){alert(e.message)} }

  return (
    <div>
      <h2>Ticket Cancellation</h2>
      <form onSubmit={submit} className="demo-form">
        <select value={form.ticketId} onChange={e=>setForm({...form, ticketId:e.target.value})}>
          <option value="">Select Ticket</option>
          {tickets.map(t=> <option key={t.ticketId||JSON.stringify(t)} value={t.ticketId||t.id}>{t.ticketId||t.id} — {t.passengerName}</option>)}
        </select>
        <input type="date" value={form.cancellationDate} onChange={e=>setForm({...form, cancellationDate:e.target.value})} />
        <button type="submit">Cancel Ticket</button>
      </form>

      <h3>Cancellation List</h3>
      <table className="data-table">
        <thead><tr><th>Cancellation ID</th><th>Ticket ID</th><th>Date</th></tr></thead>
        <tbody>{list.map(c=> <tr key={c.id||JSON.stringify(c)}><td>{c.id}</td><td>{c.ticketId}</td><td>{c.cancellationDate}</td></tr>)}</tbody>
      </table>
    </div>
  )
}
