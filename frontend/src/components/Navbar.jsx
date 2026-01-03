import React from 'react'
import { NavLink } from 'react-router-dom'

export default function Navbar(){
  return (
    <header className="nav">
      <div className="nav-inner">
        <div className="brand">Falcon Airlines</div>
        <nav className="nav-links">
          <NavLink to="/" end className={({isActive})=> isActive? 'active':''}>Home</NavLink>
          <NavLink to="/flights" className={({isActive})=> isActive? 'active':''}>Flights</NavLink>
          <NavLink to="/passengers" className={({isActive})=> isActive? 'active':''}>Passengers</NavLink>
          <NavLink to="/bookings" className={({isActive})=> isActive? 'active':''}>Bookings</NavLink>
          <NavLink to="/cancellations" className={({isActive})=> isActive? 'active':''}>Cancellations</NavLink>
          <NavLink to="/delay" className={({isActive})=> isActive? 'active':''}>Delay Prediction</NavLink>
        </nav>
      </div>
    </header>
  )
}
