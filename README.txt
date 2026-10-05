PROJECTDNA – STUDENT PROJECT AND TEAM FORMATION SYSTEM
======================================================

1. PROJECT OVERVIEW
-------------------

ProjectDNA is a web-based application designed to help students find and form project teams based on their skills and interests.

Students can create an account, add their skills and interests, browse available projects, check how well their skills match a project, receive project recommendations, and communicate with project creators.

Project creators can create and manage projects, view students who match their required skills, contact suitable students, and form teams for their projects.

The main purpose of ProjectDNA is to make finding suitable project teammates easier and more organized. Instead of manually searching for students with the required skills, the system uses skill matching to help identify suitable candidates.


2. TECHNOLOGIES USED
--------------------

ProjectDNA uses simple technologies so that the application is easy to understand, run, and demonstrate as a college project.

Frontend:
- HTML5
- CSS3
- JavaScript

Backend:
- Java
- Java Built-in HTTP Server (com.sun.net.httpserver)

Database:
- SQLite
- JDBC

Data Structures and Concepts:
- ArrayList
- HashSet
- HashMap
- PriorityQueue
- Skill-based ranking and matching


3. REQUIREMENTS AND INSTALLATION
--------------------------------

The following are required to run ProjectDNA:

- JDK 17 or later
- VS Code is recommended for editing and running the project
- Google Chrome, Microsoft Edge, or another modern web browser
- Internet connection during the first run if the SQLite JDBC driver is not available

The application can automatically download the SQLite JDBC driver when required.

The following software is NOT required:

- Node.js
- npm
- MySQL
- XAMPP
- Tomcat
- Maven
- Live Server

This makes ProjectDNA simple to set up for beginners.


4. HOW TO RUN THE PROJECT
-------------------------

Step 1:
Extract the ProjectDNA folder.

Step 2:
Open the extracted ProjectDNA folder in VS Code.

Step 3:
Make sure Java is installed.

Open the VS Code terminal and run:

java -version

Make sure JDK 17 or a newer version is installed.

Step 4:
Run setup.bat once if required.

Step 5:
Run run.bat.

The run.bat file automatically:

1. Checks whether the SQLite JDBC driver is available.
2. Downloads the driver if necessary.
3. Compiles the Java source files.
4. Creates the SQLite database.
5. Starts the Java web server.

Step 6:
Open Google Chrome or Microsoft Edge.

Visit:

http://localhost:8080

Step 7:
Keep the run.bat console window open while using the application.

Step 8:
To stop the application, close the server/terminal window.


5. DEMO ACCOUNTS
----------------

For demonstration purposes, ProjectDNA includes sample accounts.

STUDENT ACCOUNT

Email:
student@projectdna.com

Password:
1234


PROJECT CREATOR ACCOUNT

Email:
creator@projectdna.com

Password:
1234

These accounts can be used to demonstrate both the Student Portal and Creator Portal.


6. DATABASE
-----------

ProjectDNA uses SQLite as its database.

The database file is stored at:

data/projectdna.db

The database is automatically created when the application starts successfully.

The main database tables are:

- users
- students
- creators
- projects
- project_skills
- student_skills
- teams
- team_members
- contact_requests
- contact_messages

The application also creates the demo accounts and sample projects if they do not already exist in the database.


7. SKILL MATCHING
-----------------

One of the main features of ProjectDNA is skill matching.

The system compares the skills required for a project with the skills available in a student's profile.

The matching percentage is calculated using:

Match Percentage =
(Number of Matching Skills / Total Required Skills) × 100


Example:

Suppose a project requires:

- Python
- Java
- SQL
- HTML

A student has:

- Python
- Java
- HTML

The student matches 3 out of the 4 required skills.

Therefore:

3 / 4 × 100 = 75%

The system will display a 75% skill match for that student and project.


8. MAIN API ROUTES
------------------

The frontend communicates with the Java backend using API routes.

Important API routes include:

POST /api/login
POST /api/register

GET /api/dashboard
GET /api/projects
POST /api/projects

GET /api/recommendations

GET /api/profile
POST /api/profile

GET /api/team
POST /api/team

GET /api/creator/students
GET /api/creator/projects

POST /api/logout

POST /api/contact/send
GET /api/contacts
POST /api/contact/respond

These routes handle login, registration, project management, recommendations, team formation, contact requests, and communication.


9. PROJECT FOLDER STRUCTURE
---------------------------

ProjectDNA/
|
|-- src/projectdna/
|   |-- Java backend source files
|
|-- web/
|   |-- HTML pages
|   |-- css/
|   |-- js/
|
|-- lib/
|   |-- SQLite JDBC driver
|
|-- data/
|   |-- SQLite database
|
|-- bin/
|   |-- Compiled Java files
|
|-- run.bat
|-- setup.bat
|-- README.txt
|-- VIVA_NOTES.txt


The src folder contains the Java backend source code.

The web folder contains the frontend pages, CSS, and JavaScript.

The lib folder contains the SQLite JDBC driver.

The data folder contains the SQLite database.

The bin folder contains the compiled Java class files.

The run.bat file is used to compile and start the application.

The setup.bat file helps set up the SQLite JDBC driver.


10. HOW THE APPLICATION WORKS
----------------------------

The basic working flow of ProjectDNA is:

User
  ↓
Browser
  ↓
