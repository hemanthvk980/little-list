export type Session={signedIn:boolean;csrf:string;local:boolean;google:boolean};
export type Gift={imageUrl?:string;id:string;name:string;category:string;note:string;retailerUrl:string;pricePaise:number|null;quantity:number;held:number;purchased:number;available:number;variant?:string;priority?:string;merchant?:string;captureSource?:string;capturedAt?:string};
export type Wishlist={id:string;title:string;occasion:string;greeting?:string;shareId:string|null;shortCode?:string|null;photoVersion?:string|null;items:Gift[]};
export type Claim={id:string;name:string;retailerUrl:string;status:string;heldUntil:string;giftId:string;shareId:string|null};
export type Starter={key:string;name:string;category:string;quantity:number;collections:string[]};
export function returnPath(){const p=sessionStorage.getItem('little-list-return');sessionStorage.removeItem('little-list-return');return p && /^\/(onboarding|capture|wishlists(?:\/|$))/.test(p)?p:'/dashboard';}
export async function api<T>(path:string,method='GET',body?:unknown):Promise<T> {
  let csrf='';
  if(method!=='GET') { const r=await fetch('/api/v1/session',{cache:'no-store'}); if(!r.ok) throw Error('Unable to connect. Please try again.'); csrf=(await r.json()).csrf; }
  const response=await fetch('/api/v1'+path,{method,cache:'no-store',headers:{'Content-Type':'application/json','X-CSRF-TOKEN':csrf},body:body===undefined?undefined:JSON.stringify(body)});
  if(response.status===401) { sessionStorage.setItem('little-list-return',location.pathname); window.location.assign('/sign-in'); throw Error('Please sign in to continue.'); }
  if(!response.ok) { const p=await response.json().catch(()=>({})); throw Error(p.detail || 'Something went wrong. Please try again.'); }
  return response.status===204 || response.status===202 ? undefined as T : response.json();
}
