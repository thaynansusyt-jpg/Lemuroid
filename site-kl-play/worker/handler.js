const PLUS={"version":"0.5.0-plus.1","size":209509274,"sha256":"0b49706fa4147e9b18125500e5c5e0bb99221cb08446dd9511086ed7fba53b4f","filename":"KL-Play-0.5.0-plus.1.apk","url":"/downloads/KL-Play-0.5.0-plus.1.apk","host":"Hospedado no GitHub","asset":"https://github.com/thaynansusyt-jpg/Lemuroid/releases/download/v0.5.0-plus.1/KL-Play-0.5.0-plus.1.apk"};
const KEY='downloads/KL-Play-0.5.0-rc.1.apk', SIZE=202534464, SHA='709511255f7dabbbfe26dbeac898a985780c94808af205190fadbecf18c0b825';
const LEGACY={key:'downloads/KL-GBA-0.1.0-beta.1.apk',size:199005449,sha:'c964c3bc80ffb693ee614ff8d1135a47448ae0154d7a8aa0660bc5d35fa38fdd'};
const json=(data,status=200)=>new Response(JSON.stringify(data),{status,headers:{'Content-Type':'application/json','Cache-Control':'no-store'}});
export default {async fetch(request,env){
 const u=new URL(request.url),path=u.pathname;
 try{
 const account=await accountRoute(request,env);if(account)return account;
 if(path==='/api/download-status'){if(PLUS)return json({...PLUS,available:true});const o=await env.BUCKET.head(KEY);return json({available:!!o&&o.size===SIZE,size:SIZE,sha256:SHA});}
 if(PLUS&&path===PLUS.url&&['GET','HEAD'].includes(request.method))return Response.redirect(PLUS.asset,307);
 if(PLUS&&path==='/downloads/SHA256SUMS.txt')return new Response(PLUS.sha256+'  '+PLUS.filename+'\n',{headers:{'Content-Type':'text/plain; charset=utf-8','Cache-Control':'no-store'}});
 if(path==='/downloads/SHA256SUMS.txt')return new Response(SHA+'  KL-Play-0.5.0-rc.1.apk\n',{headers:{'Content-Type':'text/plain; charset=utf-8','Cache-Control':'public, max-age=300'}});
 const release=path==='/'+KEY?{key:KEY,size:SIZE,sha:SHA}:path==='/'+LEGACY.key?LEGACY:null;
 if(release){
  if(!['GET','HEAD'].includes(request.method))return new Response(null,{status:405});
  const head=await env.BUCKET.head(release.key);if(!head||head.size!==release.size)return new Response('Download em preparação',{status:503,headers:{'Retry-After':'60'}});
  let offset=0,length=release.size,status=200;const range=request.headers.get('Range');
  if(range&&(!request.headers.has('If-Range')||request.headers.get('If-Range')===head.httpEtag)){
   const match=/^bytes=(\d*)-(\d*)$/.exec(range);if(!match||(!match[1]&&!match[2]))return new Response(null,{status:416,headers:{'Content-Range':`bytes */${release.size}`}});
   if(!match[1]){length=Math.min(Number(match[2]),release.size);offset=release.size-length}else{offset=Number(match[1]);const end=match[2]?Math.min(Number(match[2]),release.size-1):release.size-1;length=end-offset+1}
   if(offset>=release.size||length<=0||!Number.isSafeInteger(offset)||!Number.isSafeInteger(length))return new Response(null,{status:416,headers:{'Content-Range':`bytes */${release.size}`}});status=206;
  }
  const filename=release.key.split('/').pop();
  const headers=new Headers({'Content-Type':'application/vnd.android.package-archive','Content-Disposition':`attachment; filename="${filename}"`,'Content-Length':String(length),'Accept-Ranges':'bytes','ETag':head.httpEtag,'Cache-Control':'public, max-age=3600','X-Content-Type-Options':'nosniff','X-Checksum-SHA256':release.sha});
  if(status===206)headers.set('Content-Range',`bytes ${offset}-${offset+length-1}/${release.size}`);
  if(request.method==='HEAD')return new Response(null,{status,headers});
  const obj=await env.BUCKET.get(release.key,status===206?{range:{offset,length}}:{});return new Response(obj.body,{status,headers});
 }
 const asset=assets[path];if(asset){if(!['GET','HEAD'].includes(request.method))return new Response(null,{status:405});const body=asset.binary?Uint8Array.from(atob(asset.body),c=>c.charCodeAt(0)):asset.body;return new Response(request.method==='HEAD'?null:body,{headers:{'Content-Type':asset.type,'X-Content-Type-Options':'nosniff','Cache-Control':path==='/'?'no-cache':'public, max-age=300'}})}
 return new Response('Página não encontrada',{status:404});
 }catch{return json({error:'Temporarily unavailable'},503)}
}};
