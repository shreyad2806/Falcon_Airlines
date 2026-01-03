import React, { useState } from 'react'
import Navbar from './components/Navbar'
import Home from './pages/Home'
import DelayDemo from './pages/DelayDemo'

export default function App(){
  const [route,setRoute] = useState('home')
  return (
    <div className="app-root">
      <Navbar onNavigate={setRoute} active={route} />
      <main className="container">
        {route === 'home' && <Home />}
        {route === 'delay' && <DelayDemo />}
      </main>
    </div>
  )
}
