'use client';
import { Suspense, useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import styles from './admin.module.css';
type Row = Record<string, string | number | boolean | null>;
type Page = {items: Row[]; total: number; page: number; size: number};
async function get<T>(path: string, signal: AbortSignal): Promise<T> {
 const res = await fetch('/api/v1'+path,{cache:'no-store',signal:AbortSignal.any([signal,AbortSignal.timeout(20000)])});
 if(!res.ok) throw new Error(res.status===401?'Please sign in to continue.':res.status===403?'This area is available only to the Little List owner.':res.status===404?'This record is no longer available.':res.status===429?'Too many requests. Please wait a minute.':'We couldn’t load these records. Please try again.');
 return res.json();
}
function Admin() {
 const params=useSearchParams(),router=useRouter();
 const user=params.get('user'),registry=params.get('registry'),tab=params.get('tab')||'items';
 const page=Math.max(0,Number(params.get('page'))||0),size=Number(params.get('size'))||20,q=params.get('q')||'';
 const [data,setData]=useState<Page>(),[detail,setDetail]=useState<Row>(),[stats,setStats]=useState<Row>(),[error,setError]=useState(''),[busy,setBusy]=useState(true),[retry,setRetry]=useState(0);
 function navigate(changes:Record<string,string|null>){const next=new URLSearchParams(params.toString());for(const [key,value] of Object.entries(changes)){if(value===null)next.delete(key);else next.set(key,value);}router.push('/admin?'+next.toString());}
 useEffect(()=>{const ctrl=new AbortController();setBusy(true);setError('');setData(undefined);setDetail(undefined);
 (async()=>{const session=await get<{admin:boolean}>('/session',ctrl.signal);if(!session.admin)throw new Error('This area is available only to the Little List owner.');
 const base=registry?'/admin/registries/'+encodeURIComponent(registry):user?'/admin/users/'+encodeURIComponent(user):'/admin';
 const endpoint=registry?base+'/'+(tab==='checklist'?'checklist':'items'):user?base+'/registries':base+'/users';
 const [records,record,overview]=await Promise.all([get<Page>(endpoint+'?'+new URLSearchParams({page:String(page),size:String(size),q,hasRegistries:params.get('hasRegistries')||'true'}),ctrl.signal),user||registry?get<Row>(base,ctrl.signal):Promise.resolve(undefined),get<Row>('/admin/overview',ctrl.signal)]);
 if(!ctrl.signal.aborted){setData(records);setDetail(record);setStats(overview);}
 })().catch(e=>{if(!ctrl.signal.aborted)setError(e.message);}).finally(()=>{if(!ctrl.signal.aborted)setBusy(false);});return()=>ctrl.abort();
 },[params,retry,user,registry,tab,page,size,q]);
 const columns=registry?(tab==='checklist'?['item_key','state']:['name','category','variant','price_paise','quantity','reserved','purchased','priority','note','retailer_url']):user?['title','occasion','published','gift_count','created_at']:['display_name','email','registry_count','first_seen','last_login'];
 function value(row:Row,key:string){const v=row[key];if(key==='price_paise')return v===null?'—':new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format(Number(v)/100);if(key==='published')return v?'Published':'Private draft';if(key.endsWith('_at')||key==='first_seen'||key==='last_login')return v?new Date(String(v)).toLocaleString():'—';return v===null||v===undefined?'—':String(v);}
 return <main className={styles.page}><p className="eyebrow">OWNER WORKSPACE · READ ONLY</p><h1>{registry?'Registry details':user?'User registries':'Users'}</h1>
 <nav aria-label="Admin breadcrumbs"><Link href="/dashboard">My dashboard</Link> / <Link href="/admin">Users</Link>{registry&&detail?.user_id&&<> / <Link href={'/admin?user='+detail.user_id}>User registries</Link></>}</nav>
 {error?<div className="panel" role="alert"><h2>We couldn’t open this view</h2><p>{error}</p><button onClick={()=>setRetry(n=>n+1)}>Try again</button> <Link href="/sign-in">Sign in</Link></div>:<>
 {stats&&<p className={styles.stats}>{stats.users} {stats.users===1?'user':'users'} · {stats.registries} {stats.registries===1?'registry':'registries'} · {stats.gifts} {stats.gifts===1?'gift':'gifts'}</p>}
 {detail&&<section className="panel"><h2>{String(detail.title||detail.display_name||'Profile not yet available')}</h2><p>{String(detail.email||detail.occasion||'')}</p>{detail.greeting&&<p>{String(detail.greeting)}</p>}</section>}
 {!user&&!registry&&<form className={styles.controls} onSubmit={e=>{e.preventDefault();navigate({q:String(new FormData(e.currentTarget).get('q')||''),page:'0'});}}><label>Search name or email<input key={q} name="q" defaultValue={q} maxLength={100}/></label><button>Search</button><label><input type="checkbox" checked={params.get('hasRegistries')!=='false'} onChange={e=>navigate({hasRegistries:String(e.target.checked),page:'0'})}/> Has registries</label></form>}
 {registry&&<nav className={styles.controls} aria-label="Registry sections"><button className={tab==='items'?'':'secondary'} onClick={()=>navigate({tab:'items',page:'0'})}>Gift items</button><button className={tab==='checklist'?'':'secondary'} onClick={()=>navigate({tab:'checklist',page:'0'})}>Checklist</button></nav>}
 {busy?<div className={styles.skeleton} role="status">Loading records…</div>:data&&<><div className={styles.table} tabIndex={0} role="region" aria-label="Admin records"><table><caption>{registry?'Registry records':user?'Saved registries':'Registry owners'}</caption><thead><tr>{columns.map(c=><th scope="col" key={c}>{c.replaceAll('_',' ').replace('price paise','Price (INR)')}</th>)}</tr></thead><tbody>{data.items.map((row,index)=><tr key={String(row.id||index)}>{columns.map((c,i)=><td key={c}>{i===0&&!registry?<Link href={user?'/admin?registry='+row.id:'/admin?user='+row.id}>{value(row,c)==='—'?'Profile pending':value(row,c)}</Link>:value(row,c)}</td>)}</tr>)}</tbody></table>{!data.items.length&&<p>No records match this view.</p>}</div>
 <div className={styles.controls}><button className="secondary" disabled={page===0} onClick={()=>navigate({page:String(page-1)})}>Previous</button><span aria-live="polite">Page {page+1} of {Math.max(1,Math.ceil(data.total/size))} · {data.total} records</span><button className="secondary" disabled={(page+1)*size>=data.total} onClick={()=>navigate({page:String(page+1)})}>Next</button><label>Rows per page<select value={size} onChange={e=>navigate({size:e.target.value,page:'0'})}>{[20,50,100].map(n=><option key={n}>{n}</option>)}</select></label></div></>}
 </>}
 </main>;
}
export default function Page(){return <Suspense fallback={<main className="workspace" role="status">Opening owner workspace…</main>}><Admin/></Suspense>;}
