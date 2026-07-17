package busapp;

import java.sql.*;
import java.util.Scanner;

public class BusReservationSystem {

    // Database Credentials
    private static final String URL = "jdbc:mysql://localhost:3306/bus_reservation_db";
    private static final String USER = "root";      
    private static final String PASSWORD = "TIGER"; 

    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            System.out.println("Connected to the Bus Reservation Database successfully!");
            
            // Automatically add dummy buses if the database is empty
            initializeDefaultBuses(conn);
            
            boolean exit = false;

            while (!exit) {
                System.out.println("\n=== Bus Reservation System ===");
                System.out.println("1. View All Buses"); 
                System.out.println("2. Search Buses By Route");
                System.out.println("3. Seat Booking (Transaction & Validation)");
                System.out.println("4. Ticket Cancellation (Transaction)");
                System.out.println("5. Passenger Details");
                System.out.println("6. Exit");
                System.out.print("Enter your choice: ");
                
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1 -> viewAllBuses(conn); 
                    case 2 -> searchBuses(conn);
                    case 3 -> bookSeat(conn);
                    case 4 -> cancelTicket(conn);
                    case 5 -> viewPassengerDetails(conn);
                    case 6 -> exit = true;
                    default -> System.out.println("Invalid choice! Please try again.");
                }
            }
   
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Module 1: View All Buses
    private static void viewAllBuses(Connection conn) throws SQLException {
        String sql = "SELECT * FROM buses";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.println("\n--- All Available Buses ---");
            System.out.printf("%-5s | %-25s | %-15s | %-15s | %-15s | %-10s\n", 
                    "ID", "Bus Name", "Source", "Destination", "Available Seats", "Fare");
            System.out.println("------------------------------------------------------------------------------------------------");
            
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.printf("%-5d | %-25s | %-15s | %-15s | %-15d | ₹%-10.2f\n", 
                        rs.getInt("bus_id"), 
                        rs.getString("bus_name"), 
                        rs.getString("source"), 
                        rs.getString("destination"),
                        rs.getInt("available_seats"), 
                        rs.getDouble("fare"));
            }
            
            if (!found) {
                System.out.println("No buses are currently registered in the system.");
            }
        }
    }

    // Module 2: Search Buses
    private static void searchBuses(Connection conn) throws SQLException {
        System.out.print("Enter Source City: ");
        String source = scanner.nextLine();
        System.out.print("Enter Destination City: ");
        String destination = scanner.nextLine();

        String sql = "SELECT * FROM buses WHERE source = ? AND destination = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, source);
            pstmt.setString(2, destination);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\n--- Available Buses ---");
                System.out.printf("%-5s | %-20s | %-15s | %-10s\n", "ID", "Bus Name", "Available Seats", "Fare");
                System.out.println("---------------------------------------------------------------");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.printf("%-5d | %-20s | %-15d | ₹%-10.2f\n", 
                            rs.getInt("bus_id"), rs.getString("bus_name"), 
                            rs.getInt("available_seats"), rs.getDouble("fare"));
                }
                if (!found) System.out.println("No buses found for this route.");
            }
        }
    }

    // Module 3: Seat Booking (Demonstrating Validation & Transactions)
    private static void bookSeat(Connection conn) {
        try {
            conn.setAutoCommit(false); 

            System.out.print("Enter Bus ID: ");
            int busId = scanner.nextInt();
            System.out.print("Enter Number of Seats to Book: ");
            int seatsToBook = scanner.nextInt();
            scanner.nextLine(); 

            String checkSeatsSQL = "SELECT available_seats, fare FROM buses WHERE bus_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSeatsSQL)) {
                checkStmt.setInt(1, busId);
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next()) {
                    int availableSeats = rs.getInt("available_seats");
                    double fare = rs.getDouble("fare");

                    if (availableSeats < seatsToBook) {
                        System.out.println("Booking Failed: Only " + availableSeats + " seats available.");
                        conn.rollback(); 
                        return;
                    }

                    System.out.print("Enter Passenger Name: ");
                    String name = scanner.nextLine();
                    
                    String phone = "";
                    while (true) {
                        System.out.print("Enter Passenger Phone (10 digits only): ");
                        phone = scanner.nextLine();
                        if (phone.matches("\\d{10}")) {
                            break; 
                        } else {
                            System.out.println("Invalid input! Phone number must be exactly 10 digits.");
                        }
                    }

                    int passengerId = getOrInsertPassenger(conn, name, phone);

                    String insertBookingSQL = "INSERT INTO bookings (bus_id, passenger_id, seats_booked) VALUES (?, ?, ?)";
                    try (PreparedStatement bookStmt = conn.prepareStatement(insertBookingSQL, Statement.RETURN_GENERATED_KEYS)) {
                        bookStmt.setInt(1, busId);
                        bookStmt.setInt(2, passengerId);
                        bookStmt.setInt(3, seatsToBook);
                        bookStmt.executeUpdate();

                        ResultSet generatedKeys = bookStmt.getGeneratedKeys();
                        int bookingId = 0;
                        if (generatedKeys.next()) {
                            bookingId = generatedKeys.getInt(1);
                        }

                        String updateBusSQL = "UPDATE buses SET available_seats = available_seats - ? WHERE bus_id = ?";
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateBusSQL)) {
                            updateStmt.setInt(1, seatsToBook);
                            updateStmt.setInt(2, busId);
                            updateStmt.executeUpdate();
                        }

                        conn.commit();
                        System.out.println("Booking Successful! Booking ID: " + bookingId);
                        System.out.println("Total Amount to Pay: ₹" + (fare * seatsToBook));
                    }
                } else {
                    System.out.println("Invalid Bus ID.");
                    conn.rollback();
                }
            }
        } catch (SQLException e) {
            try {
                System.out.println("Error occurred during booking. Rolling back changes.");
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                conn.setAutoCommit(true); 
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Helper method for Booking
    private static int getOrInsertPassenger(Connection conn, String name, String phone) throws SQLException {
        String insertSQL = "INSERT INTO passengers (name, phone) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, phone);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLIntegrityConstraintViolationException e) {
            String selectSQL = "SELECT passenger_id FROM passengers WHERE phone = ?";
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSQL)) {
                selectStmt.setString(1, phone);
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) return rs.getInt("passenger_id");
            }
        }
        return -1;
    }

    // Module 4: Ticket Cancellation (Transaction)
    private static void cancelTicket(Connection conn) {
        try {
            conn.setAutoCommit(false); 

            System.out.print("Enter Booking ID to cancel: ");
            int bookingId = scanner.nextInt();

            String getBookingSQL = "SELECT bus_id, seats_booked FROM bookings WHERE booking_id = ?";
            try (PreparedStatement getStmt = conn.prepareStatement(getBookingSQL)) {
                getStmt.setInt(1, bookingId);
                ResultSet rs = getStmt.executeQuery();

                if (rs.next()) {
                    int busId = rs.getInt("bus_id");
                    int seatsToReturn = rs.getInt("seats_booked");

                    String deleteBookingSQL = "DELETE FROM bookings WHERE booking_id = ?";
                    try (PreparedStatement delStmt = conn.prepareStatement(deleteBookingSQL)) {
                        delStmt.setInt(1, bookingId);
                        delStmt.executeUpdate();
                    }

                    String updateBusSQL = "UPDATE buses SET available_seats = available_seats + ? WHERE bus_id = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateBusSQL)) {
                        updateStmt.setInt(1, seatsToReturn);
                        updateStmt.setInt(2, busId);
                        updateStmt.executeUpdate();
                    }

                    conn.commit(); 
                    System.out.println("Ticket cancelled successfully. Seats returned to inventory.");
                } else {
                    System.out.println("Booking ID not found.");
                    conn.rollback();
                }
            }
        } catch (SQLException e) {
            try {
                System.out.println("Error cancelling ticket. Rolling back.");
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Module 5: Passenger Details
    private static void viewPassengerDetails(Connection conn) throws SQLException {
        System.out.print("Enter Passenger Phone Number to view their bookings: ");
        String phone = scanner.nextLine();

        String sql = "SELECT p.name, b.booking_id, bus.bus_name, bus.source, bus.destination, b.seats_booked, b.booking_date " +
                     "FROM passengers p " +
                     "JOIN bookings b ON p.passenger_id = b.passenger_id " +
                     "JOIN buses bus ON b.bus_id = bus.bus_id " +
                     "WHERE p.phone = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, phone);
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\n--- Passenger Booking History ---");
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    if (rs.isFirst()) {
                        System.out.println("Passenger Name: " + rs.getString("name"));
                        System.out.println("---------------------------------------------------------------");
                    }
                    System.out.printf("Booking ID: %d | Bus: %s (%s to %s) | Seats: %d | Date: %s\n",
                            rs.getInt("booking_id"), rs.getString("bus_name"), 
                            rs.getString("source"), rs.getString("destination"),
                            rs.getInt("seats_booked"), rs.getTimestamp("booking_date"));
                }
                if (!found) System.out.println("No bookings found for this phone number.");
            }
        }
    }

    // Automatic Base Data Populator using Batch Processing
    private static void initializeDefaultBuses(Connection conn) {
        String checkSQL = "SELECT COUNT(*) FROM buses";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkSQL)) {
            
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("No bus records found. Populating default bus routes...");
                
                String insertSQL = "INSERT INTO buses (bus_name, source, destination, total_seats, available_seats, fare) VALUES (?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                    Object[][] busData = {
                        {"Volvo AC Sleeper", "Varanasi", "Hyderabad", 40, 40, 2500.00},
                        {"RedBus Express", "Varanasi", "Delhi", 50, 50, 1200.00},
                        {"Garuda Travels", "Hyderabad", "Bangalore", 35, 35, 1500.00},
                        {"UPSRTC Janrath AC", "Robertsganj", "Varanasi", 45, 45, 150.00},
                        {"UPSRTC Pink Express", "Varanasi", "Lucknow", 40, 40, 850.00},
                        {"Kashi Vishwanath Travels", "Varanasi", "Delhi", 42, 42, 1400.00},
                        {"VRL Sleeper AC", "Bangalore", "Mumbai", 30, 30, 2200.00},
                        {"Neeta Volvo", "Pune", "Hyderabad", 36, 36, 1800.00},
                        {"Royal Cruiser", "Kolkata", "Siliguri", 40, 40, 1500.00},
                        {"Orange Tours & Travels", "Hyderabad", "Chennai", 45, 45, 1600.00},
                        {"IntrCity SmartBus", "Delhi", "Jaipur", 38, 38, 900.00},
                        {"Mahasagar Travels", "Ahmedabad", "Surat", 50, 50, 600.00},
                        {"Zingbus AC Seater", "Lucknow", "Kanpur", 48, 48, 250.00}
                    };

                    for (Object[] bus : busData) {
                        pstmt.setString(1, (String) bus[0]);
                        pstmt.setString(2, (String) bus[1]);
                        pstmt.setString(3, (String) bus[2]);
                        pstmt.setInt(4, (Integer) bus[3]);
                        pstmt.setInt(5, (Integer) bus[4]);
                        pstmt.setDouble(6, (Double) bus[5]);
                        pstmt.addBatch();
                    }
                    
                    pstmt.executeBatch(); 
                    System.out.println("Successfully added " + busData.length + " default bus routes into the database!");
                }
            }
        } catch (SQLException e) {
            System.out.println("Notice: Default bus setup check skipped. (" + e.getMessage() + ")");
        }
    }
}