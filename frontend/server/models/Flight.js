import mongoose from 'mongoose'
const Schema = mongoose.Schema

const FlightSchema = new Schema({
  flightCode: String,
  source: String,
  destination: String,
  flightDate: String,
  seats: Number
},{ timestamps: true })

export default mongoose.model('Flight', FlightSchema)
