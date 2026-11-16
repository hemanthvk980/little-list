import { SharedList } from '../../../components/client';
export const metadata={robots:{index:false,follow:false}};
export default async function Page({params}:{params:Promise<{id:string}>}){const {id}=await params;return <SharedList id={id}/>;}
