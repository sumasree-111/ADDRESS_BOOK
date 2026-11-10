#  Addressly – Smart Address Book

**Addressly** is a Java-based desktop address book application designed to manage customer details and their addresses easily. It uses SQLite to store data locally and provides a simple, user-friendly interface with Light and Dark themes.

##  Features

- **Add Customer:** Save customer details and multiple addresses.
- **View Customers:** View saved customer information.
- **Search:** Quickly find customers.
- **Edit Details:** Update customer and address information.
- **Delete:** Move unwanted records to the Recycle Bin.
- **Recycle Bin:** Restore deleted records or permanently delete them.
- **Default Address:** Mark an address as the default address.
- **Light & Dark Mode:** Switch between two interface themes.
- **Database Storage:** Store customer and address information using SQLite.

##  Technologies Used

- **Programming Language:** Java
- **GUI:** Java Swing
- **Database:** SQLite
- **Database Connectivity:** JDBC
- **IDE:** Visual Studio Code

##  Project Structure

```text
ADDRESS_BOOK/
│
├── ex.java
├── addressly.db
└── README.md
```

##  Requirements

- Java JDK installed
- Visual Studio Code or any Java IDE
- SQLite JDBC Driver

##  How to Run

1. Clone this repository:

```bash
git clone https://github.com/sumasree-111/ADDRESS_BOOK.git
```

2. Open the project folder in VS Code.

3. Make sure the SQLite JDBC driver JAR is available in the project folder.

4. Compile the Java file:

```bash
javac -cp ".;sqlite-jdbc.jar" ex.java
```

5. Run the application:

```bash
java -cp ".;sqlite-jdbc.jar" ex
```

*Note: These commands are for Windows. Replace `sqlite-jdbc.jar` with the actual JDBC driver filename if it is different.*

## Project Objective

The main objective of Addressly is to provide a simple and efficient way to manage customer contact information and addresses with database storage and easy record management.


GitHub: [sumasree-111](https://github.com/sumasree-111)

---

