import client from './client';

export const searchFlights = (params = {}) => client.get('/api/flights', { params });
export const getFlight = (id) => client.get(`/api/flights/${id}`);
export const createFlight = (data) => client.post('/api/flights', data);
export const updateFlight = (id, data) => client.put(`/api/flights/${id}`, data);
export const deleteFlight = (id) => client.delete(`/api/flights/${id}`);
