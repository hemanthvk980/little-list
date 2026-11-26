'use client';
import {useEffect,useState,type FormEvent} from 'react';
import {Heart,Sun,Sparkles} from 'lucide-react';
import {api} from './api';
import {Feedback} from './ui';
export type Journey={chapter:string;arrivalDate:string|null;multiple:boolean;experience:string;feeling:string;countdownEnabled:boolean};
export const emptyJourney:Journey={chapter:'',arrivalDate:null,multiple:false,experience:'',feeling:'',countdownEnabled:true};
export function daysUntil(date:string,today=new Date()){const parts=date.split('-').map(Number);return Math.round((Date.UTC(parts[0],parts[1]-1,parts[2])-Date.UTC(today.getFullYear(),today.getMonth(),today.getDate()))/86400000);}
export function Arrival({id,editable=false}:{id:string;editable?:boolean}){
 const [j,setJ]=useState<Journey>(),[error,setError]=useState(''),[message,setMessage]=useState(''),[busy,setBusy]=useState(false),[today,setToday]=useState(new Date());
 useEffect(()=>{let active=true;const refresh=()=>api<Journey>('/wishlists/'+id+'/journey').then(value=>{if(active){setJ(value);setError('');}}).catch(e=>{if(active)setError(e.message);});void refresh();window.addEventListener('arrival-saved',refresh);const t=setInterval(()=>setToday(new Date()),60000);return()=>{active=false;clearInterval(t);window.removeEventListener('arrival-saved',refresh);};},[id]);
 async function save(e:FormEvent){e.preventDefault();setBusy(true);setError('');try{setJ(await api('/wishlists/'+id+'/journey','PUT',j));setMessage('Your private arrival details are saved.');window.dispatchEvent(new Event('arrival-saved'));}catch(e){setError((e as Error).message);}finally{setBusy(false);}}
 const days=j?.arrivalDate?daysUntil(j.arrivalDate,today):null;
 if(!editable&&j&&!j.countdownEnabled)return null;
 return <section className={'arrival-panel '+(editable?'arrival-edit':'')} aria-label="Your private arrival countdown"><Feedback error={error} message={message}/>{!j&&!error?<p role="status">Loading your little milestone…</p>:j&&<><div className="arrival-art" aria-hidden="true"><Sun size={24}/><Heart size={55} strokeWidth={1.3}/><Sparkles size={22}/></div><p className="eyebrow">JUST FOR YOU</p><h2>{days===null?'A new chapter awaits':days>0?<><strong>{days}</strong> days to your little hello</>:days===0?'Your estimated date is here':'One day at a time'}</h2><p>{days===null?'Add an optional date in settings for a gentle countdown.':days!<=0?'Every family has its own timing. Update or hide this whenever you like.':'An estimated arrival, a little closer each day.'}</p>{editable?<form onSubmit={save}><label>Expected arrival or homecoming <small>Optional · never shown to guests</small><input type="date" value={j.arrivalDate||''} onChange={e=>setJ({...j,arrivalDate:e.target.value||null})}/></label><label className="check-option"><input type="checkbox" checked={j.countdownEnabled} onChange={e=>setJ({...j,countdownEnabled:e.target.checked})}/>Show my countdown</label><button disabled={busy}>{busy?'Saving…':'Save arrival details'}</button></form>:<a href={'/wishlists/'+id+'/settings'}>Edit or hide countdown</a>}</>}</section>;
}
