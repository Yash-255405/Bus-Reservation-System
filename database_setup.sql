CREATE DATABASE bus_reservation_db;
USE bus_reservation_db;

-- Table 1: Buses
CREATE TABLE buses (
    bus_id INT AUTO_INCREMENT PRIMARY KEY,
    bus_name VARCHAR(100) NOT NULL,
    source VARCHAR(50) NOT NULL,
    destination VARCHAR(50) NOT NULL,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL,
    fare DECIMAL(10, 2) NOT NULL
);

-- Table 2: Passengers
CREATE TABLE passengers (
    passenger_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) UNIQUE NOT NULL
);

-- Table 3: Bookings
CREATE TABLE bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    bus_id INT,
    passenger_id INT,
    seats_booked INT NOT NULL,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bus_id) REFERENCES buses(bus_id),
    FOREIGN KEY (passenger_id) REFERENCES passengers(passenger_id)
);

-- Insert dummy data for buses
USE bus_reservation_db;
INSERT INTO buses (bus_name, source, destination, total_seats, available_seats, fare) VALUES 
('Volvo AC Sleeper', 'Varanasi', 'Hyderabad', 40, 40, 2500.00),
('RedBus Express', 'Varanasi', 'Delhi', 50, 50, 1200.00),
('Garuda Travels', 'Hyderabad', 'Bangalore', 35, 35, 1500.00);

INSERT INTO buses (bus_name, source, destination, total_seats, available_seats, fare) VALUES 
('UPSRTC Janrath AC', 'Robertsganj', 'Varanasi', 45, 45, 150.00),
('UPSRTC Pink Express', 'Varanasi', 'Lucknow', 40, 40, 850.00),
('Kashi Vishwanath Travels', 'Varanasi', 'Delhi', 42, 42, 1400.00),
('VRL Sleeper AC', 'Bangalore', 'Mumbai', 30, 30, 2200.00),
('Neeta Volvo', 'Pune', 'Hyderabad', 36, 36, 1800.00),
('Royal Cruiser', 'Kolkata', 'Siliguri', 40, 40, 1500.00),
('Orange Tours & Travels', 'Hyderabad', 'Chennai', 45, 45, 1600.00),
('IntrCity SmartBus', 'Delhi', 'Jaipur', 38, 38, 900.00),
('Mahasagar Travels', 'Ahmedabad', 'Surat', 50, 50, 600.00),
('Zingbus AC Seater', 'Lucknow', 'Kanpur', 48, 48, 250.00);
