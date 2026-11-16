'use client';
import {useEffect,useState} from 'react';

export function ShareQr({url}:{url:string}){
 const [image,setImage]=useState(''),[error,setError]=useState('');
 const target=url+'?via=qr';
 const local=new URL(url).hostname==='localhost'||new URL(url).hostname==='127.0.0.1';
 useEffect(()=>{
  let active=true;setImage('');setError('');
  import('qrcode').then(qr=>qr.toDataURL(target,{width:768,margin:4,errorCorrectionLevel:'M',color:{dark:'#000000',light:'#ffffff'}})).then(value=>{if(active)setImage(value);}).catch(()=>{if(active)setError('Unable to create the QR code. You can still copy the link.');});
  return()=>{active=false;};
 },[target]);
 return <section className="share-qr" aria-label="Registry QR code"><div>{image?<img src={image} width={224} height={224} alt="QR code for this shared registry"/>:<p role="status">{error||'Creating your QR code…'}</p>}</div><div><h3>A little scan. A lot of love.</h3><p>Save this code for your invitation or show it to family. Scans appear under QR code in Link activity.</p>{image&&<a className="button secondary" href={image} download="little-list-registry-qr.png">Download QR code</a>}<p className="quiet">Revoking the link also disables this QR code. After republishing, download a new one.</p>{local&&<p className="qr-local-note">Local demo only: this code points to localhost. A phone needs a publicly accessible app address before it can open your registry.</p>}</div></section>;
}
