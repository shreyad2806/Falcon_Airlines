import React from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Navbar from './components/Navbar'
import Home from './pages/Home'
import Flights from './pages/Flights'
import Passengers from './pages/Passengers'
import Bookings from './pages/Bookings'
import Cancellations from './pages/Cancellations'
import DelayPrediction from './pages/DelayPrediction'

export default function App(){
  return (
    <BrowserRouter>
      <div className="app-root">
        <Navbar />
        <main className="container">
          <Routes>
            <Route path="/" element={<Home/>} />
            <Route path="/flights" element={<Flights/>} />
            <Route path="/passengers" element={<Passengers/>} />
            <Route path="/bookings" element={<Bookings/>} />
            <Route path="/cancellations" element={<Cancellations/>} />
            <Route path="/delay" element={<DelayPrediction/>} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}
