// HTTP contract smoke test through Next's same-origin proxy. Mail goes only to local Mailpit.
import assert from 'node:assert/strict';
import { randomBytes } from 'node:crypto';
const base=process.env.SMOKE_URL||'http://localhost:3000';
function client(){let cookie='';return async(path,method='GET',body,csrf=true)=>{
  let token='';if(method!=='GET'&&csrf){const s=await fetch(base+'/api/v1/session',{headers:{cookie}});for(const c of s.headers.getSetCookie())cookie=c.split(';')[0];token=(await s.json()).csrf;}
  const r=await fetch(base+'/api/v1'+path,{method,headers:{cookie,'Content-Type':'application/json','X-CSRF-TOKEN':token},body:body===undefined?undefined:JSON.stringify(body)});for(const c of r.headers.getSetCookie())cookie=c.split(';')[0];return r;
};}
async function login(user){const s=await fetch(base+'/api/v1/session');const cookie=s.headers.getSetCookie().map(c=>c.split(';')[0]).join('; ');const token=(await s.json()).csrf;const r=await fetch(base+'/api/v1/local-login',{method:'POST',headers:{cookie,'Content-Type':'application/x-www-form-urlencoded','X-CSRF-TOKEN':token},body:new URLSearchParams({username:user,password:'littlelist-local'})});assert.equal(r.status,204);let auth=r.headers.getSetCookie().map(c=>c.split(';')[0]).join('; ')||cookie;
assert.notEqual(auth,cookie,'Session identifier rotates on login');
return async(path,method='GET',body,csrf=true)=>{let token='';if(method!=='GET'&&csrf){const s=await fetch(base+'/api/v1/session',{headers:{cookie:auth}});token=(await s.json()).csrf;}return fetch(base+'/api/v1'+path,{method,headers:{cookie:auth,'Content-Type':'application/json','X-CSRF-TOKEN':token},body:body===undefined?undefined:JSON.stringify(body)});};}
const host=await login('demo'), other=await login('another'), guest=client();
assert.equal((await guest('/wishlists')).status,401);
assert.equal((await host('/wishlists','POST',{title:'CSRF must block'},false)).status,403);
const created=await host('/wishlists','POST',{title:'HTTP smoke test',occasion:'Local verification'});assert.equal(created.status,201);const list=await created.json();
try{
 assert.equal((await other('/wishlists/'+list.id)).status,404);
 const added=await host('/wishlists/'+list.id+'/items','POST',{name:'Smoke swaddle',category:'Sleep',note:'Test only',retailerUrl:'https://example.com/swaddle',quantity:1,pricePaise:99000});assert.equal(added.status,201);const gift=(await added.json()).items[0];
 const shared=await (await host('/wishlists/'+list.id+'/publication','PUT')).json();const path='/shared/'+shared.shareId;
 assert.match(shared.shortCode,/^[A-Za-z0-9_-]{22}$/);
 const short=base+'/r/'+shared.shortCode;
 const redirect=await fetch(short+'?via=whatsapp',{redirect:'manual'});
 assert.equal(redirect.status,302);assert.equal(redirect.headers.get('location'),path);assert.match(redirect.headers.get('cache-control'),/no-store/);
 assert.equal((await fetch(short,{method:'HEAD',redirect:'manual'})).status,302);
 assert.equal((await fetch(short,{headers:{'User-Agent':'facebookexternalhit/1.1'},redirect:'manual'})).status,302);
 assert.equal((await (await host('/wishlists/'+list.id+'/link-stats')).json()).totalOpens,1);
 assert.equal((await fetch(short+'?via=qr',{redirect:'manual'})).status,302);
 const qrStats=await (await host('/wishlists/'+list.id+'/link-stats')).json();assert.equal(qrStats.totalOpens,2);assert.equal(qrStats.days.find(d=>d.channel==='qr').opens,1);
 assert.equal((await other('/wishlists/'+list.id+'/link-stats')).status,404);
 assert.equal((await guest('/wishlists/'+list.id+'/link-stats')).status,401);
 const view=await guest(path);assert.equal(view.status,200);assert.match(view.headers.get('cache-control'),/no-store/);const publicBody=await view.json();assert.ok(!JSON.stringify(publicBody).includes('owner_subject'));
 const email='smoke-'+Date.now()+'@example.test';assert.equal((await guest(path+'/items/'+gift.id+'/claim-requests','POST',{email})).status,202);
 assert.equal((await (await guest(path)).json()).items[0].available,1);
 const inbox=await (await fetch('http://localhost:8025/api/v1/messages')).json();const entry=inbox.messages.find(m=>m.To.some(to=>to.Address===email));assert.ok(entry,'Mailpit received verification');
 const mail=await (await fetch('http://localhost:8025/api/v1/message/'+entry.ID)).json();const token=mail.Text.match(/#token=([a-f0-9]{64})/)[1];const managementToken=randomBytes(32).toString('hex');
 assert.equal((await fetch(base+'/confirm#token='+token)).status,200);assert.equal((await (await guest(path)).json()).items[0].available,1);
 const confirmation=await guest('/claims/confirm','POST',{token,managementToken});assert.equal(confirmation.status,200);const claim=await confirmation.json();
 assert.equal((await (await guest('/claims/confirm','POST',{token,managementToken})).json()).id,claim.id);
 const extraList=await (await host('/wishlists/'+list.id+'/items','POST',{name:'Purchased book',category:'Play',quantity:1})).json();
 const extra=extraList.items.find(g=>g.name==='Purchased book'),purchasedEmail='purchased-'+Date.now()+'@example.test';
 assert.equal((await guest(path+'/items/'+extra.id+'/claim-requests','POST',{email:purchasedEmail,intent:'purchase'})).status,202);
 const purchaseInbox=await (await fetch('http://localhost:8025/api/v1/messages')).json();
 const purchaseEntry=purchaseInbox.messages.find(m=>m.To.some(to=>to.Address===purchasedEmail));assert.ok(purchaseEntry);
 const purchaseMail=await (await fetch('http://localhost:8025/api/v1/message/'+purchaseEntry.ID)).json();assert.match(purchaseMail.Text,/intent=purchase/);
 const purchaseToken=purchaseMail.Text.match(/#token=([a-f0-9]{64})/)[1],purchaseSecret=randomBytes(32).toString('hex');
 assert.equal((await (await guest('/claims/confirm','POST',{token:purchaseToken,managementToken:purchaseSecret})).json()).status,'purchased');
 const publicGift=(await (await guest(path)).json()).items.find(g=>g.id===extra.id);assert.equal(publicGift.purchased,1);assert.equal(publicGift.available,0);
 await host('/wishlists/'+list.id+'/publication','DELETE');assert.equal((await guest(path)).status,404);
 assert.equal((await fetch(short,{redirect:'manual'})).status,404);
 for(let i=0;i<2;i++){const r=await guest('/claims/purchase','POST',{token:managementToken});assert.equal(r.status,200);assert.equal((await r.json()).status,'purchased');}
 assert.equal((await guest('/claims/release','POST',{token:managementToken})).status,409);
 const logout=await host('/logout','POST');assert.equal(logout.status,204);assert.equal((await host('/wishlists')).status,401);
 console.log('PASS: same-origin login, CSRF rejection, ownership, private/public DTOs, no-store, Mailpit delivery, GET safety, confirm retry, revocation, purchase retry, final-state conflict, logout.');
}finally{const cleanup=await login('demo');await cleanup('/wishlists/'+list.id,'DELETE');}
