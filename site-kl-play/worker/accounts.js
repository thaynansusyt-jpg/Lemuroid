const ACCOUNT_ORIGIN='https://kl-gba-play.emilysousa65477.chatgpt.site';
const encoder=new TextEncoder();
const hex=b=>Array.from(new Uint8Array(b),x=>x.toString(16).padStart(2,'0')).join('');
const random=n=>hex(crypto.getRandomValues(new Uint8Array(n)));
const digest=async value=>hex(await crypto.subtle.digest('SHA-256',encoder.encode(value)));
const reply=(body,status=200)=>new Response(JSON.stringify(body),{status,headers:{'Content-Type':'application/json','Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'}});
class AccountError extends Error { constructor(message,status=400){super(message);this.status=status} }
const query=(env,sql,...args)=>env.DB.prepare(sql).bind(...args);
async function limited(env,request,kind,max,window=900){
 const now=Math.floor(Date.now()/1000),bucket=Math.floor(now/window),ip=kind==='global-create'?'all':request.headers.get('CF-Connecting-IP')||'unknown';
 const key=await digest(kind+':'+ip+':'+bucket);
 const row=await query(env,'INSERT INTO kl_limits (key,count,expires) VALUES (?,1,?) ON CONFLICT(key) DO UPDATE SET count=count+1 RETURNING count',key,now+window).first();
 if(row.count>max)throw new AccountError('Muitas tentativas. Aguarde alguns minutos.',429);
 // Expired rows are bounded garbage collection, not schema changes.
 await query(env,'DELETE FROM kl_limits WHERE key IN (SELECT key FROM kl_limits WHERE expires < ? LIMIT 50)',now).run();
}
async function input(request,max=1048576){
 if(!request.headers.get('Content-Type')?.startsWith('application/json'))throw new AccountError('Envie dados JSON.',415);
 if(Number(request.headers.get('Content-Length'))>max)throw new AccountError('O perfil ultrapassa o limite de 1 MB.',413);
 const reader=request.body?.getReader();if(!reader)throw new AccountError('Dados ausentes.');
 const chunks=[];let size=0;while(true){const {done,value}=await reader.read();if(done)break;size+=value.length;if(size>max){await reader.cancel();throw new AccountError('O perfil ultrapassa o limite de 1 MB.',413)}chunks.push(value)}
 const bytes=new Uint8Array(size);let at=0;for(const c of chunks){bytes.set(c,at);at+=c.length}
 try{return JSON.parse(new TextDecoder().decode(bytes))}catch{throw new AccountError('Dados inválidos.')}
}
async function authenticate(request,env){
 const token=request.headers.get('Authorization')?.replace(/^Bearer /,'');
 if(!token||!/^\w{64}$/.test(token))throw new AccountError('Entre na conta KL novamente.',401);
 const hash=await digest(token),now=Math.floor(Date.now()/1000);
 const row=await query(env,'SELECT a.*,s.hash AS session_hash FROM kl_sessions s JOIN kl_accounts a ON a.id=s.account WHERE s.hash=? AND s.expires>?',hash,now).first();
 if(!row)throw new AccountError('Sua sessão expirou. Entre novamente.',401);return row;
}
async function session(env,account){
 const token=random(32),expires=Math.floor(Date.now()/1000)+30*86400;
 await query(env,'INSERT INTO kl_sessions (hash,account,expires) VALUES (?,?,?)',await digest(token),account,expires).run();
 await query(env,'DELETE FROM kl_sessions WHERE hash IN (SELECT hash FROM kl_sessions WHERE expires < ? LIMIT 50)',Math.floor(Date.now()/1000)).run();return token;
}
const accountInfo=row=>({id:row.id,username:row.username,revision:row.revision,updated:row.updated,siiPlus:true});
const validColor=(s)=>typeof s==='string'&&/^#[0-9a-fA-F]{6}$/.test(s);
function validateProfile(p){
 if(!p||typeof p!=='object'||Array.isArray(p)||typeof p.name!=='string'||p.name.length>32||!p.sii||!p.days||typeof p.days!=='object'||Array.isArray(p.days))throw new AccountError('Perfil inválido.');
 if(!validColor(p.sii.ink)||!validColor(p.sii.shirt)||!['kl','tee','hoodie','sport','sii_plus','sonic','super_sonic'].includes(p.sii.clothes)||!['happy','cool','calm'].includes(p.sii.face))throw new AccountError('Sii inválido.');
 const achievements=p.achievements||[],events=p.events||[];
 const eventNames=['sonic_theme','wiiu_theme','sonic_skin','fullscreen','nds_layout','3ds_layout','profile_name','sii_edit'];
 if(!Array.isArray(achievements)||achievements.length>128||achievements.some(x=>!Number.isInteger(x)||x<1||x>128)||new Set(achievements).size!==achievements.length||!Array.isArray(events)||events.length>8||events.some(x=>!eventNames.includes(x))||new Set(events).size!==events.length)throw new AccountError('Conquistas inválidas.');
 const badge=p.sii.badge||0;
 if(!Number.isInteger(badge)||badge<0||badge>128||(badge&&!achievements.includes(badge))||(p.sii.clothes==='sonic'&&!achievements.includes(1))||(p.sii.clothes==='super_sonic'&&!achievements.includes(100)))throw new AccountError('Recompensa inválida.');
 if(Object.keys(p.days).length>4000)throw new AccountError('O diário excede o limite desta versão.');
 for(const [date,d] of Object.entries(p.days)){
  if(!/^\d{4}-\d{2}-\d{2}$/.test(date)||!Number.isFinite(Date.parse(date))||new Date(date).toISOString().slice(0,10)!==date||!d||!Number.isSafeInteger(d.millis||0)||(d.millis||0)<0||!Number.isSafeInteger(d.sessions||0)||(d.sessions||0)<0||typeof(d.note||'')!=='string'||(d.note||'').length>2000)throw new AccountError('Diário inválido.');
  for(const key of ['sonicMillis','screenMillis','sonicSessions'])if(d[key]!==undefined&&(!Number.isSafeInteger(d[key])||d[key]<0||d[key]>(key==='sonicSessions'?d.sessions||0:d.millis||0)))throw new AccountError('Progresso inválido.');
  if(d.games&& (typeof d.games!=='object'||Array.isArray(d.games)||Object.keys(d.games).length>1000))throw new AccountError('Jogos inválidos.');
  for(const [id,g] of Object.entries(d.games||{}))if(id.length>200||!g||typeof g.title!=='string'||g.title.length>160||!Number.isSafeInteger(g.millis)||g.millis<0)throw new AccountError('Estatísticas inválidas.');
 }
 // Ignore identity/provider flags from the client; server owns account identity and Sii+ entitlement.
 return {name:p.name,sii:{ink:p.sii.ink,shirt:p.sii.shirt,clothes:p.sii.clothes,face:p.sii.face,badge},days:p.days,achievements,events};
}
async function accountRoute(request,env){
 const path=new URL(request.url).pathname;
 if(!path.startsWith('/api/kl/'))return null;
 try{
  if(!env.DB)throw new AccountError('Contas temporariamente indisponíveis.',503);
  const origin=request.headers.get('Origin');if(origin&&origin!==ACCOUNT_ORIGIN)throw new AccountError('Origem não permitida.',403);
  if(path==='/api/kl/register'&&request.method==='POST'){
   await input(request,1024);await limited(env,request,'create',5,3600);await limited(env,request,'global-create',500,86400);
   const id=random(16),username='KL-'+random(8).toUpperCase(),password=random(32),now=Math.floor(Date.now()/1000);
   await query(env,'INSERT INTO kl_accounts (id,username,password_hash,created) VALUES (?,?,?,?)',id,username,await digest(id+':'+password),now).run();
   return reply({id,username,password,siiPlus:true},201);
  }
  if(path==='/api/kl/login'&&request.method==='POST'){
   await limited(env,request,'login',50);const data=await input(request,2048),username=String(data.username||'').trim().toUpperCase(),password=String(data.password||'').trim();
   if(!/^KL-[0-9A-F]{16}$/.test(username)||! /^[0-9a-f]{64}$/.test(password))throw new AccountError('Nome ou senha incorretos.',401);
   const row=await query(env,'SELECT * FROM kl_accounts WHERE username=?',username).first();
   if(!row||await digest(row.id+':'+password)!==row.password_hash)throw new AccountError('Nome ou senha incorretos.',401);
   return reply({...accountInfo(row),token:await session(env,row.id)});
  }
  const row=await authenticate(request,env);
  if(path==='/api/kl/profile'&&request.method==='GET')return reply({...accountInfo(row),profile:row.revision?JSON.parse(row.profile):null});
  if(path==='/api/kl/profile'&&request.method==='PUT'){
   await limited(env,request,'backup:'+row.id,120);const data=await input(request);
   if(!Number.isSafeInteger(data.revision)||data.revision<0)throw new AccountError('Revisão inválida.');
   const profile=JSON.stringify(validateProfile(data.profile)),now=Math.floor(Date.now()/1000);
   const updated=await query(env,'UPDATE kl_accounts SET profile=?,revision=revision+1,updated=? WHERE id=? AND revision=? RETURNING revision',profile,now,row.id,data.revision).first();
   if(!updated)throw new AccountError('Existe um backup mais recente. Seus dados locais foram preservados. Restaure o backup antes de enviar novamente.',409);
   return reply({revision:updated.revision,updated:now});
  }
  if(path==='/api/kl/logout'&&request.method==='POST'){await query(env,'DELETE FROM kl_sessions WHERE hash=?',row.session_hash).run();return reply({ok:true})}
  if(path==='/api/kl/account'&&request.method==='DELETE'){
   await env.DB.batch([query(env,'DELETE FROM kl_sessions WHERE account=?',row.id),query(env,'DELETE FROM kl_accounts WHERE id=?',row.id)]);return reply({ok:true});
  }
  return reply({error:'Rota não encontrada.'},404);
 }catch(e){if(e instanceof AccountError)return reply({error:e.message},e.status);console.error('KL account storage unavailable');return reply({error:'Não foi possível acessar a conta. Tente novamente.'},503)}
}
