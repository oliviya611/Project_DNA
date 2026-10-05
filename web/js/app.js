const API='/api';
let state=JSON.parse(localStorage.getItem('pdUser')||'null');
let selected=[];
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
async function api(path,opts={}){
  try{
    const r=await fetch(API+path,{headers:{'Content-Type':'application/json'},...opts});
    return await r.json();
  }catch(e){return {success:false,message:'Could not connect to the server. Make sure ProjectDNA is running.'};}
}
function save(u){state=u;localStorage.setItem('pdUser',JSON.stringify(u));}
function need(){if(!state){location.href='index.html';return false}return true;}
function logout(){localStorage.removeItem('pdUser');location.href='index.html';}
function nav(active,type){
  const creator=type==='creator';
  return `<div class="layout"><aside class="side">
    <h2>◈ ProjectDNA</h2><p class="muted">${creator?'Creator':'Student'} Portal</p>
    <a class="${active==='dash'?'active':''}" href="${creator?'creator.html':'dashboard.html'}">⌂ Dashboard</a>
    <a class="${active==='projects'?'active':''}" href="projects.html">▣ Projects</a>
    ${!creator?`<a class="${active==='rec'?'active':''}" href="recommendations.html">★ Recommendations</a>`:`<a class="${active==='students'?'active':''}" href="team.html">♙ Team Formation</a>`}
    <a class="${active==='contacts'?'active':''}" href="contacts.html">✉ Contact & Requests <span id="requestBadge" class="badge"></span></a>
    <a class="${active==='profile'?'active':''}" href="profile.html">◎ My Profile</a>
    <a href="#" onclick="logout();return false">↪ Logout</a>
  </aside><main class="main"><div class="top"><div><h1 id="title">ProjectDNA</h1><p class="muted">Build projects. Find skills. Form better teams.</p></div><div class="user-chip">${esc(state?.name||'User')} <span>${creator?'Creator':'Student'}</span></div></div><div id="content"></div></main></div>`;
}
async function loadBadge(){
  if(!state)return;
  const list=await api(`/contacts?userId=${state.id}`);
  const n=Array.isArray(list)?list.filter(x=>x.incoming&&x.status==='PENDING').length:0;
  const b=document.getElementById('requestBadge'); if(b){b.textContent=n?n:'';b.style.display=n?'inline-flex':'none';}
}
function showModal(html){
  let m=document.getElementById('modal'); if(!m){m=document.createElement('div');m.id='modal';m.className='modal';document.body.appendChild(m);}
  m.innerHTML=`<div class="modal-card">${html}<button class="modal-close" onclick="closeModal()">×</button></div>`;m.classList.add('show');
}
function closeModal(){const m=document.getElementById('modal');if(m)m.classList.remove('show');}
function contactUser(receiverId,receiverName,projectId=0,projectTitle=''){
  showModal(`<h2>Contact ${esc(receiverName)}</h2>${projectTitle?`<p class="muted">Regarding: <b>${esc(projectTitle)}</b></p>`:''}
    <label>Message</label><textarea id="contactMessage" rows="5" placeholder="Hi, I am interested in working with you on this project..."></textarea>
    <button class="primary" onclick="sendContact(${receiverId},${projectId})">Send Request</button><div id="contactMsg" class="msg"></div>`);
}
async function sendContact(receiverId,projectId){
  const msg=document.getElementById('contactMessage').value.trim(), box=document.getElementById('contactMsg');
  if(!msg){box.textContent='Please enter a message.';box.classList.add('show');return;}
  const r=await api('/contact/send',{method:'POST',body:JSON.stringify({senderId:state.id,receiverId,projectId,message:msg})});
  box.textContent=r.message;box.classList.add('show');
  if(r.success)setTimeout(closeModal,700);
}
async function dashboard(creator=false){
  if(!need())return;document.getElementById('app').innerHTML=nav('dash',state.type);
  const d=await api(`/dashboard?userId=${state.id}&type=${state.type}`);document.getElementById('title').textContent=creator?'Creator Dashboard':'Student Dashboard';
  document.getElementById('content').innerHTML=creator?`
    <div class="hero card"><div><span class="eyebrow">PROJECT CREATOR</span><h2>Welcome, ${esc(d.name||state.name)} 👋</h2><p class="muted">Create projects, discover students and build your team.</p></div><a class="primary-link" href="projects.html">＋ Create Project</a></div>
    <div class="grid"><div class="card stat"><span>My Projects</span><b>${d.myProjects||0}</b></div><div class="card stat"><span>Students Available</span><b>${d.matchingStudents||0}</b></div><div class="card stat"><span>Highest Match</span><b>${d.bestMatch||0}%</b></div></div>
    <div class="actions"><a class="action" href="projects.html"><span>＋</span>Create / Manage Projects</a><a class="action" href="team.html"><span>♙</span>Find Students</a><a class="action" href="contacts.html"><span>✉</span>Contact & Requests</a><a class="action" href="profile.html"><span>◎</span>My Profile</a></div>`:`
    <div class="hero card"><div><span class="eyebrow">STUDENT PORTAL</span><h2>Welcome, ${esc(d.name||state.name)} 👋</h2><p class="muted">Find projects that match your skills and connect with project creators.</p></div><a class="primary-link" href="recommendations.html">★ View Recommendations</a></div>
    <div class="grid"><div class="card stat"><span>Total Students</span><b>${d.totalStudents||0}</b></div><div class="card stat"><span>Available Projects</span><b>${d.availableProjects||0}</b></div><div class="card stat"><span>Best Match</span><b>${d.bestMatch||0}%</b></div></div>
    <div class="actions"><a class="action" href="projects.html"><span>▣</span>Browse Projects</a><a class="action" href="recommendations.html"><span>★</span>Recommendations</a><a class="action" href="contacts.html"><span>✉</span>Contact & Requests</a><a class="action" href="profile.html"><span>◎</span>My Profile</a></div>`;
  await loadBadge();
}
async function projects(){
  if(!need())return;
  document.getElementById('app').innerHTML=nav('projects',state.type);
  let list=await api(`/projects?userId=${state.id}`);
  const create=state.type==='creator'?`<div class="card"><div class="section-head"><div><span class="eyebrow">CREATOR TOOLS</span><h2>Create a New Project</h2></div></div><form id="projectForm" class="form-grid"><div><label>Project Title</label><input id="ptitle" required placeholder="AI Attendance System"></div><div><label>Required Skills</label><input id="projectSkills" required placeholder="Python, AI, HTML"></div><div class="full"><label>Description</label><textarea id="desc" rows="3" placeholder="Describe what students will build..."></textarea></div><button class="primary full">Publish Project</button></form></div>`:'';
  const matchLabel=state.type==='creator'?'Top Student Match':'Skill Match';
  const arr=Array.isArray(list)?list:[];
  document.getElementById('content').innerHTML=create+`<div class="section-head"><div><h2>${state.type==='creator'?'My Projects':'Available Projects'}</h2><p class="muted">${state.type==='creator'?'Manage your projects, control recruitment status, and view the highest student match.':'Explore open projects and contact creators when you find a good match.'}</p></div></div><div class="tablelike">${arr.length?arr.map(p=>{
    const open=String(p.status||'OPEN').toUpperCase()==='OPEN';
    const status=open?'OPEN':'CLOSED';
    const statusClass=open?'open':'closed';
    const creatorControls=state.type==='creator'?`<button class="smallbtn status-action ${open?'close-action':'reopen-action'}" onclick="toggleProjectStatus(${p.id},'${status}')">${open?'🔒 Close Project':'🔓 Reopen Project'}</button>`:'';
    const studentContact=state.type==='student'&&open?`<button class="smallbtn contact-btn" onclick="contactUser(${p.creatorId||0},'${esc(p.creator)}',${p.id},'${esc(p.title)}')">✉ Contact Creator</button>`:'';
    return `<div class="card project ${!open?'project-closed':''}><div class="project-main"><div class="project-title-row"><h3>${esc(p.title)}</h3><span class="status-pill ${statusClass}">${open?'Open':'Closed'}</span></div><p class="muted">${esc(p.description)}</p><p><b>Creator:</b> ${esc(p.creator)}</p><div>${esc(p.requiredSkills).split(',').map(x=>`<span class="tag">${esc(x.trim())}</span>`).join('')}</div>${studentContact}${creatorControls}</div><div class="match"><strong>${p.match}%</strong><div class="muted">${matchLabel}</div><div class="progress"><i style="width:${p.match}%"></i></div>${p.match>=70&&open?'<span class="good">Strong match</span>':''}</div></div>`;
  }).join(''):'<div class="card empty">No projects available.</div>'}</div>`;
  const f=document.getElementById('projectForm');
  if(f)f.onsubmit=async e=>{e.preventDefault();const r=await api('/projects',{method:'POST',body:JSON.stringify({creatorId:state.id,title:document.getElementById('ptitle').value.trim(),description:document.getElementById('desc').value.trim(),requiredSkills:document.getElementById('projectSkills').value.trim()})});showToast(r.message);if(r.success)projects();};
  await loadBadge();
}
async function toggleProjectStatus(projectId,currentStatus){
  const next=String(currentStatus).toUpperCase()==='OPEN'?'CLOSED':'OPEN';
  const action=next==='OPEN'?'reopen':'close';
  if(!confirm(`Are you sure you want to ${action} this project?`))return;
  const r=await api('/projects/status',{method:'POST',body:JSON.stringify({projectId,creatorId:state.id,status:next})});
  showToast(r.message);
  if(r.success)projects();
}
async function rec(){
  if(!need())return;document.getElementById('app').innerHTML=nav('rec',state.type);let list=await api(`/recommendations?userId=${state.id}`);
  document.getElementById('content').innerHTML='<div class="section-head"><div><span class="eyebrow">SMART RECOMMENDATIONS</span><h2>Projects For You</h2><p class="muted">Highest skill matches appear first.</p></div></div><div class="tablelike">'+(Array.isArray(list)?list:[]).map(p=>`<div class="card project"><div class="project-main"><h3>${esc(p.title)}</h3><p>${esc(p.description)}</p><p><b>Matching skills:</b> ${esc(p.matchingSkills||p.requiredSkills)}</p><div>${esc(p.requiredSkills).split(',').map(x=>`<span class="tag">${esc(x.trim())}</span>`).join('')}</div><button class="smallbtn" onclick="contactUser(${p.creatorId||0},'${esc(p.creator)}',${p.id},'${esc(p.title)}')">✉ Contact Creator</button></div><div class="match"><strong>${p.match}%</strong><div class="progress"><i style="width:${p.match}%"></i></div></div></div>`).join('')+'</div>';
  await loadBadge();
}
async function profile(){
  if(!need())return;document.getElementById('app').innerHTML=nav('profile',state.type);let p=await api(`/profile?userId=${state.id}`);let extra=state.type==='student'?`<label>Interests</label><input id="interests" value="${esc(p.interests)}"><label>Skills</label><input id="profileSkills" value="${esc(p.skills)}"><p class="hint">Separate skills with commas, e.g. Python, Java, SQL.</p>`:`<label>Organization / Department</label><input id="organization" value="${esc(p.organization)}"><label>Area of Expertise</label><input id="expertise" value="${esc(p.expertise)}">`;
  document.getElementById('content').innerHTML=`<div class="grid2"><div class="card"><span class="eyebrow">YOUR ACCOUNT</span><h2>My Profile</h2><form id="profileForm"><label>Name</label><input id="profileName" value="${esc(p.name)}" required><label>Email</label><input id="profileEmail" type="email" value="${esc(p.email)}" required>${extra}<button class="primary">Save Changes</button></form></div><div class="card info-card"><h3>How your profile is used</h3><p>Your skills are compared with project requirements to calculate a simple match percentage.</p><p>Creators can discover students, and students can contact creators directly from project pages.</p><div class="formula">Matching skills ÷ Required skills × 100</div></div></div>`;
  document.getElementById('profileForm').onsubmit=async e=>{e.preventDefault();let d={userId:state.id,type:state.type,name:document.getElementById('profileName').value.trim(),email:document.getElementById('profileEmail').value.trim()};if(state.type==='student'){d.interests=document.getElementById('interests').value.trim();d.skills=document.getElementById('profileSkills').value.trim()}else{d.organization=document.getElementById('organization').value.trim();d.expertise=document.getElementById('expertise').value.trim()}let r=await api('/profile',{method:'POST',body:JSON.stringify(d)});showToast(r.message);if(r.success){state.name=d.name;state.email=d.email;save(state)}};
  await loadBadge();
}
async function team(){
  if(!need())return;
  if(state.type!=='creator'){location.href='dashboard.html';return;}
  document.getElementById('app').innerHTML=nav('students',state.type);
  const projectsList=await api(`/creator/projects?userId=${state.id}`);
  const projects=Array.isArray(projectsList)?projectsList:[];
  if(!projects.length){
    document.getElementById('content').innerHTML=`<div class="section-head"><div><span class="eyebrow">TEAM FORMATION</span><h2>Find Students & Form Teams</h2><p class="muted">Create a project first, then select it here to find the best matching students.</p></div></div><div class="card empty">No projects found. Create a project from the Projects page first.</div>`;
    await loadBadge(); return;
  }
  const requestedId=new URLSearchParams(location.search).get('projectId');
  let projectId=Number(requestedId)||Number(projects[0].id);
  if(!projects.some(p=>Number(p.id)===projectId))projectId=Number(projects[0].id);
  selected=[];
  await renderTeam(projects,projectId);
}
async function renderTeam(projects,projectId){
  const project=projects.find(p=>Number(p.id)===Number(projectId));
  const list=await api(`/team?userId=${state.id}&projectId=${projectId}`);
  const teamStudents=Array.isArray(list)?list:[];
  selected=teamStudents.filter(s=>s.selected===true||s.selected===1||s.selected==='true').map(s=>Number(s.id));
  const options=projects.map(p=>`<option value="${p.id}" ${Number(p.id)===Number(projectId)?'selected':''}>${esc(p.title)}${String(p.status||'OPEN').toUpperCase()==='CLOSED'?' (Closed)':''}</option>`).join('');
  document.getElementById('content').innerHTML=`<div class="section-head"><div><span class="eyebrow">TEAM FORMATION</span><h2>Find Students & Form Teams</h2><p class="muted">Select one of your projects. Students are ranked by how well their skills match that project's required skills.</p></div></div>
  <div class="card"><label><b>Select Project</b></label><select id="teamProject" class="project-select">${options}</select><p class="hint"><b>${esc(project?.title||'Selected Project')}</b> — Required skills: ${esc(project?.requiredSkills||'')}</p></div>
  <div class="card info-card"><h3>How team formation works</h3><p>1. Select your project. 2. Review student skill matches. 3. Contact suitable students if needed. 4. Select members. 5. Save the team for this project.</p></div>
  <div class="tablelike">${teamStudents.map(s=>`<div class="card student ${selected.includes(Number(s.id))?'selected':''}"><div><h3>${esc(s.name)}</h3><p class="muted">${esc(s.email)}</p><div>${esc(s.skills).split(',').map(x=>`<span class="tag">${esc(x.trim())}</span>`).join('')}</div><small>${esc(s.interests||'')}</small></div><div class="match"><strong>${s.match}%</strong><div class="muted">Project Skill Match</div><div class="progress"><i style="width:${s.match}%"></i></div>${s.match>=70?'<span class="good">Strong match</span>':''}<div class="button-row"><button class="smallbtn" onclick="contactUser(${s.id},'${esc(s.name)}',${projectId},'${esc(project?.title||'')}')">✉ Contact</button><button class="smallbtn" onclick="selectMember(${s.id},this)">${selected.includes(Number(s.id))?'Selected':'Add to Team'}</button></div></div></div>`).join('')||'<div class="card empty">No students are registered yet.</div>'}</div>
  <div class="card"><h3>Selected Team Members</h3><div id="selected">${selected.length?selected.join(', '):'None yet'}</div><button class="primary" onclick="saveSelected(${projectId})">Create / Save Team</button></div>`;
  document.getElementById('teamProject').onchange=e=>{location.href=`team.html?projectId=${e.target.value}`};
  await loadBadge();
}
function selectMember(id,b){id=Number(id);if(selected.includes(id)){selected=selected.filter(x=>x!==id);b.textContent='Add to Team'}else{selected.push(id);b.textContent='Selected'}const x=document.getElementById('selected');if(x)x.textContent=selected.length?selected.map(id=>{const s=document.querySelector(`.student button[onclick^="selectMember(${id},"]`);return s?.closest('.student')?.querySelector('h3')?.textContent||id}).join(', '):'None yet';}
async function saveSelected(projectId){if(!selected.length){showToast('Select at least one student.');return;}let r=await api('/team',{method:'POST',body:JSON.stringify({userId:state.id,projectId,memberIds:selected.join(',')})});showToast(r.message);if(r.success){selected=[];setTimeout(()=>team(),500)}}
async function contacts(){
  if(!need())return;
  document.getElementById('app').innerHTML=nav('contacts',state.type);
  const list=await api(`/contacts?userId=${state.id}`);const arr=Array.isArray(list)?list:[];
  const incoming=arr.filter(x=>x.incoming&&x.status==='PENDING'), outgoing=arr.filter(x=>!x.incoming&&x.status==='PENDING'), accepted=arr.filter(x=>x.status==='ACCEPTED');
  document.getElementById('content').innerHTML=`<div class="hero card"><div><span class="eyebrow">COMMUNICATION CENTER</span><h2>Contact & Requests</h2><p class="muted">Send project requests, accept connections and reply to your project contacts.</p></div><div class="contact-stats"><b>${incoming.length}</b><span>Pending</span></div></div>
    <div class="grid2"><div><h2>Incoming Requests</h2><div class="tablelike">${incoming.length?incoming.map(x=>requestCard(x,true)).join(''):'<div class="card empty">No pending requests right now.</div>'}</div></div><div><h2>My Sent Requests</h2><div class="tablelike">${outgoing.length?outgoing.map(x=>requestCard(x,false)).join(''):'<div class="card empty">You have not sent any pending requests.</div>'}</div></div></div>
    <h2>Accepted Connections</h2><p class="muted">Once a request is accepted, you can reply and continue the conversation here.</p><div class="tablelike">${accepted.length?accepted.map(x=>acceptedCard(x)).join(''):'<div class="card empty">Accepted connections will appear here.</div>'}</div>`;
  await loadBadge();
}
function requestCard(x,incoming){return `<div class="card request"><div><h3>${esc(incoming?x.senderName:x.receiverName)}</h3><p class="muted">${esc(incoming?x.senderEmail:x.receiverEmail)}</p>${x.projectTitle?`<p><b>Project:</b> ${esc(x.projectTitle)}</p>`:''}<p>${esc(x.message)}</p><small class="muted">${esc(x.createdAt||'')}</small></div><div class="request-actions">${incoming?`<button class="smallbtn" onclick="respondRequest(${x.id},'ACCEPTED')">✓ Accept</button><button class="smallbtn reject" onclick="respondRequest(${x.id},'REJECTED')">Reject</button>`:`<span class="status-pill pending">PENDING</span>`}</div></div>`}
function acceptedCard(x){const name=x.incoming?x.senderName:x.receiverName,email=x.incoming?x.senderEmail:x.receiverEmail;return `<div class="card request"><div><h3>${esc(name)}</h3><p class="muted">${esc(email)}</p>${x.projectTitle?`<span class="tag">${esc(x.projectTitle)}</span>`:''}<p>${esc(x.message)}</p><small class="muted">Connected successfully. You can now reply.</small></div><div class="request-actions"><span class="status-pill accepted">Accepted</span><button class="smallbtn" onclick="openConversation(${x.id})">💬 Reply</button></div></div>`}
async function openConversation(requestId){
  const contactsList=await api(`/contacts?userId=${state.id}`);const contact=Array.isArray(contactsList)?contactsList.find(x=>Number(x.id)===Number(requestId)):null;
  if(!contact){showToast('Connection not found.');return;}
  const name=contact.incoming?contact.senderName:contact.receiverName, projectTitle=contact.projectTitle||'';
  const messages=await api(`/messages?requestId=${requestId}&userId=${state.id}`);const arr=Array.isArray(messages)?messages:[];
  const history=arr.length?arr.map(m=>`<div class="chat-row ${m.mine?'mine':'theirs'}"><div class="chat-bubble"><div>${esc(m.message)}</div><small>${esc(m.createdAt||'')}</small></div></div>`).join(''):'<div class="empty">No messages yet.</div>';
  showModal(`<h2>Conversation with ${esc(name)}</h2>${projectTitle?`<p class="muted">Project: <b>${esc(projectTitle)}</b></p>`:''}<div class="chat-history">${history}</div><label>Reply</label><textarea id="replyMessage" rows="4" placeholder="Type your reply..."></textarea><button class="primary" onclick="sendReply(${requestId})">Send Reply</button><div id="replyMsg" class="msg"></div>`);
  const ta=document.getElementById('replyMessage');if(ta)ta.focus();
}
async function sendReply(requestId){
  const ta=document.getElementById('replyMessage'),box=document.getElementById('replyMsg');const message=(ta?.value||'').trim();
  if(!message){box.textContent='Please enter a reply.';box.classList.add('show');return;}
  const r=await api('/messages/send',{method:'POST',body:JSON.stringify({requestId,senderId:state.id,message})});
  box.textContent=r.message;box.classList.add('show');
  if(r.success){setTimeout(()=>openConversation(requestId),500);}
}
async function respondRequest(id,status){const r=await api('/contact/respond',{method:'POST',body:JSON.stringify({requestId:id,userId:state.id,status})});showToast(r.message);if(r.success)contacts();}
function showToast(msg){let t=document.getElementById('toast');if(!t){t=document.createElement('div');t.id='toast';t.className='toast';document.body.appendChild(t)}t.textContent=msg;t.classList.add('show');setTimeout(()=>t.classList.remove('show'),2500)}

if(location.pathname.endsWith('index.html')||location.pathname==='/'){
  let type='student';
  document.querySelectorAll('.tab').forEach(b=>b.onclick=()=>{document.querySelectorAll('.tab').forEach(x=>x.classList.remove('active'));b.classList.add('active');type=b.dataset.type});
  document.getElementById('loginForm').onsubmit=async e=>{e.preventDefault();const email=document.getElementById('email').value.trim(),password=document.getElementById('password').value;const r=await api('/login',{method:'POST',body:JSON.stringify({email,password,accountType:type})});const m=document.getElementById('msg');m.textContent=r.message;m.classList.add('show');if(r.success){save(r.user);location.href=r.user.type==='creator'?'creator.html':'dashboard.html'}};
}
if(location.pathname.endsWith('register.html')){
  const form=document.getElementById('registerForm'),nameEl=document.getElementById('name'),emailEl=document.getElementById('email'),passwordEl=document.getElementById('password'),typeEl=document.getElementById('type'),interestsEl=document.getElementById('interests'),skillsEl=document.getElementById('skills'),organizationEl=document.getElementById('organization'),expertiseEl=document.getElementById('expertise'),studentFieldsEl=document.getElementById('studentFields'),creatorFieldsEl=document.getElementById('creatorFields'),msgEl=document.getElementById('msg');
  typeEl.onchange=()=>{studentFieldsEl.classList.toggle('hidden',typeEl.value!=='student');creatorFieldsEl.classList.toggle('hidden',typeEl.value!=='creator')};
  form.onsubmit=async e=>{e.preventDefault();const d={fullName:nameEl.value.trim(),email:emailEl.value.trim(),password:passwordEl.value,accountType:typeEl.value,interests:interestsEl.value.trim(),skills:skillsEl.value.trim(),organization:organizationEl.value.trim(),expertise:expertiseEl.value.trim()};if(!d.fullName||!d.email||!d.password){msgEl.textContent='Please fill Full Name, Email and Password.';msgEl.classList.add('show');return}if(d.accountType==='student'&&(!d.interests||!d.skills)){msgEl.textContent='Please enter your interests and skills.';msgEl.classList.add('show');return}if(d.accountType==='creator'&&(!d.organization||!d.expertise)){msgEl.textContent='Please enter Organization / Department and Area of Expertise.';msgEl.classList.add('show');return}const r=await api('/register',{method:'POST',body:JSON.stringify(d)});msgEl.textContent=r.message;msgEl.classList.add('show');if(r.success)setTimeout(()=>location.href='index.html',900)};
}
if(window.PAGE==='studentDashboard')dashboard(false);if(window.PAGE==='creatorDashboard')dashboard(true);if(window.PAGE==='projects')projects();if(window.PAGE==='recommendations')rec();if(window.PAGE==='profile')profile();if(window.PAGE==='team')team();if(window.PAGE==='contacts')contacts();
