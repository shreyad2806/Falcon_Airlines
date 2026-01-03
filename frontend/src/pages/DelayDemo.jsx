import React, { useState } from 'react'

export default function DelayDemo(){
  const [loading,setLoading] = useState(false)
  const [result,setResult] = useState(null)

  async function handleSubmit(e){
    e.preventDefault()
    setLoading(true)
    setResult(null)
    const payload = { sample: true }
    try{
      const res = await fetch('/api/predict', {
        method: 'POST',
        headers: { 'Content-Type':'application/json' },
        body: JSON.stringify(payload)
      })
      if(!res.ok) throw new Error('API error')
      const data = await res.json()
      setResult(data)
    }catch(err){
      setResult({ error: err.message })
    }finally{ setLoading(false) }
  }

  return (
    <section className="delay-demo">
      <h2>Flight Delay Prediction</h2>
      <p>Quick demo form — connect this to your backend `/api/predict` endpoint.</p>
      <form onSubmit={handleSubmit} className="demo-form">
        <div className="row">
          <label>Sample Input</label>
          <input defaultValue="demo" />
        </div>
        <div className="actions">
          <button type="submit" disabled={loading}>{loading? 'Checking...':'Predict'}</button>
        </div>
      </form>
      <div className="result">
        {result ? <pre>{JSON.stringify(result, null, 2)}</pre> : <p>No prediction yet.</p>}
      </div>
    </section>
  )
}
