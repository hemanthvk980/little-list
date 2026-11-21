import type { Metadata } from 'next';
import Link from 'next/link';
import styles from './help.module.css';

export const metadata: Metadata = {
  title: 'How it works & help | Little List',
  description: 'Create a baby registry, share it with loved ones, and choose a thoughtful gift from any store.',
};

const questions = [
  ['Can I add gifts from different stores?', 'Yes. Add links from Amazon India, Flipkart, FirstCry or another store, and review the details before saving. You can also add an item to buy locally. Check the retailer for current prices, availability and delivery options.'],
  ['Do guests need to sign in with Google?', 'No. Guests can open a shared registry without a Google account. To reserve a gift or record a purchase, they verify their email and confirm the action.'],
  ['How long is a gift reserved?', 'A confirmed reservation holds one unit for 48 hours. Requesting a verification email alone does not reserve it. Use your private gift link to mark it purchased or release the hold if your plans change.'],
  ['I already bought a gift. What should I do?', 'Choose “I purchased this” on the registry, enter your email, then open the verification link and confirm your purchase. The registry updates only after confirmation. For gifts with multiple requested units, it shows how many have been purchased.'],
  ['Where is my verification email?', 'Check your spam or junk folder and make sure you entered the correct address. If the email does not arrive or confirmation fails, contact the person who shared the registry before buying, so they can help avoid duplicate gifts.'],
  ['Who can see my registry?', 'Your draft stays private until you publish it. Anyone with the published link or QR code can open it, so share it with people you trust. Revoking the link stops access through that link; existing private gift links remain usable.'],
  ['Can I share on WhatsApp or with a QR code?', 'Yes. Publish your registry, then open its sharing options to copy the link, share on WhatsApp or download the QR code. If you revoke and republish your registry, share the new link and replace any old QR codes.'],
  ['Who handles payments, delivery and returns?', 'You buy directly from the retailer or local shop. Little List records your gift choice; it does not process the order or verify the retailer’s payment. Contact the seller for order, delivery or return questions.'],
];

export default function Page() {
  return <main className={styles.page}>
    <section className={styles.hero} aria-labelledby="help-title">
      <p className="eyebrow">A LITTLE GUIDANCE, A LOT OF LOVE</p>
      <h1 id="help-title">A few helpful little things</h1>
      <p>Whether you’re welcoming a little one or choosing a gift, here’s how to make it feel easy.</p>
      <Link className="button" href="/sample">Explore a sample registry →</Link>
    </section>
    <div className={styles.guides}>
      <section className="panel" aria-labelledby="parents-title">
        <p className="eyebrow">FOR PARENTS</p>
        <h2 id="parents-title">Make a list that feels like you</h2>
        <ol>
          <li><strong>Start with your family.</strong> Sign in with Google, choose your occasion and add a personal note.</li>
          <li><strong>Add what would help.</strong> Bring together gifts from different stores, with sizes, preferences and prices in rupees.</li>
          <li><strong>Share when you’re ready.</strong> Keep your draft private, then publish and send the link or QR code to your loved ones.</li>
        </ol>
        <Link className="button secondary" href="/dashboard">Go to my registries →</Link>
      </section>
      <section className="panel" aria-labelledby="guests-title">
        <p className="eyebrow">FOR FRIENDS & FAMILY</p>
        <h2 id="guests-title">Give a little love</h2>
        <ol>
          <li><strong>Find something thoughtful.</strong> Open the shared registry and browse by category or budget.</li>
          <li><strong>Reserve if you need time.</strong> Verify your email and confirm a 48-hour hold before shopping.</li>
          <li><strong>Let the family know it’s purchased.</strong> Buy from the retailer or a local shop, then record and confirm your purchase.</li>
        </ol>
        <p className={styles.note}>Keep your private gift link safe. It lets you manage your gift, so don’t share it with others.</p>
      </section>
    </div>
    <section className={styles.faq} aria-labelledby="questions-title">
      <p className="eyebrow">GOOD TO KNOW</p>
      <h2 id="questions-title">Little questions, answered</h2>
      {questions.map(([question, answer]) => <details key={question}>
        <summary>{question}</summary><p>{answer}</p>
      </details>)}
    </section>
  </main>;
}
