import client from './client';

export const holdSeat = (flightId, seatId) =>
  client.post('/api/payments/hold-seat', { flightId, seatId });

export const releaseSeat = (flightId, seatId) =>
  client.post('/api/payments/release-seat', null, { params: { flightId, seatId } });

export const processPayment = (bookingId, paymentMethod) =>
  client.post('/api/payments/process', { bookingId, paymentMethod });

export const simulateFailure = (bookingId) =>
  client.post('/api/payments/simulate-failure', null, { params: { bookingId } });

export const getPaymentByBooking = (bookingId) =>
  client.get(`/api/payments/booking/${bookingId}`);
