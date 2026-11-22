'use client';
import { Fallback } from '../components/fallback';
export default function ErrorPage({reset}:{reset:()=>void}){return <><Fallback temporary/><div style={{textAlign:'center',marginBottom:40}}><button onClick={reset}>Try again</button></div></>;}
