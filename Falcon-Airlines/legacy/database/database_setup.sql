-- Database Setup Script for Airline Management System
-- This script creates the required database and tables

-- Create database
CREATE DATABASE IF NOT EXISTS airlinedb;
USE airlinedb;

-- Create FlightTbl
CREATE TABLE IF NOT EXISTS FlightTbl (
    FlCode VARCHAR(10) PRIMARY KEY,
    FlSource VARCHAR(50) NOT NULL,
    FlDest VARCHAR(50) NOT NULL,
    FlDate DATE NOT NULL,
    FlSeats INT NOT NULL
);

-- Create PassengerTbl
CREATE TABLE IF NOT EXISTS PassengerTbl (
    PassId INT AUTO_INCREMENT PRIMARY KEY,
    PassName VARCHAR(100) NOT NULL,
    PassNationality VARCHAR(50) NOT NULL,
    PassGender VARCHAR(10) NOT NULL,
    PassPassport VARCHAR(20) NOT NULL,
    PassPhone VARCHAR(15) NOT NULL,
    PassAddress TEXT NOT NULL
);

-- Create BookingTbl
CREATE TABLE IF NOT EXISTS BookingTbl (
    BookId INT AUTO_INCREMENT PRIMARY KEY,
    BookPassId INT NOT NULL,
    BookPassName VARCHAR(100) NOT NULL,
    BookFlightCode VARCHAR(10) NOT NULL,
    BookGender VARCHAR(10) NOT NULL,
    BookPassport VARCHAR(20) NOT NULL,
    BookNationality VARCHAR(50) NOT NULL,
    BookAmount DECIMAL(10,2) NOT NULL,
    BookDate DATE NOT NULL,
    FOREIGN KEY (BookPassId) REFERENCES PassengerTbl(PassId),
    FOREIGN KEY (BookFlightCode) REFERENCES FlightTbl(FlCode)
);

-- Create CancellationTbl
CREATE TABLE IF NOT EXISTS CancellationTbl (
    CancelId INT AUTO_INCREMENT PRIMARY KEY,
    CancelTicketId INT NOT NULL,
    CancelFlightCode VARCHAR(10) NOT NULL,
    CancelDate DATE NOT NULL,
    FOREIGN KEY (CancelTicketId) REFERENCES BookingTbl(BookId),
    FOREIGN KEY (CancelFlightCode) REFERENCES FlightTbl(FlCode)
);

-- Insert sample data for testing
INSERT INTO FlightTbl (FlCode, FlSource, FlDest, FlDate, FlSeats) VALUES
('FA001', 'Delhi', 'Mumbai', '2024-01-15', 150),
('FA002', 'Mumbai', 'Bangalore', '2024-01-16', 120),
('FA003', 'Bangalore', 'Delhi', '2024-01-17', 180),
('FA004', 'Chennai', 'Kolkata', '2024-01-18', 200),
('FA005', 'Hyderabad', 'Pune', '2024-01-19', 100);

INSERT INTO PassengerTbl (PassName, PassNationality, PassGender, PassPassport, PassPhone, PassAddress) VALUES
('John Doe', 'India', 'Male', 'A12345678', '9876543210', 'Delhi, India'),
('Jane Smith', 'USA', 'Female', 'B87654321', '1234567890', 'Mumbai, India'),
('Mike Johnson', 'UK', 'Male', 'C11223344', '5555555555', 'Bangalore, India'),
('Sarah Wilson', 'Canada', 'Female', 'D99887766', '1111111111', 'Chennai, India'),
('David Brown', 'Australia', 'Male', 'E55443322', '2222222222', 'Hyderabad, India');

INSERT INTO BookingTbl (BookPassId, BookPassName, BookFlightCode, BookGender, BookPassport, BookNationality, BookAmount, BookDate) VALUES
(1, 'John Doe', 'FA001', 'Male', 'A12345678', 'India', 5000.00, '2024-01-10'),
(2, 'Jane Smith', 'FA002', 'Female', 'B87654321', 'USA', 4500.00, '2024-01-11'),
(3, 'Mike Johnson', 'FA003', 'Male', 'C11223344', 'UK', 6000.00, '2024-01-12');

-- Create indexes for better performance
CREATE INDEX idx_flight_code ON FlightTbl(FlCode);
CREATE INDEX idx_passenger_id ON PassengerTbl(PassId);
CREATE INDEX idx_booking_id ON BookingTbl(BookId);
CREATE INDEX idx_cancellation_id ON CancellationTbl(CancelId);

-- Grant permissions (adjust as needed)
GRANT ALL PRIVILEGES ON airlinedb.* TO 'root'@'localhost';
FLUSH PRIVILEGES;

-- Display table structure for verification
DESCRIBE FlightTbl;
DESCRIBE PassengerTbl;
DESCRIBE BookingTbl;
DESCRIBE CancellationTbl;

-- Show sample data
SELECT * FROM FlightTbl LIMIT 5;
SELECT * FROM PassengerTbl LIMIT 5;
SELECT * FROM BookingTbl LIMIT 5; 