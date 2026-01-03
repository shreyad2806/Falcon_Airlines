const API_BASE = '/api'

async function request(path, options){
  const res = await fetch(API_BASE + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if(!res.ok){
    const text = await res.text()
    throw new Error(text || res.statusText)
  }
  return res.status === 204 ? null : res.json()
}

export function get(path){ return request(path, { method: 'GET' }) }
export function post(path, body){ return request(path, { method: 'POST', body: JSON.stringify(body) }) }
export function put(path, body){ return request(path, { method: 'PUT', body: JSON.stringify(body) }) }
export function del(path){ return request(path, { method: 'DELETE' }) }
