package projectdna;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;

/** ProjectDNA - simple Java HTTP server + SQLite JDBC backend. */
public class Main {
    static final int PORT = 8080;
    static final String WEB = "web";
    static final String DB_URL = "jdbc:sqlite:data/projectdna.db";

    public static void main(String[] args) throws Exception {
        System.out.println("================================");
        System.out.println("       PROJECTDNA");
        System.out.println("================================");
        Database.init();
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api", Main::api);
        server.createContext("/", Main::staticFile);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Server running at: http://localhost:" + PORT);
        System.out.println("Database: SQLite");
        System.out.println("Status: Application started successfully");
    }

    static void api(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            Map<String,String> q = query(ex.getRequestURI().getRawQuery());
            String body = readBody(ex);
            Map<String,String> data = parseJson(body);
            Object result;
            switch (method + " " + path) {
                case "POST /api/login" -> result = login(data);
                case "POST /api/register" -> result = register(data);
                case "GET /api/dashboard" -> result = dashboard(q);
                case "GET /api/projects" -> result = projects(q);
                case "POST /api/projects" -> result = createProject(data);
                case "POST /api/projects/status" -> result = updateProjectStatus(data);
                case "GET /api/recommendations" -> result = recommendations(q);
                case "GET /api/profile" -> result = profile(q);
                case "POST /api/profile" -> result = updateProfile(data);
                case "GET /api/team" -> result = team(q);
                case "POST /api/team" -> result = saveTeam(data);
                case "GET /api/creator/students" -> result = creatorStudents(q);
                case "GET /api/creator/projects" -> result = creatorProjects(q);
                case "POST /api/logout" -> result = ok("Logged out");
                case "POST /api/contact/send" -> result = sendContact(data);
                case "GET /api/contacts" -> result = contacts(q);
                case "POST /api/contact/respond" -> result = respondContact(data);
                case "GET /api/messages" -> result = messages(q);
                case "POST /api/messages/send" -> result = sendMessage(data);
                default -> { sendJson(ex, 404, error("Endpoint not found")); return; }
            }
            sendJson(ex, 200, result);
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(ex, 500, error("Database connection failed. Please check the application configuration."));
        }
    }

    static Map<String,Object> login(Map<String,String> d) throws Exception {
        String email=d.getOrDefault("email","").trim(), password=d.getOrDefault("password","");
        String type=d.getOrDefault("accountType","student").toLowerCase();
        try(Connection c=Database.conn(); PreparedStatement p=c.prepareStatement("SELECT id,name,email,type FROM users WHERE lower(email)=lower(?) AND password=? AND type=?")) {
            p.setString(1,email); p.setString(2,password); p.setString(3,type);
            try(ResultSet r=p.executeQuery()) { if(r.next()) { Map<String,Object> m=ok("Login successful"); m.put("user", user(r)); return m; } }
        }
        return error("Invalid email, password or account type.");
    }
    static Map<String,Object> register(Map<String,String> d) throws Exception {
        String name=d.getOrDefault("fullName","").trim(), email=d.getOrDefault("email","").trim(), pw=d.getOrDefault("password","");
        String type=d.getOrDefault("accountType","student").toLowerCase();
        if(name.isEmpty()||email.isEmpty()||pw.isEmpty()) return error("Please fill all required fields.");
        try(Connection c=Database.conn()) {
            try(PreparedStatement x=c.prepareStatement("SELECT id FROM users WHERE lower(email)=lower(?)")){x.setString(1,email); if(x.executeQuery().next()) return error("Email is already registered.");}
            PreparedStatement p=c.prepareStatement("INSERT INTO users(name,email,password,type) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS);
            p.setString(1,name);p.setString(2,email);p.setString(3,pw);p.setString(4,type);p.executeUpdate();
            int id; try(ResultSet k=p.getGeneratedKeys()){k.next();id=k.getInt(1);}
            if(type.equals("student")) {
                PreparedStatement s=c.prepareStatement("INSERT INTO students(user_id,interests,skills) VALUES(?,?,?)");
                s.setInt(1,id);s.setString(2,d.getOrDefault("interests",""));s.setString(3,d.getOrDefault("skills",""));s.executeUpdate();
            } else {
                PreparedStatement s=c.prepareStatement("INSERT INTO creators(user_id,organization,expertise) VALUES(?,?,?)");
                s.setInt(1,id);s.setString(2,d.getOrDefault("organization",""));s.setString(3,d.getOrDefault("expertise",""));s.executeUpdate();
            }
        }
        return ok("Registration successful. You can now log in.");
    }
    static Map<String,Object> dashboard(Map<String,String> q) throws Exception {
        int id=intv(q.get("userId")); String type=q.getOrDefault("type","student"); Map<String,Object> m=ok("Dashboard loaded");
        try(Connection c=Database.conn()) {
            m.put("totalStudents", scalar(c,"SELECT COUNT(*) FROM users WHERE type='student'"));
            m.put("availableProjects", scalar(c,"SELECT COUNT(*) FROM projects WHERE status='OPEN'"));
            if(type.equals("creator")) { m.put("myProjects",scalar(c,"SELECT COUNT(*) FROM projects WHERE creator_id="+id)); m.put("matchingStudents",scalar(c,"SELECT COUNT(*) FROM users WHERE type='student'")); m.put("bestMatch", creatorBestMatch(c,id)); }
            else { m.put("bestMatch", bestMatch(c,id)); }
            try(PreparedStatement p=c.prepareStatement("SELECT name FROM users WHERE id=?")){p.setInt(1,id);ResultSet r=p.executeQuery();if(r.next())m.put("name",r.getString(1));}
        } return m;
    }
    static List<Map<String,Object>> projects(Map<String,String> q) throws Exception {
        int user=intv(q.get("userId"));
        if(user>0){
            try(Connection c=Database.conn(); PreparedStatement p=c.prepareStatement("SELECT type FROM users WHERE id=?")){
                p.setInt(1,user);
                try(ResultSet r=p.executeQuery()){
                    if(r.next() && "creator".equalsIgnoreCase(r.getString(1))) return projectList(null,user);
                }
            }
        }
        return projectList(q.get("userId"));
    }
    static Map<String,Object> createProject(Map<String,String> d) throws Exception {
        int creator=intv(d.get("creatorId")); String title=d.getOrDefault("title","").trim(), desc=d.getOrDefault("description","").trim(), skills=d.getOrDefault("requiredSkills","").trim();
        if(creator<=0||title.isEmpty()||skills.isEmpty()) return error("Project title and required skills are required.");
        try(Connection c=Database.conn(); PreparedStatement p=c.prepareStatement("INSERT INTO projects(creator_id,title,description,required_skills,status) VALUES(?,?,?,?,'OPEN')")){p.setInt(1,creator);p.setString(2,title);p.setString(3,desc);p.setString(4,skills);p.executeUpdate();}
        return ok("Project created successfully.");
    }
    static Map<String,Object> updateProjectStatus(Map<String,String> d) throws Exception {
        int creator=intv(d.get("creatorId")), project=intv(d.get("projectId"));
        String status=d.getOrDefault("status","").trim().toUpperCase();
        if(creator<=0||project<=0||(!status.equals("OPEN")&&!status.equals("CLOSED"))) return error("Invalid project status request.");
        try(Connection c=Database.conn(); PreparedStatement p=c.prepareStatement("UPDATE projects SET status=? WHERE id=? AND creator_id=?")){
            p.setString(1,status); p.setInt(2,project); p.setInt(3,creator);
            if(p.executeUpdate()==0) return error("Project not found or you do not own this project.");
        }
        return ok(status.equals("OPEN")?"Project reopened successfully.":"Project closed successfully.");
    }
    static List<Map<String,Object>> recommendations(Map<String,String> q) throws Exception {
        // PriorityQueue keeps the highest skill-match projects at the front.
        PriorityQueue<Map<String,Object>> queue = new PriorityQueue<>((a,b) ->
            Integer.compare(((Number)b.get("match")).intValue(), ((Number)a.get("match")).intValue()));
        queue.addAll(projectList(q.get("userId")));
        List<Map<String,Object>> list = new ArrayList<>();
        while (!queue.isEmpty()) list.add(queue.poll());
        return list;
    }
    static Map<String,Object> profile(Map<String,String> q) throws Exception { return profileData(intv(q.get("userId"))); }
    static Map<String,Object> updateProfile(Map<String,String> d) throws Exception {
        int id=intv(d.get("userId")); String type=d.getOrDefault("type","student");
        try(Connection c=Database.conn()) {
            try(PreparedStatement p=c.prepareStatement("UPDATE users SET name=?, email=? WHERE id=?")){p.setString(1,d.getOrDefault("name",""));p.setString(2,d.getOrDefault("email",""));p.setInt(3,id);p.executeUpdate();}
            if(type.equals("student")) {try(PreparedStatement p=c.prepareStatement("UPDATE students SET interests=?,skills=? WHERE user_id=?")){p.setString(1,d.getOrDefault("interests",""));p.setString(2,d.getOrDefault("skills",""));p.setInt(3,id);p.executeUpdate();}}
            else {try(PreparedStatement p=c.prepareStatement("UPDATE creators SET organization=?,expertise=? WHERE user_id=?")){p.setString(1,d.getOrDefault("organization",""));p.setString(2,d.getOrDefault("expertise",""));p.setInt(3,id);p.executeUpdate();}}
        } return ok("Profile updated successfully.");
    }
    static List<Map<String,Object>> team(Map<String,String> q) throws Exception {
        int uid=intv(q.get("userId")); int projectId=intv(q.get("projectId")); List<Map<String,Object>> out=new ArrayList<>();
        if(uid<=0||projectId<=0)return out;
        String required=""; String title="";
        try(Connection c=Database.conn(); PreparedStatement p=c.prepareStatement("SELECT title,required_skills FROM projects WHERE id=? AND creator_id=?")){p.setInt(1,projectId);p.setInt(2,uid);ResultSet r=p.executeQuery();if(!r.next())return out;title=r.getString("title");required=r.getString("required_skills");}
        try(Connection c=Database.conn()) {
            Set<Integer> savedMembers=new HashSet<>();
            // Load the most recently saved team for this creator/project so the UI remembers selected students.
            try(PreparedStatement t=c.prepareStatement("SELECT id FROM teams WHERE creator_id=? AND project_id=? ORDER BY id DESC LIMIT 1")) {
                t.setInt(1,uid); t.setInt(2,projectId);
                try(ResultSet tr=t.executeQuery()) {
                    if(tr.next()) {
                        int teamId=tr.getInt(1);
                        try(PreparedStatement m=c.prepareStatement("SELECT user_id FROM team_members WHERE team_id=?")) {
                            m.setInt(1,teamId);
                            try(ResultSet mr=m.executeQuery()) { while(mr.next()) savedMembers.add(mr.getInt(1)); }
                        }
                    }
                }
            }
            try(PreparedStatement p=c.prepareStatement("SELECT u.id,u.name,u.email,s.skills,s.interests FROM users u JOIN students s ON s.user_id=u.id WHERE u.type='student'")){
                ResultSet r=p.executeQuery();
                while(r.next()){
                    Map<String,Object> x=new LinkedHashMap<>();
                    int studentId=r.getInt("id");
                    x.put("id",studentId); x.put("name",r.getString("name")); x.put("email",r.getString("email"));
                    x.put("skills",r.getString("skills")); x.put("interests",r.getString("interests"));
                    x.put("match",match(required,r.getString("skills"))); x.put("projectId",projectId); x.put("projectTitle",title);
                    x.put("selected",savedMembers.contains(studentId)); out.add(x);
                }
            }
        }
        out.sort((a,b)->Integer.compare(((Number)b.get("match")).intValue(),((Number)a.get("match")).intValue())); return out;
    }
    // Send a simple project/contact request from one user to another.
    static Map<String,Object> sendContact(Map<String,String> d) throws Exception {
        int sender=intv(d.get("senderId")), receiver=intv(d.get("receiverId")), project=intv(d.get("projectId"));
        String message=d.getOrDefault("message","").trim();
        if(sender<=0||receiver<=0||sender==receiver) return error("Please select a valid contact.");
        if(message.isEmpty()) return error("Please enter a message.");
        try(Connection c=Database.conn()) {
            if(project>0) {
                try(PreparedStatement p=c.prepareStatement("SELECT id,status FROM projects WHERE id=?")){p.setInt(1,project);try(ResultSet r=p.executeQuery()){if(!r.next()) return error("Project not found."); if(!"OPEN".equalsIgnoreCase(r.getString("status"))) return error("This project is closed and is no longer accepting requests.");}}
            }
            try(PreparedStatement p=c.prepareStatement("SELECT id FROM contact_requests WHERE sender_id=? AND receiver_id=? AND IFNULL(project_id,0)=? AND status='PENDING'")) {
                p.setInt(1,sender);p.setInt(2,receiver);p.setInt(3,project);if(p.executeQuery().next())return error("A request is already pending.");
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO contact_requests(sender_id,receiver_id,project_id,message,status,created_at) VALUES(?,?,?,?,?,datetime('now'))")) {
                p.setInt(1,sender);p.setInt(2,receiver);if(project>0)p.setInt(3,project);else p.setNull(3,Types.INTEGER);p.setString(4,message);p.setString(5,"PENDING");p.executeUpdate();
            }
        }
        return ok("Request sent successfully.");
    }

    static List<Map<String,Object>> contacts(Map<String,String> q) throws Exception {
        int uid=intv(q.get("userId")); List<Map<String,Object>> out=new ArrayList<>();
        String sql="SELECT cr.id,cr.sender_id,cr.receiver_id,cr.project_id,cr.message,cr.status,cr.created_at, " +
                   "s.name sender_name,s.email sender_email,r.name receiver_name,r.email receiver_email,p.title project_title " +
                   "FROM contact_requests cr JOIN users s ON s.id=cr.sender_id JOIN users r ON r.id=cr.receiver_id " +
                   "LEFT JOIN projects p ON p.id=cr.project_id WHERE cr.sender_id=? OR cr.receiver_id=? ORDER BY cr.id DESC";
        try(Connection c=Database.conn();PreparedStatement p=c.prepareStatement(sql)){p.setInt(1,uid);p.setInt(2,uid);ResultSet r=p.executeQuery();while(r.next()){
            Map<String,Object>x=new LinkedHashMap<>();x.put("id",r.getInt("id"));x.put("senderId",r.getInt("sender_id"));x.put("receiverId",r.getInt("receiver_id"));x.put("projectId",r.getObject("project_id"));
            x.put("message",r.getString("message"));x.put("status",r.getString("status"));x.put("createdAt",r.getString("created_at"));x.put("senderName",r.getString("sender_name"));x.put("senderEmail",r.getString("sender_email"));x.put("receiverName",r.getString("receiver_name"));x.put("receiverEmail",r.getString("receiver_email"));x.put("projectTitle",r.getString("project_title"));x.put("incoming",r.getInt("receiver_id")==uid);out.add(x);
        }} return out;
    }

    static Map<String,Object> respondContact(Map<String,String> d) throws Exception {
        int id=intv(d.get("requestId")), uid=intv(d.get("userId")); String status=d.getOrDefault("status","").toUpperCase();
        if(id<=0||(!status.equals("ACCEPTED")&&!status.equals("REJECTED")))return error("Invalid request response.");
        try(Connection c=Database.conn();PreparedStatement p=c.prepareStatement("UPDATE contact_requests SET status=? WHERE id=? AND receiver_id=? AND status='PENDING'")){p.setString(1,status);p.setInt(2,id);p.setInt(3,uid);if(p.executeUpdate()==0)return error("Request is no longer pending or does not belong to you.");}
        return ok(status.equals("ACCEPTED")?"Request accepted. You can now coordinate with this person.":"Request rejected.");
    }

    static List<Map<String,Object>> messages(Map<String,String> q) throws Exception {
        int requestId=intv(q.get("requestId")), uid=intv(q.get("userId"));
        List<Map<String,Object>> out=new ArrayList<>();
        if(requestId<=0||uid<=0)return out;
        try(Connection c=Database.conn()) {
            int senderId=0,receiverId=0;
            String original="",created="";
            try(PreparedStatement p=c.prepareStatement("SELECT sender_id,receiver_id,message,created_at,status FROM contact_requests WHERE id=? AND (sender_id=? OR receiver_id=?)")) {
                p.setInt(1,requestId);p.setInt(2,uid);p.setInt(3,uid);
                try(ResultSet r=p.executeQuery()) {
                    if(!r.next())return out;
                    if(!"ACCEPTED".equalsIgnoreCase(r.getString("status")))return out;
                    senderId=r.getInt("sender_id"); receiverId=r.getInt("receiver_id"); original=r.getString("message"); created=r.getString("created_at");
                }
            }
            Map<String,Object> first=new LinkedHashMap<>();
            first.put("id",0);first.put("requestId",requestId);first.put("senderId",senderId);first.put("receiverId",receiverId);first.put("message",original);first.put("createdAt",created);first.put("original",true);first.put("mine",senderId==uid);
            out.add(first);
            try(PreparedStatement p=c.prepareStatement("SELECT id,sender_id,receiver_id,message,created_at FROM contact_messages WHERE request_id=? ORDER BY id ASC")) {
                p.setInt(1,requestId);
                try(ResultSet r=p.executeQuery()) {
                    while(r.next()) {
                        Map<String,Object> x=new LinkedHashMap<>();
                        x.put("id",r.getInt("id"));x.put("requestId",requestId);x.put("senderId",r.getInt("sender_id"));x.put("receiverId",r.getInt("receiver_id"));x.put("message",r.getString("message"));x.put("createdAt",r.getString("created_at"));x.put("original",false);x.put("mine",r.getInt("sender_id")==uid);out.add(x);
                    }
                }
            }
        }
        return out;
    }

    static Map<String,Object> sendMessage(Map<String,String> d) throws Exception {
        int requestId=intv(d.get("requestId")), sender=intv(d.get("senderId"));
        String message=d.getOrDefault("message","").trim();
        if(requestId<=0||sender<=0||message.isEmpty())return error("Please enter a message.");
        try(Connection c=Database.conn()) {
            int receiver=0;
            try(PreparedStatement p=c.prepareStatement("SELECT sender_id,receiver_id,status FROM contact_requests WHERE id=? AND (sender_id=? OR receiver_id=?)")) {
                p.setInt(1,requestId);p.setInt(2,sender);p.setInt(3,sender);
                try(ResultSet r=p.executeQuery()) {
                    if(!r.next())return error("Connection not found.");
                    if(!"ACCEPTED".equalsIgnoreCase(r.getString("status")))return error("You can reply only after the request is accepted.");
                    receiver=r.getInt("sender_id")==sender?r.getInt("receiver_id"):r.getInt("sender_id");
                }
            }
            try(PreparedStatement p=c.prepareStatement("INSERT INTO contact_messages(request_id,sender_id,receiver_id,message,created_at) VALUES(?,?,?,?,datetime('now'))")) {
                p.setInt(1,requestId);p.setInt(2,sender);p.setInt(3,receiver);p.setString(4,message);p.executeUpdate();
            }
        }
        return ok("Reply sent successfully.");
    }

    static Map<String,Object> saveTeam(Map<String,String> d) throws Exception {
        int creator=intv(d.get("userId")); int project=intv(d.get("projectId")); String members=d.getOrDefault("memberIds","");
        if(creator<=0||project<=0)return error("Please select a project first.");
        try(Connection c=Database.conn()){
            c.setAutoCommit(false);
            try {
                try(PreparedStatement check=c.prepareStatement("SELECT id FROM projects WHERE id=? AND creator_id=?")){
                    check.setInt(1,project); check.setInt(2,creator);
                    if(!check.executeQuery().next()){c.rollback(); return error("Project not found or you do not own this project.");}
                }
                int tid=0;
                // Update the latest saved team for this project instead of creating duplicates every time.
                try(PreparedStatement find=c.prepareStatement("SELECT id FROM teams WHERE creator_id=? AND project_id=? ORDER BY id DESC LIMIT 1")){
                    find.setInt(1,creator); find.setInt(2,project);
                    try(ResultSet r=find.executeQuery()){if(r.next())tid=r.getInt(1);}
                }
                if(tid==0){
                    try(PreparedStatement p=c.prepareStatement("INSERT INTO teams(creator_id,name,project_id) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)){
                        p.setInt(1,creator); p.setString(2,"ProjectDNA Team"); p.setInt(3,project); p.executeUpdate();
                        try(ResultSet r=p.getGeneratedKeys()){r.next(); tid=r.getInt(1);}
                    }
                } else {
                    try(PreparedStatement del=c.prepareStatement("DELETE FROM team_members WHERE team_id=?")){del.setInt(1,tid);del.executeUpdate();}
                }
                try(PreparedStatement x=c.prepareStatement("INSERT OR IGNORE INTO team_members(team_id,user_id) VALUES(?,?)")){
                    for(String s:members.split(",")){
                        int id=intv(s); if(id>0){x.setInt(1,tid);x.setInt(2,id);x.addBatch();}
                    }
                    x.executeBatch();
                }
                c.commit();
            } catch(Exception e){c.rollback(); throw e;} finally {c.setAutoCommit(true);}
        }
        return ok("Team saved successfully for the selected project. Selected members will remain selected.");
    }
    static List<Map<String,Object>> creatorStudents(Map<String,String> q) throws Exception {
        int creator=intv(q.get("userId")); List<Map<String,Object>> all=new ArrayList<>(); List<Map<String,Object>> ps=creatorProjects(q);
        for(Map<String,Object> pr:ps){int pid=((Number)pr.get("id")).intValue();String req=(String)pr.get("requiredSkills"); try(Connection c=Database.conn();PreparedStatement p=c.prepareStatement("SELECT u.id,u.name,u.email,s.skills FROM users u JOIN students s ON s.user_id=u.id WHERE u.type='student'")){ResultSet r=p.executeQuery();while(r.next()){Map<String,Object>x=new LinkedHashMap<>();x.put("project",pr.get("title"));x.put("projectId",pid);x.put("id",r.getInt("id"));x.put("name",r.getString("name"));x.put("email",r.getString("email"));x.put("skills",r.getString("skills"));x.put("match",match(req,r.getString("skills")));all.add(x);}}}
        all.sort((a,b)->Integer.compare(((Number)b.get("match")).intValue(),((Number)a.get("match")).intValue()));return all;
    }
    static List<Map<String,Object>> creatorProjects(Map<String,String> q) throws Exception { return projectList(null, intv(q.get("userId"))); }

    static List<Map<String,Object>> projectList(String uid) throws Exception { return projectList(uid,null); }
    static List<Map<String,Object>> projectList(String uid,Integer creatorFilter) throws Exception {
        int user=intv(uid); List<Map<String,Object>> out=new ArrayList<>();
        try(Connection c=Database.conn()){
            String userType="";
            if(user>0){
                try(PreparedStatement t=c.prepareStatement("SELECT type FROM users WHERE id=?")){t.setInt(1,user);try(ResultSet tr=t.executeQuery()){if(tr.next())userType=tr.getString(1);}}
            }
            StringBuilder sql=new StringBuilder("SELECT p.id,p.title,p.description,p.required_skills,p.status,p.creator_id,u.name creator FROM projects p JOIN users u ON u.id=p.creator_id");
            if(creatorFilter!=null) sql.append(" WHERE p.creator_id=?");
            if("student".equals(userType)) sql.append(creatorFilter!=null?" AND":" WHERE").append(" p.status='OPEN'");
            try(PreparedStatement p=c.prepareStatement(sql.toString())){
                if(creatorFilter!=null)p.setInt(1,creatorFilter);
                ResultSet r=p.executeQuery();
                String ss=user>0?studentSkills(user):"";
                while(r.next()){
                    Map<String,Object>x=new LinkedHashMap<>();
                    x.put("id",r.getInt("id")); x.put("creatorId",r.getInt("creator_id"));
                    x.put("title",r.getString("title")); x.put("description",r.getString("description"));
                    x.put("requiredSkills",r.getString("required_skills")); x.put("status",r.getString("status")); x.put("creator",r.getString("creator"));
                    int projectMatch=(creatorFilter!=null || "creator".equalsIgnoreCase(userType))?topStudentMatch(c,r.getString("required_skills")):match(r.getString("required_skills"),ss);
                    x.put("match",projectMatch); out.add(x);
                }
            }
        } return out;
    }
    static int topStudentMatch(Connection c,String required)throws Exception{
        int best=0;
        try(PreparedStatement p=c.prepareStatement("SELECT skills FROM students");ResultSet r=p.executeQuery()){
            while(r.next()) best=Math.max(best,match(required,r.getString(1)));
        }
        return best;
    }
    static Map<String,Object> profileData(int id) throws Exception { Map<String,Object> x=new LinkedHashMap<>();try(Connection c=Database.conn();PreparedStatement p=c.prepareStatement("SELECT id,name,email,type FROM users WHERE id=?")){p.setInt(1,id);ResultSet r=p.executeQuery();if(!r.next())return error("User not found.");x.put("id",id);x.put("name",r.getString("name"));x.put("email",r.getString("email"));x.put("type",r.getString("type"));if(r.getString("type").equals("student")){PreparedStatement s=c.prepareStatement("SELECT interests,skills FROM students WHERE user_id=?");s.setInt(1,id);ResultSet z=s.executeQuery();if(z.next()){x.put("interests",z.getString(1));x.put("skills",z.getString(2));}}else{PreparedStatement s=c.prepareStatement("SELECT organization,expertise FROM creators WHERE user_id=?");s.setInt(1,id);ResultSet z=s.executeQuery();if(z.next()){x.put("organization",z.getString(1));x.put("expertise",z.getString(2));}}}return x; }
    static String studentSkills(int id)throws Exception{try(Connection c=Database.conn();PreparedStatement p=c.prepareStatement("SELECT skills FROM students WHERE user_id=?")){p.setInt(1,id);ResultSet r=p.executeQuery();return r.next()?r.getString(1):"";}}
    static int creatorBestMatch(Connection c,int creatorId)throws Exception{
        int best=0;
        List<Map<String,Object>> ps=projectList(null,creatorId);
        try(Statement st=c.createStatement();ResultSet r=st.executeQuery("SELECT skills FROM students")){
            List<String> students=new ArrayList<>(); while(r.next())students.add(r.getString(1));
            for(Map<String,Object> p:ps){String req=(String)p.get("requiredSkills");for(String sk:students)best=Math.max(best,match(req,sk));}
        } return best;
    }
    static int bestMatch(Connection c,int uid)throws Exception{int best=0;String s=studentSkills(uid);try(Statement st=c.createStatement();ResultSet r=st.executeQuery("SELECT required_skills FROM projects")){while(r.next())best=Math.max(best,match(r.getString(1),s));}return best;}
    static int match(String required,String student){Set<String> a=skills(required), b=skills(student);if(a.isEmpty())return 0;int n=0;for(String x:a)if(b.contains(x))n++;return n*100/a.size();}
    static Set<String> skills(String s){Set<String> h=new HashSet<>();for(String x:(s==null?"":s).split(",|;|\\n")){x=x.trim().toLowerCase();if(!x.isEmpty())h.add(x);}return h;}
    static Object scalar(Connection c,String sql)throws Exception{try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){return r.next()?r.getInt(1):0;}}
    static Map<String,Object> user(ResultSet r)throws Exception{Map<String,Object>u=new LinkedHashMap<>();u.put("id",r.getInt("id"));u.put("name",r.getString("name"));u.put("email",r.getString("email"));u.put("type",r.getString("type"));return u;}
    static Map<String,Object> ok(String msg){Map<String,Object>m=new LinkedHashMap<>();m.put("success",true);m.put("message",msg);return m;}
    static Map<String,Object> error(String msg){Map<String,Object>m=new LinkedHashMap<>();m.put("success",false);m.put("message",msg);return m;}
    static int intv(String s){try{return Integer.parseInt(s==null?"0":s);}catch(Exception e){return 0;}}
    static String readBody(HttpExchange e)throws IOException{return new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);}
    static Map<String,String> query(String s){Map<String,String>m=new HashMap<>();if(s==null)return m;for(String p:s.split("&")){String[]a=p.split("=",2);if(a.length==2)m.put(URLDecoder.decode(a[0],StandardCharsets.UTF_8),URLDecoder.decode(a[1],StandardCharsets.UTF_8));}return m;}
    // Small JSON parser for simple form-like JSON sent by our frontend.
    static Map<String,String> parseJson(String s){Map<String,String>m=new HashMap<>();if(s==null)return m;java.util.regex.Matcher z=java.util.regex.Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*(?:\\\"((?:\\\\.|[^\\\"])*)\\\"|([^,}]+))").matcher(s);while(z.find()){String v=z.group(2)!=null?z.group(2):z.group(3);v=v.trim().replace("\\\"","\"").replace("\\\\","\\");m.put(z.group(1),v);}return m;}
    static void sendJson(HttpExchange e,int status,Object obj)throws IOException{String s=Json.stringify(obj);byte[]b=s.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json; charset=UTF-8");e.getResponseHeaders().set("Access-Control-Allow-Origin","*");e.sendResponseHeaders(status,b.length);try(OutputStream o=e.getResponseBody()){o.write(b);}}
    static void staticFile(HttpExchange e)throws IOException{String p=e.getRequestURI().getPath();if(p.equals("/"))p="/index.html";Path f=Paths.get(WEB+p).normalize();if(!f.startsWith(Paths.get(WEB))||!Files.exists(f)||Files.isDirectory(f)){byte[]b="Not found".getBytes();e.sendResponseHeaders(404,b.length);e.getResponseBody().write(b);e.close();return;}byte[]b=Files.readAllBytes(f);e.getResponseHeaders().set("Content-Type",mime(f.toString()));e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();}
    static String mime(String f){if(f.endsWith(".html"))return"text/html; charset=UTF-8";if(f.endsWith(".css"))return"text/css; charset=UTF-8";if(f.endsWith(".js"))return"application/javascript; charset=UTF-8";return"application/octet-stream";}
}

class Json { static String stringify(Object o){if(o==null)return"null";if(o instanceof String)return"\""+esc((String)o)+"\"";if(o instanceof Number||o instanceof Boolean)return o.toString();if(o instanceof Map){StringBuilder s=new StringBuilder("{");boolean f=true;for(Object k:((Map<?,?>)o).keySet()){if(!f)s.append(',');f=false;s.append(stringify(k.toString())).append(':').append(stringify(((Map<?,?>)o).get(k)));}return s.append('}').toString();}if(o instanceof Iterable){StringBuilder s=new StringBuilder("[");boolean f=true;for(Object x:(Iterable<?>)o){if(!f)s.append(',');f=false;s.append(stringify(x));}return s.append(']').toString();}return stringify(o.toString());}static String esc(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}}
