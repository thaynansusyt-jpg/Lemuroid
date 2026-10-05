'use strict';
const themeButton=document.getElementById('theme-toggle');
function setTheme(theme){
 const dark=theme==='dark';document.documentElement.dataset.theme=dark?'dark':'light';
 themeButton.setAttribute('aria-pressed',String(dark));themeButton.setAttribute('aria-label',dark?'Ativar tema claro':'Ativar tema escuro');
 document.getElementById('theme-label').textContent=dark?'Escuro':'Claro';
 document.querySelector('meta[name="theme-color"]').content=dark?'#0b1019':'#f6f9fb';
 try{localStorage.setItem('kl-theme',dark?'dark':'light')}catch{}
}
setTheme(document.documentElement.dataset.theme);
themeButton.addEventListener('click',()=>setTheme(document.documentElement.dataset.theme==='dark'?'light':'dark'));
const skins={green:'Verde',fire:'Vermelho',light:'Claro',dark:'Escuro'};
document.querySelectorAll('[data-skin]').forEach(button=>button.addEventListener('click',()=>{
 const skin=button.dataset.skin, image=document.getElementById('skin-preview');image.src='/assets/skin-'+skin+'.webp';image.alt='Screenshot real do KL Play com controles '+skins[skin].toLowerCase()+' em Pokémon Quetzal';
 document.getElementById('skin-name').textContent=skins[skin];
 document.querySelectorAll('[data-skin]').forEach(item=>{const selected=item===button;item.classList.toggle('active',selected);item.setAttribute('aria-pressed',String(selected))});
}));
const dialog=document.getElementById('photo-dialog');
document.querySelectorAll('[data-image]').forEach(button=>button.addEventListener('click',()=>{
 const image=document.getElementById('photo-full');image.src='/assets/'+button.dataset.image+'.webp';image.alt=button.dataset.caption;
 document.getElementById('photo-caption').textContent=button.dataset.caption;dialog.showModal();
}));
dialog.querySelector('button').addEventListener('click',()=>dialog.close());
dialog.addEventListener('click',event=>{if(event.target===dialog){const rect=dialog.getBoundingClientRect();if(event.clientX<rect.left||event.clientX>rect.right||event.clientY<rect.top||event.clientY>rect.bottom)dialog.close()}});
async function loadDownload(){
 const state=document.getElementById('release-state'), button=document.getElementById('apk-link'), note=document.getElementById('download-note');
 button.addEventListener('click',event=>{if(button.getAttribute('aria-disabled')==='true')event.preventDefault()});
 try{
  const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),10000);
  let response;try{response=await fetch('/api/download-status',{cache:'no-store',signal:controller.signal})}finally{clearTimeout(timer)}
  if(!response.ok)throw new Error('Unavailable');const info=await response.json();
  if(!info.available){state.textContent='APK novo em preparação';button.textContent='Download em preparação';return}
  button.href='/downloads/KL-Play-0.5.0-rc.1.apk';button.setAttribute('download','KL-Play-0.5.0-rc.1.apk');button.removeAttribute('aria-disabled');button.textContent='Baixar APK para Android ↓';state.textContent='0.5.0-rc.1 disponível';note.textContent='Download direto • '+(info.size/1048576).toFixed(1).replace('.',',')+' MB • sem conta GitHub.';
 }catch{state.textContent='Download temporariamente indisponível';button.textContent='Tente novamente em instantes';note.textContent='Recarregue a página para conferir o download.'}
}
loadDownload();
