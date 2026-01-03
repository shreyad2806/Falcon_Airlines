import mongoose from 'mongoose'
const Schema = mongoose.Schema

const BookingSchema = new Schema({
  passengerId: String,
  passengerName: String,
  flightCode: String,
  amount: Number,
  ticketId: String
},{ timestamps: true })

export default mongoose.model('Booking', BookingSchema)
