PROJECTDNA - COMPLETE STUDENT PROJECT AND TEAM FORMATION SYSTEM
================================================================

1. PROJECT OVERVIEW
-------------------
ProjectDNA is a beginner-friendly web application for students and project creators. Students can register, maintain skills/interests, browse projects, see skill matches, receive recommendations, and form teams. Creators can create projects and view matching students.

2. TECHNOLOGIES
----------------
Frontend: HTML5, CSS3, Vanilla JavaScript
Backend: Java built-in HTTP Server (com.sun.net.httpserver)
Database: SQLite through JDBC
Data structures: ArrayList, HashSet, HashMap, PriorityQueue concept/ranking

3. EXTERNAL INSTALLATION
------------------------
Required:
- JDK 17 or newer (JDK 24 is also fine).
- VS Code is recommended for editing/running, but not required by the application itself.
- A modern browser such as Chrome or Edge.
- Internet is needed ON FIRST RUN if lib/sqlite-jdbc.jar is missing, because run.bat automatically downloads the JDBC driver. After it is downloaded, the application can use the local JAR.

No Node.js, npm, MySQL, XAMPP, Tomcat, Maven or Live Server is required.

4. HOW TO RUN IN VS CODE
------------------------
1. Extract ProjectDNA_COMPLETE.zip.
2. Open the extracted ProjectDNA folder in VS Code.
3. Make sure JDK is installed. In the VS Code terminal, run: java -version
4. Double-click setup.bat once, or simply double-click run.bat.
5. run.bat downloads the SQLite JDBC driver automatically if necessary, compiles the Java source, creates data/projectdna.db, and starts the server.
6. Open Chrome and visit: http://localhost:8080
7. Keep the run.bat console open while demonstrating.
8. Close the console window to stop the server.

5. DEMO ACCOUNTS
----------------
Student:
Email: student@projectdna.com
Password: 1234

Project Creator:
Email: creator@projectdna.com
Password: 1234

6. DATABASE
-----------
The first successful startup automatically creates data/projectdna.db and these tables:
users, students, creators, projects, project_skills, student_skills, teams, team_members, contact_requests.
Demo accounts and sample projects are inserted only if they do not already exist.

7. SKILL MATCHING
-----------------
Match Percentage = (Number of Matching Skills / Total Required Skills) x 100
Example: required Python, Java, SQL, HTML and student has Python, Java, HTML = 3/4 x 100 = 75%.

8. API ROUTES
-------------
POST /api/login
POST /api/register
GET  /api/dashboard
GET  /api/projects
POST /api/projects
GET  /api/recommendations
GET  /api/profile
POST /api/profile
GET  /api/team
POST /api/team
GET  /api/creator/students
GET  /api/creator/projects
POST /api/logout
POST /api/contact/send
GET  /api/contacts
POST /api/contact/respond

9. FOLDER STRUCTURE
-------------------
src/projectdna/       Java source
web/                  HTML/CSS/JS
lib/                  SQLite JDBC driver (downloaded automatically if absent)
data/                 SQLite database created at runtime
bin/                  compiled .class files
run.bat               compile + run
setup.bat             driver setup helper
README.txt            this guide
VIVA_NOTES.txt        viva answers

10. COMMON ERRORS
-----------------
'java is not recognized': Install JDK 17+ and restart VS Code/Windows terminal.
'SQLite JDBC driver not found': Run setup.bat with internet access. The driver is downloaded automatically.
'Port 8080 already in use': Close the other Java/server program using port 8080, then run again.
'Invalid login': Use the demo credentials exactly and choose the matching account type.
'Unable to connect': The Java server is not running. Start run.bat and keep its console open.

11. VIVA FLOW
-------------
Explain: Browser -> HTML/CSS/JS -> Java HTTP server -> API route -> JDBC -> SQLite -> JSON response -> browser.

12. SECURITY NOTE
-----------------
This is an educational college project. Passwords are stored simply for beginner demonstration and are not suitable for a production application. A real system should use password hashing, sessions/JWT, validation, HTTPS and stronger security controls.

13. CONTACT AND REQUEST SYSTEM
------------------------------
The interactive version includes a simple communication workflow:
- Students can click "Contact Creator" from a project or recommendation.
- Creators can click "Contact" beside a matching student.
- A sender writes a message and sends a project/contact request.
- The receiver sees pending requests in "Contact & Requests".
- The receiver can Accept or Reject the request.
- Accepted connections remain visible in the communication center.
- A notification badge shows the number of pending incoming requests.
- Contact requests are stored in SQLite in the contact_requests table.

This is intentionally simple for a college viva. It is a request/communication system, not a production chat service.

TEAM FORMATION UPDATE
---------------------
Team formation is controlled by Project Creators. The Student Portal no longer shows a
Find Teammates option. Students can browse projects, view recommendations, contact
creators, and manage requests. Creators use Team Formation to find students, compare
skill matches, contact students, select members, and save a team.


MATCH DISPLAY FIX
- Student project pages show the student's own skill match.
- Creator project pages now show the highest skill match among available students instead of comparing the project against the creator profile.
- The creator label is "Top Student Match" to make the meaning clear.


OPEN/CLOSE PROJECT UPDATE
--------------------------
Creators can now control whether a project is accepting students.
- New projects start as OPEN.
- Click "Close Project" to stop student recruitment/contact requests.
- Closed projects remain visible to the creator with a CLOSED status.
- Click "Reopen Project" to make the project available again.
- Students only see OPEN projects and cannot send project contact requests for CLOSED projects.

TEAM FORMATION MATCH FIX
------------------------
Team Formation is now project-specific. A creator selects one of their projects,
and ProjectDNA compares that project's required skills against every student's skills.
The displayed percentage is therefore the student's match for the selected project,
not a comparison against the creator's profile. Saved teams are linked to the selected
project. The Team Formation page also passes the selected project when contacting a student.

PROJECT FILTER FIX
- Creator Projects page now shows only projects owned by the logged-in creator.
- Students continue to see all OPEN projects.

CONTACT REPLY / MESSAGING UPDATE
- Accepted connections now have a Reply button in Contact & Requests.
- Clicking Reply opens the conversation history for that project/contact request.
- The original request message is shown first, followed by replies.
- Both the student and project creator can reply after the request is accepted.
- Replies are stored in the SQLite contact_messages table, so they remain available after refresh/restart.
- Pending requests still use Accept / Reject. Reply becomes available after acceptance.
