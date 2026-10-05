import fs from 'node:fs';
import path from 'node:path';
const types={html:'text/html; charset=utf-8',css:'text/css; charset=utf-8',js:'text/javascript; charset=utf-8',svg:'image/svg+xml',webp:'image/webp',png:'image/png',txt:'text/plain; charset=utf-8'};
const assets={};
function collect(folder){for(const entry of fs.readdirSync(folder,{withFileTypes:true})){const file=path.join(folder,entry.name);if(entry.isDirectory()){collect(file);continue}const name=path.relative('frontend',file).split(path.sep).join('/'),ext=name.split('.').pop();const binary=['webp','png'].includes(ext);assets[name==='index.html'?'/':'/'+name]={body:fs.readFileSync(file,binary?'base64':'utf8'),type:types[ext]||'application/octet-stream',binary};}}
collect('frontend');fs.writeFileSync('worker/index.js','const assets = '+JSON.stringify(assets)+';\n'+fs.readFileSync('worker/handler.js','utf8'));
