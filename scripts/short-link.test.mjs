import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { spawn } from 'node:child_process';
import { once } from 'node:events';

// Exercise the built Next route against a controlled backend, without touching registry data.
test('short links preserve redirects, attribution and HEAD; failures return branded status pages', {timeout:60000}, async t => {
 const requests=[];
 const backend=http.createServer((req,res)=>{
  requests.push({url:req.url,method:req.method,agent:req.headers['user-agent'],purpose:req.headers.purpose});
  if(req.url.startsWith('/r/'+ 'a'.repeat(22))){res.writeHead(302,{Location:'/shared/00000000-0000-0000-0000-000000000001'});res.end();}
  else if(req.url.startsWith('/r/'+ 'b'.repeat(22))){res.writeHead(404);res.end();}
  else if(req.url.startsWith('/r/'+ 'd'.repeat(22))){res.writeHead(302,{Location:'https://untrusted.example/'});res.end();}
  else {res.writeHead(500);res.end();}
 });
 backend.listen(0,'127.0.0.1');await once(backend,'listening');
 t.after(()=>{backend.closeAllConnections();backend.close();});
 const reservation=http.createServer();reservation.listen(0,'127.0.0.1');await once(reservation,'listening');const port=reservation.address().port;await new Promise(r=>reservation.close(r));
 const child=spawn(process.execPath,['frontend/node_modules/next/dist/bin/next','start','frontend','--hostname','127.0.0.1','--port',String(port)],{env:{...process.env,BACKEND_URL:'http://127.0.0.1:'+backend.address().port},stdio:'ignore'});
 t.after(()=>child.kill());
 const base='http://127.0.0.1:'+port;
 let ready=false;
 for(let i=0;i<80;i++){try{if((await fetch(base+'/help')).ok){ready=true;break;}}catch{}await new Promise(r=>setTimeout(r,250));}
 assert.ok(ready,'Next server starts');
 const good=await fetch(base+'/r/'+'a'.repeat(22)+'?via=qr',{redirect:'manual',headers:{'User-Agent':'test-browser'}});
 assert.equal(good.status,302);assert.equal(good.headers.get('location'),'/shared/00000000-0000-0000-0000-000000000001');assert.equal(good.headers.get('cache-control'),'no-store');assert.ok(requests.some(r=>r.url.endsWith('?via=qr')));
 const head=await fetch(base+'/r/'+'a'.repeat(22),{method:'HEAD',redirect:'manual',headers:{Purpose:'prefetch','User-Agent':'WhatsApp'}});
 assert.equal(head.status,302);assert.equal(await head.text(),'');assert.ok(requests.some(r=>r.method==='HEAD'&&r.purpose==='prefetch'&&r.agent==='WhatsApp'));
 for(const code of ['invalid','b'.repeat(22)]){const r=await fetch(base+'/r/'+code);assert.equal(r.status,404);assert.match(await r.text(),/This little list isn’t available/);}
 for(const code of ['c'.repeat(22),'d'.repeat(22)]){const r=await fetch(base+'/r/'+code,{redirect:'manual'});assert.equal(r.status,503);assert.equal(r.headers.get('retry-after'),'30');assert.match(await r.text(),/A little hiccup/);}
});
