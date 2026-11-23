"use client";
import {useEffect,useRef,useState,type ReactNode} from 'react';
export function GiftDialog({children,onClose}:{children:ReactNode;onClose:()=>void}){
 const dialog=useRef<HTMLDialogElement>(null);const [dirty,setDirty]=useState(false);
 const close=()=>{if(!dirty||window.confirm('Leave without saving these gift details?'))onClose();};
 useEffect(()=>{const node=dialog.current;node?.showModal();const previous=document.body.style.overflow;document.body.style.overflow='hidden';return()=>{node?.close();document.body.style.overflow=previous;};},[]);
 return <dialog ref={dialog} className="gift-dialog" aria-labelledby="gift-dialog-title" onCancel={e=>{e.preventDefault();close();}} onChangeCapture={()=>setDirty(true)}><header><h2 id="gift-dialog-title">Review your little find</h2><button type="button" className="secondary" aria-label="Close gift preview" onClick={close}>Close</button></header>{children}</dialog>;
}
