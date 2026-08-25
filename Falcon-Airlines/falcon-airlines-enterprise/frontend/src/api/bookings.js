import client from './client';

export const getBooking = (id) => client.get(`/api/bookings/${id}`);
export const getBookingByRef = (ref) => client.get(`/api/bookings/reference/${ref}`);
export const createBooking = (data) => client.post('/api/bookings', data);
export const cancelBooking = (id, reason) => client.post(`/api/bookings/${id}/cancel`, { cancellationReason: reason });
export const checkSeatAvailability = (flightId) => client.get('/api/bookings/seats/availability', { params: { flightId } });
