import React from 'react'

export default function Navbar({ onNavigate, active }){
  return (
    <header className="nav">
      <div className="nav-inner">
        <div className="brand">Falcon Airlines</div>
        <nav className="nav-links">
          <button className={active==='home'? 'active':''} onClick={()=>onNavigate('home')}>Home</button>
          <button className={active==='delay'? 'active':''} onClick={()=>onNavigate('delay')}>Delay Prediction</button>
        </nav>
      </div>
    </header>
  )
}
