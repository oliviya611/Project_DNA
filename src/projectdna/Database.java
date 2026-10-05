package projectdna;
import java.sql.*;
public class Database {
 static final String URL="jdbc:sqlite:data/projectdna.db";
 static Connection conn() throws SQLException { return DriverManager.getConnection(URL); }
 static void init() throws Exception {
  Class.forName("org.sqlite.JDBC");
  try(Connection c=conn(); Statement s=c.createStatement()){
   s.executeUpdate("PRAGMA foreign_keys=ON");
   s.executeUpdate("CREATE TABLE IF NOT EXISTS users(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,email TEXT UNIQUE NOT NULL,password TEXT NOT NULL,type TEXT NOT NULL)");
   s.executeUpdate("CREATE TABLE IF NOT EXISTS students(user_id INTEGER PRIMARY KEY,interests TEXT,skills TEXT,FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)");
   s.executeUpdate("CREATE TABLE IF NOT EXISTS creators(user_id INTEGER PRIMARY KEY,organization TEXT,expertise TEXT,FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)");
   s.executeUpdate("CREATE TABLE IF NOT EXISTS projects(id INTEGER PRIMARY KEY AUTOINCREMENT,creator_id INTEGER NOT NULL,title TEXT NOT NULL,description TEXT,required_skills TEXT NOT NULL,FOREIGN KEY(creator_id) REFERENCES users(id) ON DELETE CASCADE)");
   try { s.executeUpdate("ALTER TABLE projects ADD COLUMN status TEXT NOT NULL DEFAULT 'OPEN'"); } catch(SQLException ignored) { /* status column already exists */ }
   s.executeUpdate("UPDATE projects SET status='OPEN' WHERE status IS NULL OR status=''");
   s.executeUpdate("CREATE TABLE IF NOT EXISTS project_skills(id INTEGER PRIMARY KEY AUTOINCREMENT,project_id INTEGER,skill TEXT,FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE CASCADE)");
   s.executeUpdate("CREATE TABLE IF NOT EXISTS student_skills(id INTEGER PRIMARY KEY AUTOINCREMENT,user_id INTEGER,skill TEXT,FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)");
   s.executeUpdate("CREATE TABLE IF NOT EXISTS teams(id INTEGER PRIMARY KEY AUTOINCREMENT,creator_id INTEGER,name TEXT,FOREIGN KEY(creator_id) REFERENCES users(id) ON DELETE CASCADE)");
   try { s.executeUpdate("ALTER TABLE teams ADD COLUMN project_id INTEGER REFERENCES projects(id) ON DELETE SET NULL"); } catch(SQLException ignored) { /* project_id already exists */ }
   s.executeUpdate("CREATE TABLE IF NOT EXISTS team_members(team_id INTEGER,user_id INTEGER,PRIMARY KEY(team_id,user_id),FOREIGN KEY(team_id) REFERENCES teams(id) ON DELETE CASCADE,FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)");
   s.executeUpdate("CREATE TABLE IF NOT EXISTS contact_requests(id INTEGER PRIMARY KEY AUTOINCREMENT,sender_id INTEGER NOT NULL,receiver_id INTEGER NOT NULL,project_id INTEGER,message TEXT NOT NULL,status TEXT NOT NULL DEFAULT 'PENDING',created_at TEXT NOT NULL,FOREIGN KEY(sender_id) REFERENCES users(id) ON DELETE CASCADE,FOREIGN KEY(receiver_id) REFERENCES users(id) ON DELETE CASCADE,FOREIGN KEY(project_id) REFERENCES projects(id) ON DELETE SET NULL)");
   s.executeUpdate("CREATE TABLE IF NOT EXISTS contact_messages(id INTEGER PRIMARY KEY AUTOINCREMENT,request_id INTEGER NOT NULL,sender_id INTEGER NOT NULL,receiver_id INTEGER NOT NULL,message TEXT NOT NULL,created_at TEXT NOT NULL,FOREIGN KEY(request_id) REFERENCES contact_requests(id) ON DELETE CASCADE,FOREIGN KEY(sender_id) REFERENCES users(id) ON DELETE CASCADE,FOREIGN KEY(receiver_id) REFERENCES users(id) ON DELETE CASCADE)");
  }
  demo("Student Demo","student@projectdna.com","1234","student"); demo("Creator Demo","creator@projectdna.com","1234","creator");
  try(Connection c=conn();PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM projects")){ResultSet r=p.executeQuery();if(r.next()&&r.getInt(1)==0){PreparedStatement x=c.prepareStatement("INSERT INTO projects(creator_id,title,description,required_skills) VALUES((SELECT id FROM users WHERE email=?),?,?,?)");x.setString(1,"creator@projectdna.com");x.setString(2,"AI Attendance System");x.setString(3,"Develop an attendance system using AI.");x.setString(4,"Python, AI, HTML");x.executeUpdate();x.setString(2,"Smart Campus Dashboard");x.setString(3,"Build a dashboard for campus data and analytics.");x.setString(4,"JavaScript, SQL, HTML, CSS");x.executeUpdate();}}
 }
 static void demo(String n,String e,String pw,String type)throws Exception{try(Connection c=conn();PreparedStatement p=c.prepareStatement("SELECT id FROM users WHERE email=?")){p.setString(1,e);if(p.executeQuery().next())return;PreparedStatement x=c.prepareStatement("INSERT INTO users(name,email,password,type) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS);x.setString(1,n);x.setString(2,e);x.setString(3,pw);x.setString(4,type);x.executeUpdate();int id;try(ResultSet r=x.getGeneratedKeys()){r.next();id=r.getInt(1);}if(type.equals("student")){PreparedStatement s=c.prepareStatement("INSERT INTO students(user_id,interests,skills) VALUES(?,?,?)");s.setInt(1,id);s.setString(2,"AI, Data Science, Web Development");s.setString(3,"Python, Java, SQL, HTML, CSS");s.executeUpdate();}else{PreparedStatement s=c.prepareStatement("INSERT INTO creators(user_id,organization,expertise) VALUES(?,?,?)");s.setInt(1,id);s.setString(2,"Sathyabama CSE");s.setString(3,"AI and Software Development");s.executeUpdate();}}}
}
