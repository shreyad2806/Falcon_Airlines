import express from 'express'
import cors from 'cors'
import mongoose from 'mongoose'
import dotenv from 'dotenv'
import path from 'path'

dotenv.config()
const app = express()
app.use(cors())
app.use(express.json())

const MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/falcon'

mongoose.connect(MONGODB_URI).then(()=> console.log('MongoDB connected')).catch(err=> console.error('MongoDB error', err))

// Models
import Flight from './models/Flight.js'
import Passenger from './models/Passenger.js'
import Booking from './models/Booking.js'
import Cancellation from './models/Cancellation.js'

// Simple REST endpoints
app.get('/api/flights', async (req,res)=>{ const items = await Flight.find(); res.json(items) })
app.post('/api/flights', async (req,res)=>{ const f = new Flight(req.body); await f.save(); res.status(201).json(f) })

app.get('/api/passengers', async (req,res)=>{ res.json(await Passenger.find()) })
app.post('/api/passengers', async (req,res)=>{ const p=new Passenger(req.body); await p.save(); res.status(201).json(p) })

app.get('/api/bookings', async (req,res)=>{ res.json(await Booking.find()) })
app.post('/api/bookings', async (req,res)=>{ const b=new Booking(req.body); await b.save(); res.status(201).json(b) })

app.get('/api/cancellations', async (req,res)=>{ res.json(await Cancellation.find()) })
app.post('/api/cancellations', async (req,res)=>{ const c=new Cancellation(req.body); await c.save(); res.status(201).json(c) })

// Simple predict route (stub) — adapt to call ML model or existing Python service
app.post('/api/predict', async (req,res)=>{
  const payload = req.body || {}
  // This is a placeholder. Replace with real model inference or proxying to Python backend.
  const risk = Math.round(Math.random()*100)
  res.json({ riskScore: risk, riskLevel: risk>60? 'High':'Low', input: payload })
})

const PORT = process.env.PORT || 5000
app.listen(PORT, ()=> console.log(`Server listening ${PORT}`))
