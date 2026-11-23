'use client';
import {useId,useState} from 'react';
export const OCCASIONS=[
 {group:'Before baby arrives',values:['Baby shower','Godh bharai','Seemantham','Seemantha','Valaikappu','Dohale Jevan','Shaadh','Shrimant']},
 {group:'Welcoming your little one',values:['Welcome baby','Naming ceremony','Naamkaran','Namakaranam','Cradle ceremony','Adoption welcome']},
 {group:'Little milestones',values:['Annaprashan / first rice ceremony','Annaprasana','Choroonu','Mundan / first haircut','First birthday','Baptism / christening','Aqiqah']}
];
const choices=OCCASIONS.flatMap(g=>g.values);
export function OccasionField({value,defaultValue='',onChange,disabled=false}:{value?:string;defaultValue?:string;onChange?:(v:string)=>void;disabled?:boolean}){
 const [local,setLocal]=useState(defaultValue),[custom,setCustom]=useState(!!(value??defaultValue)&&!choices.includes(value??defaultValue));
 const current=value??local,id=useId();
 function update(v:string){setLocal(v);onChange?.(v);}
 const own=custom||(!!current&&!choices.includes(current));
 return <div className="occasion-field"><label htmlFor={id}>Occasion <small>optional</small></label><div className="occasion-select"><select id={id} disabled={disabled} value={own?'__custom':current} onChange={e=>{if(e.target.value==='__custom'){setCustom(true);update('');}else{setCustom(false);update(e.target.value);}}}><option value="">Choose an occasion</option>{OCCASIONS.map(g=><optgroup key={g.group} label={g.group}>{g.values.map(v=><option key={v} value={v}>{v}</option>)}</optgroup>)}<option value="__custom">Write my own occasion…</option></select></div>{own&&<label className="custom-occasion">Your occasion wording<input disabled={disabled} value={current} maxLength={80} onChange={e=>update(e.target.value)} placeholder="The name your family uses"/></label>}<input type="hidden" name="occasion" value={current}/><small>Choose what feels right for your family. Names and traditions vary.</small></div>;
}

