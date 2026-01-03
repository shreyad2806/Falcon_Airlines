import React, { useEffect, useState } from 'react'
import { get, post } from '../api'

export default function Bookings(){
  const [passengers, setPassengers] = useState([])
  const [flights, setFlights] = useState([])
  const [bookings, setBookings] = useState([])
  const [form, setForm] = useState({ passengerId:'', flightCode:'', amount:0 })

  useEffect(()=>{ fetchData() },[])
  async function fetchData(){ try{ const [p,f,b]= await Promise.all([get('/passengers'), get('/flights'), get('/bookings')]); setPassengers(p||[]); setFlights(f||[]); setBookings(b||[]) }catch(e){console.error(e)} }

  async function submit(e){ e.preventDefault(); try{ await post('/bookings', form); setForm({ passengerId:'', flightCode:'', amount:0 }); fetchData() }catch(e){alert(e.message)} }

  return (
    <div>
      <h2>Ticket Booking</h2>
      <form onSubmit={submit} className="demo-form">
        <select value={form.passengerId} onChange={e=>setForm({...form, passengerId:e.target.value})}>
          <option value="">Select Passenger</option>
          {passengers.map(p=> <option key={p.id||p.passportNumber} value={p.id||p.passportNumber}>{p.passengerName}</option>)}
        </select>
        <select value={form.flightCode} onChange={e=>setForm({...form, flightCode:e.target.value})}>
          <option value="">Select Flight</option>
          {flights.map(f=> <option key={f.flightCode} value={f.flightCode}>{f.flightCode} — {f.source}→{f.destination}</option>)}
        </select>
        <input type="number" placeholder="Amount" value={form.amount} onChange={e=>setForm({...form, amount:Number(e.target.value)})} />
        <button type="submit">Book Ticket</button>
      </form>

      <h3>Booking List</h3>
      <table className="data-table">
        <thead><tr><th>Passenger</th><th>Flight</th><th>Amount</th></tr></thead>
        <tbody>{bookings.map(b=> <tr key={b.ticketId||JSON.stringify(b)}><td>{b.passengerName||b.passengerId}</td><td>{b.flightCode}</td><td>{b.amount}</td></tr>)}</tbody>
      </table>
    </div>
  )
}