HTML / CSS / JavaScript
  ↓
Java HTTP Server
  ↓
API Route
  ↓
JDBC
  ↓
SQLite Database
  ↓
JSON Response
  ↓
Browser


For example, when a student logs in, the browser sends the login information to the Java backend.

The Java backend checks the information stored in the SQLite database.

The backend then sends the result back to the browser in JSON format.

The frontend displays the appropriate page based on the response.


11. CONTACT AND REQUEST SYSTEM
------------------------------

ProjectDNA includes a simple communication system between students and project creators.

Students can:

- Contact project creators.
- Send project or contact requests.
- View incoming requests.
- Accept or manage requests.
- Reply to accepted connections.

Project creators can:

- View suitable students.
- Contact students.
- Send requests.
- Accept or reject incoming requests.
- Reply to accepted connections.

The contact requests are stored in the SQLite database.

A notification badge is also displayed when there are pending incoming requests.

This feature is designed as a simple college-level communication system rather than a complete real-time chat application.


12. TEAM FORMATION
------------------

Team formation is mainly controlled by the Project Creator.

The Student Portal does not contain a separate "Find Teammates" option.

Students can:

- Browse projects.
- View their skill matches.
- Receive project recommendations.
- Contact project creators.
- Manage contact requests.

Project creators can use the Team Formation section to:

1. Select one of their projects.
2. View students who match the project's required skills.
3. Compare their skill match percentages.
4. Contact suitable students.
5. Select team members.
6. Save the team for the selected project.

This makes team formation project-specific and more organized.


13. MATCH DISPLAY FIX
---------------------

The matching system displays different information depending on the user's role.

Student View:

Students can see their own skill match for each project.

Creator View:

Creators can see the highest matching student for their project.

The creator-side label is:

"Top Student Match"

This makes it clear that the percentage represents the best matching student and not the creator's own skills.


14. OPEN AND CLOSE PROJECT FEATURE
----------------------------------

Project creators can control whether their project is accepting students.

When a new project is created, it starts as:

OPEN

The creator can click:

"Close Project"

when they no longer want to accept new students.

A closed project remains visible to the creator but is displayed with:

CLOSED

The creator can later click:

"Reopen Project"

to make the project available again.

Students can only see projects that are currently OPEN.

Students cannot send project contact requests for CLOSED projects.


15. PROJECT FILTERING
---------------------

Project visibility depends on the type of user.

Creator:

A creator can see only the projects created by their own account.

This prevents projects belonging to other creators from appearing in their personal project list.

Student:

Students can browse all available OPEN projects.

This allows students to discover projects that match their skills and interests.


16. REPLY AND MESSAGING FEATURE
------------------------------

The communication system also allows users to continue a conversation after a request has been accepted.

Once a request is accepted, a "Reply" button becomes available.

When the user clicks Reply:

- The conversation history is displayed.
- The original request message is shown first.
- New replies appear below it.
- Both students and project creators can continue the conversation.

The messages are stored in the SQLite database using the:

contact_messages

table.

Because the messages are stored in the database, they remain available even after refreshing the page or restarting the application.


17. COMMON PROBLEMS AND SOLUTIONS
---------------------------------

Problem 1:
"java is not recognized"

Solution:
Install JDK 17 or a newer version and restart VS Code or the terminal.


Problem 2:
"SQLite JDBC driver not found"

Solution:
Run setup.bat with an internet connection.

Normally, run.bat can also download the SQLite JDBC driver automatically.


Problem 3:
"Port 8080 already in use"

Solution:
Another application may already be using port 8080.

Close the other Java/server application and start ProjectDNA again.


Problem 4:
"Invalid login"

Solution:
Make sure the correct email, password, and account type are being used.

Use the demo credentials provided in this document.


Problem 5:
"Unable to connect to server"

Solution:
The Java server is probably not running.

Start run.bat and keep the server window open while using the website.


18. SECURITY NOTE
-----------------

ProjectDNA is an educational college project, so the authentication system is intentionally kept simple.

Passwords are stored in a basic form for demonstration purposes.

A real-world application would require stronger security features such as:

- Password hashing
- Secure sessions or JWT
- Input validation
- HTTPS
- Strong authentication and authorization
- Better database security

Therefore, ProjectDNA should be considered a college-level prototype and not a production-ready application.


19. VIVA EXPLANATION
--------------------

If asked how ProjectDNA works, the basic explanation is:

"ProjectDNA is a web application where the frontend is developed using HTML, CSS, and JavaScript. The backend is developed using Java's built-in HTTP server. The frontend communicates with the backend through API routes. The backend processes the requests and uses JDBC to communicate with the SQLite database. The database stores information about users, students, projects, skills, teams, and contact requests. The results are returned to the frontend in JSON format and displayed to the user."


20. CONCLUSION
--------------

ProjectDNA brings project discovery, skill matching, recommendations, communication, and team formation together in a single application.

The main goal of the project is to make it easier for students and project creators to find suitable team members based on skills and project requirements.

The project demonstrates several important concepts, including:

- Web development
- Java backend programming
- REST-style API communication
- SQLite database management
- JDBC
- Data structures
- Skill matching
- Recommendation logic
- Team formation
- Client-server communication

Overall, ProjectDNA provides a simple and practical solution for student project team formation while demonstrating how frontend, backend, database, and data structure concepts can work together in one application.
