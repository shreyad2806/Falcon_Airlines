import client from './client';

export const searchAirports = (params = {}) => client.get('/api/airports', { params });
export const getAirport = (id) => client.get(`/api/airports/${id}`);
export const createAirport = (data) => client.post('/api/airports', data);
