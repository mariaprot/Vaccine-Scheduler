import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import src.main.scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
               reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Create patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Create patient failed");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }
    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in, try again");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login patient failed");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login patient failed");
        } else {
            System.out.println("Logged in as " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // check if someone is logged in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }
        String date = tokens[1];

        // for all rows, output the ones with the date matching the string
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String selectCaregiver = "SELECT * FROM Availabilities WHERE Time = ? ORDER BY Username";
        String selectVaccine = "SELECT * FROM Vaccines";

        try {
            PreparedStatement getCaregiver = con.prepareStatement(selectCaregiver);
            PreparedStatement getVaccine = con.prepareStatement(selectVaccine);

            getCaregiver.setString(1, date);

            ResultSet resultSet = getCaregiver.executeQuery();
            ResultSet resultSet1 = getVaccine.executeQuery();

            while(resultSet.next() & resultSet1.next()) {
                System.out.println(resultSet.getString(2) + " " + resultSet1.getString(1)
                        + " " + resultSet1.getInt(2));
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }

        if (currentPatient == null) {
            System.out.println("Please login as a patient");
            return;
        }

        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again");
            return;
        }

        String date = tokens[1];
        String vaccine = tokens[2];

        // for all rows, output the ones with the date matching the string
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            String caregiverAvailability = "SELECT * FROM Availabilities WHERE Time = ? ORDER BY USERNAME";
            String vaccineAvailability = "SELECT * FROM Vaccines WHERE Name = ? AND Doses > 0";

            PreparedStatement getCaregiver = con.prepareStatement(caregiverAvailability);
            PreparedStatement getVaccine = con.prepareStatement(vaccineAvailability);

            getCaregiver.setString(1, date);
            getVaccine.setString(1, vaccine);

            ResultSet caregiverSet = getCaregiver.executeQuery();
            ResultSet vaccineSet = getVaccine.executeQuery();

            String caregiver = null;
            if (!caregiverSet.next()) {
                System.out.println("No caregiver is available");
                return;
            } else {
                caregiver = caregiverSet.getString("Username");
            }
            String updateAvailabilities = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?";
            PreparedStatement deleteDate = con.prepareStatement(updateAvailabilities);


            Vaccine vaccine1 = new Vaccine.VaccineBuilder(vaccine, 0).build();
            if(!vaccineSet.next()) {
                System.out.println("Not enough doses");
                return;
            }
            String updateVaccines = "UPDATE Vaccines SET Doses = Doses - 1 WHERE Name = ?";
            PreparedStatement decreaseDose = con.prepareStatement(updateVaccines);

            String createAppointment = "INSERT INTO Appointments(Time, Name, PatientUsername, CaregiverUsername) " +
                    "VALUES (?, ?, ?, ?)";

            try {
                PreparedStatement getAppointment = con.prepareStatement(createAppointment, Statement.RETURN_GENERATED_KEYS);

                getAppointment.setString(1, date);
                getAppointment.setString(2, vaccine);
                getAppointment.setString(3, currentPatient.getUsername());

                if (usernameExistsCaregiver(caregiver)) {
                    getAppointment.setString(4, caregiver);

                    int affectedRows = getAppointment.executeUpdate();

                    if (affectedRows > 0) {
                        ResultSet generatedKeys = getAppointment.getGeneratedKeys();

                        deleteDate.setString(1, date);
                        deleteDate.setString(2, caregiver);
                        deleteDate.executeUpdate();

                        decreaseDose.setString(1, vaccine);
                        decreaseDose.executeUpdate();

                        if (generatedKeys.next()) {
                            int appointmentID = generatedKeys.getInt(1);
                            System.out.println("Appointment ID " + appointmentID +
                                    ", Caregiver username " + caregiver);
                        } else {
                            System.out.println("Please try again");
                        }
                    } else {
                        System.out.println("Please try again");
                    }
                }
            } catch (SQLException e) {
                System.out.println("Please try again");
                e.printStackTrace();
            }


        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }

        if (tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }

        int id = Integer.parseInt(tokens[1]);

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            String getDate = "";
            String deleteAppointment = "";
            if (currentCaregiver != null) {
                getDate = "SELECT * FROM Appointments WHERE ID = ? AND CaregiverUsername = ?";
                deleteAppointment = "DELETE FROM Appointments WHERE ID = ? AND CaregiverUsername = ?";
            } else if (currentPatient != null) {
                getDate = "SELECT * FROM Appointments WHERE ID = ? AND PatientUsername = ?";
                deleteAppointment = "DELETE FROM Appointments WHERE ID = ? AND PatientUsername = ?";
            }

            PreparedStatement aptDate = con.prepareStatement(getDate);
            aptDate.setInt(1, id);

            if (currentCaregiver != null) {
                aptDate.setString(2, currentCaregiver.getUsername());
            } else if (currentPatient != null) {
                aptDate.setString(2, currentPatient.getUsername());
            }


            ResultSet rs = aptDate.executeQuery();

            if (rs.next()) {
                Date date = rs.getDate(2);
                String vaccine = rs.getString(3);

                PreparedStatement appointment = con.prepareStatement(deleteAppointment);
                appointment.setInt(1, id);
                if (currentCaregiver != null) {
                    appointment.setString(2, currentCaregiver.getUsername());
                } else if (currentPatient != null) {
                    appointment.setString(2, currentPatient.getUsername());
                }

                String updateVaccines = "UPDATE Vaccines SET Doses = Doses + 1 WHERE Name = ?";
                PreparedStatement decreaseDose = con.prepareStatement(updateVaccines);

                decreaseDose.setString(1, vaccine);
                decreaseDose.executeQuery();

                int affectedRows = appointment.executeUpdate();

                if (affectedRows > 0) {
                    System.out.println("Canceled appointment with ID " + id);

                }
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }


    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("Please login first");
            return;
        }

        if (tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }

        // for all rows, output the ones with the date matching the string
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            if(currentCaregiver != null) {
                String appointment = "SELECT * FROM Appointments WHERE CaregiverUsername = ? ORDER BY ID";
                PreparedStatement getAppointment = con.prepareStatement(appointment);
                getAppointment.setString(1, currentCaregiver.getUsername());
                ResultSet rs = getAppointment.executeQuery();

                while (rs.next()) {
                    System.out.println(rs.getInt(1) + " " + rs.getString(3) + " " +
                            rs.getDate(2) + " " + rs.getString(4));
                }
            }

            if(currentPatient != null) {
                String appointment = "SELECT * FROM Appointments WHERE PatientUsername = ? ORDER BY ID";
                PreparedStatement getAppointment = con.prepareStatement(appointment);
                getAppointment.setString(1, currentPatient.getUsername());
                ResultSet rs = getAppointment.executeQuery();

                while (rs.next()) {
                    System.out.println(rs.getInt(1) + " " +  rs.getString(3) + " " +
                            rs.getDate(2) + " " + rs.getString(5));
                }
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
        }
        if (tokens.length != 2) {
            System.out.println("Please try again");
        } else {
            try {
                currentPatient = null;
                currentCaregiver = null;
                System.out.println("Successfully logged out.");
            } catch (Exception e) {
                System.out.println("Please try again");
                e.printStackTrace();
            }
        }
    }
}
