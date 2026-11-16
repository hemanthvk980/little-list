import type { Metadata } from 'next';
import Link from 'next/link';
import './style.css';
export const metadata: Metadata = { title: 'Little List — A little love, thoughtfully given', description: 'One thoughtful baby wishlist. Any store. Everyone you love.' };
export default function Layout({ children }: Readonly<{children: React.ReactNode}>) {
  return <html lang="en"><body><header className="nav"><Link className="brand" href="/">little list<span>✳</span></Link><nav aria-label="Main navigation"><Link href="/help">How it works</Link><Link className="sample-nav" href="/sample">Explore a sample</Link><Link className="button small" href="/dashboard">My registries <span>↗</span></Link></nav></header>{children}<footer><Link className="brand" href="/">little list<span>✳</span></Link><p>A little less guessing. A lot more love.</p><nav aria-label="Footer"><Link href="/privacy">Privacy</Link><Link href="/terms">Terms</Link><Link href="/help">Help</Link></nav><small>Made for the many ways India welcomes a little one.</small></footer></body></html>;
}
