# Smart Hospital Patient Management System

## Data Structures Mini Project

This project is a web-based Smart Hospital Patient Management System implemented using **Java, HTML, CSS and JavaScript**.

The project demonstrates two important data structures:

- **Queue** — stores patients in consultation order using FIFO (First-In, First-Out).
- **HashMap** — stores patient records using a unique patient ID, allowing quick patient lookup.

### Features

1. Add/register a new patient.
2. Automatically place the patient into the waiting queue.
3. Display the current waiting list.
4. Call the next patient using FIFO order.
5. Search for a patient using their unique patient ID.
6. Complete/remove a patient's appointment.
7. Responsive hospital dashboard UI.
8. Java backend and browser-based frontend in one folder.

## Technologies Used

- Java
- Java Collections Framework
- `Queue`
- `HashMap`
- HTML5
- CSS3
- JavaScript
- Java built-in `HttpServer`

No external Java libraries are required.

## Project Structure

```text
Smart-Hospital-Patient-Management-System/
│
├── Main.java
├── index.html
├── style.css
├── script.js
└── README.md
```

## How to Run

### 1. Install Java

Install **JDK 11 or newer**.

Check the installation:

```bash
java -version
javac -version
```

### 2. Open the project folder

Open Command Prompt / PowerShell / Terminal inside this folder.

### 3. Compile

```bash
javac Main.java
```

### 4. Start the server

```bash
java Main
```

You should see:

```text
Smart Hospital Patient Management System
Open: http://localhost:8080
```

### 5. Open the website

Open this address in your browser:

```text
http://localhost:8080
```

## Data Structure Explanation

### Queue

The waiting list uses a Queue:

```java
private final Queue<String> waitingQueue = new LinkedList<>();
```

When a patient is registered:

```java
waitingQueue.offer(patient.getId());
```

When the doctor calls the next patient:

```java
String id = waitingQueue.poll();
```

This follows FIFO:

```text
First patient added → First patient called
```

### HashMap

Patient records are stored in:

```java
private final HashMap<String, Patient> patients = new HashMap<>();
```

The patient ID is used as the key:

```java
patients.put(patient.getId(), patient);
```

A patient can then be searched directly using the ID:

```java
return patients.get(id);
```

## Project Flow

```text
User
  ↓
HTML/CSS/JavaScript Website
  ↓
Java HTTP Server
  ↓
PatientManager
  ↓
Queue + HashMap
```

## Important Note

The browser interface communicates with the Java backend through HTTP endpoints. The Java program must be running before opening the website.

For the college submission, upload this complete folder to a **public GitHub repository** and submit that repository link in the Google Sheet.

## Suggested GitHub Repository Name

`smart-hospital-patient-management-system`

## Suggested Project Title

**Smart Hospital Patient Management System Using Queue and HashMap**


### Java File Structure

To make GitHub submission simpler, all Java classes are contained in **Main.java**:

- `Patient` — patient model
- `PatientManager` — Queue and HashMap logic
- `Main` — Java HTTP server and API

Compile everything with only:

```bash
javac Main.java
```
