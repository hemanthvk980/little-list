import Link from 'next/link';
import { Fallback } from '../../components/fallback';
export const metadata={robots:{index:false,follow:false}};
export default async function Page({searchParams}:{searchParams:Promise<{retry?:string}>}){
 const {retry}=await searchParams;
 const safe=retry && /^\/r\/[A-Za-z0-9_-]{22}(\?via=[A-Za-z0-9_%.-]{1,80})?$/.test(retry);
 return <><Fallback temporary/>{safe&&<p style={{textAlign:'center'}}><Link className="button" prefetch={false} href={retry}>Try this registry again</Link></p>}</>;
}
