import { NextRequest } from 'next/server';
export const dynamic='force-dynamic';
const headers={'Cache-Control':'no-store','X-Robots-Tag':'noindex, nofollow','Referrer-Policy':'no-referrer'};
function unavailable(request:NextRequest,status:404|503){
 const temporary=status===503;
 // No request data is interpolated into HTML. An empty href retries this exact URL.
 const html=`<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>${temporary?'A little hiccup':'Registry unavailable'} | Little List</title><style>body{margin:0;background:#faf8f3;color:#382d35;font:18px/1.7 system-ui,sans-serif}main{max-width:640px;margin:12vh auto;padding:24px;text-align:center}h1{font-family:Georgia,serif;font-size:clamp(32px,6vw,48px)}.art{font-size:72px}a{display:inline-block;background:#63445d;color:white;padding:12px 20px;border-radius:12px;margin:8px;text-decoration:none}a:focus-visible{outline:3px solid #bc7b39;outline-offset:4px}</style></head><body><main><div class="art" aria-hidden="true">${temporary?'🧸 ☁️':'🧦 ✨'}</div><h1>${temporary?'A little hiccup.':'This little list isn’t available.'}</h1><p>${temporary?'We couldn’t reach this registry just now. Please try again in a moment.':'The link may have changed. Ask the person who shared it for a current link.'}</p>${temporary?'<a href="">Try again</a>':''}<a href="/">Back to Little List</a><a href="/sample">Explore a sample</a></main></body></html>`;
 return new Response(request.method==='HEAD'?null:html,{status,headers:{...headers,'Content-Type':'text/html; charset=utf-8',...(temporary?{'Retry-After':'30'}:{})}});
}
async function resolve(request:NextRequest,{params}:{params:Promise<{code:string}>}) {
 const {code}=await params;
 if(!/^[A-Za-z0-9_-]{22}$/.test(code))return unavailable(request,404);
 try {
  const target=new URL('/r/'+code,process.env.BACKEND_URL||'http://127.0.0.1:8080');
  const via=request.nextUrl.searchParams.get('via');if(via)target.searchParams.set('via',via);
  const forwarded=new Headers();for(const key of ['user-agent','purpose','sec-purpose']){const value=request.headers.get(key);if(value)forwarded.set(key,value);}
  const result=await fetch(target,{method:request.method,headers:forwarded,redirect:'manual',cache:'no-store',signal:AbortSignal.timeout(15000)});
  const location=result.headers.get('location');
  if(result.status===302&&location&&/^\/shared\/[0-9a-f-]{36}$/.test(location))return new Response(null,{status:302,headers:{...headers,Location:location}});
  return unavailable(request,result.status===404?404:503);
 }catch{return unavailable(request,503);}
}
export {resolve as GET,resolve as HEAD};
