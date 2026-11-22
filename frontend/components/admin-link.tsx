'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
export function AdminLink(){const [admin,setAdmin]=useState(false);useEffect(()=>{const controller=new AbortController();fetch('/api/v1/session',{cache:'no-store',signal:controller.signal}).then(r=>r.ok?r.json():null).then(s=>setAdmin(s?.admin===true)).catch(()=>{});return()=>controller.abort();},[]);return admin?<div className="workspace" style={{paddingBottom:0}}><Link href="/admin">Owner workspace →</Link></div>:null;}
