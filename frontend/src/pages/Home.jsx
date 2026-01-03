import React from 'react'

export default function Home(){
  return (
    <section className="home">
      <div className="hero">
        <h1>Welcome to Falcon Airlines</h1>
        <p>Admin dashboard and tools — now rebuilt with React for a modern UI.</p>
        <div className="hero-actions">
          <button onClick={()=>{window.alert('Explore Delay Prediction from navigation')}}>Get Started</button>
        </div>
      </div>

      <div className="cards">
        <div className="card">
          <h3>Flights</h3>
          <p>Manage flights quickly and view summary stats.</p>
        </div>
        <div className="card">
          <h3>Passengers</h3>
          <p>Manage passenger records and bookings.</p>
        </div>
        <div className="card">
          <h3>Delay Prediction</h3>
          <p>Use ML-powered predictions to estimate delay risk.</p>
        </div>
      </div>
    </section>
  )
}
