import React, { useState } from 'react'
import { post } from '../api'

export default function DelayPrediction(){
  const [loading,setLoading] = useState(false)
  const [result,setResult] = useState(null)
  const [form,setForm] = useState({ airline:'AA', origin:'JFK', destination:'LAX', departureTime:'', temperature:75, windSpeed:10, visibility:10, precipitation:0 })

  async function handleSubmit(e){
    e.preventDefault(); setLoading(true); setResult(null)
    try{
      const data = await post('/predict', form)
      setResult(data)
    }catch(err){ setResult({ error: err.message }) }
    finally{ setLoading(false) }
  }

  return (
    <section className="delay-demo">
      <h2>Flight Delay Prediction</h2>
      <p>Submit flight details to `/api/predict`.</p>
      <form onSubmit={handleSubmit} className="demo-form">
        <input value={form.airline} onChange={e=>setForm({...form, airline:e.target.value})} />
        <input value={form.origin} onChange={e=>setForm({...form, origin:e.target.value})} />
        <input value={form.destination} onChange={e=>setForm({...form, destination:e.target.value})} />
        <input type="datetime-local" value={form.departureTime} onChange={e=>setForm({...form, departureTime:e.target.value})} />
        <input type="number" value={form.temperature} onChange={e=>setForm({...form, temperature:Number(e.target.value)})} />
        <input type="number" value={form.windSpeed} onChange={e=>setForm({...form, windSpeed:Number(e.target.value)})} />
        <input type="number" value={form.visibility} onChange={e=>setForm({...form, visibility:Number(e.target.value)})} />
        <input type="number" value={form.precipitation} onChange={e=>setForm({...form, precipitation:Number(e.target.value)})} />
        <button type="submit" disabled={loading}>{loading? 'Predicting...':'Predict'}</button>
      </form>
      <div className="result">{result ? <pre>{JSON.stringify(result,null,2)}</pre> : <p>No prediction yet.</p>}</div>
    </section>
  )
}
