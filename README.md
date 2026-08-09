# Maya — apna AI assistant

Free, open-source Android AI assistant. Har user apni khud ki free Gemini API
key use karta hai, isliye poora app aur uska backend hamesha free rehta hai —
koi shared server cost nahi.

## Abhi kya kaam karta hai

**Chat & Voice**
- Text + voice chat — Gemini API se real AI replies, adjustable language
  (Hinglish/Hindi/English/Spanish) aur personality (Humor %/Formality %)
- Mic button — bolke poocho, Maya bolke jawab bhi deti hai (TTS speed/style
  bhi badal sakte ho)
- **Continuous mode (🔁)** — har reply ke baad khud dobara sunna shuru
- **🎧 "Hey Maya" background listening** — battery-optimized (wake lock
  sirf active listening ke waqt, idle par restart slow hota hai, 1 ghante
  baad khud band ho jaata hai), reboot ke baad khud restart ho jaati hai
- Chat history phone par local save hoti hai

**Device actions (seedha baat karke)**
- App kholna, naam se ya number se call/SMS/WhatsApp, email draft
- Alarm, calendar event, reminder (waqt par notification)
- Volume, brightness, Bluetooth panel, web page kholna, web search
- Weather (live, free — Open-Meteo), unread SMS padhna, RAM usage
- Clipboard copy, Settings kholna
- **🔗 Multi-step chaining** — "X karo phir Y karo" jaisa ek se zyada kaam
  ek request mein (max 4 steps, taaki loop kabhi infinite na ho)

**Security & personalization**
- **🔑 Permission checklist** — pehli baar app khulte hi (aur baad mein
  Settings se kabhi bhi) ek screen dikhti hai jahan har permission ke
  saamne ek button hai — dabao aur seedha sahi jagah khul jaata hai
  (ya to system ka Allow/Deny popup, ya Settings ka exact screen) —
  khud dhoondhna nahi padta
- **🔒 App lock** — Settings mein on karo, face/fingerprint se unlock
  (biometric na ho toh app bahar nahi rakhegi)
- Naam se call/SMS/WhatsApp ke liye Contacts permission chahiye (checklist se)

Dark glassmorphism theme, animated floating AI orb. API key sirf phone par
local rehti hai — kabhi GitHub par commit nahi hoti.

## Hotword ("Hey Maya") ke baare mein honest baat
Siri/Google Assistant dedicated low-power hardware chip use karte hain jo
third-party apps ko milta hi nahi. Maya iske bajaye Android ke built-in
speech recognizer ko loop mein chalati hai:
- Zyada battery use hoga Siri ke comparison mein (kaafi optimize kiya hai)
- Jab tak on hai, notification hamesha dikhegi (Android ka privacy rule)
- **Vivo/Xiaomi/Oppo jaise phones** background services ko bahut aggressively
  maarte hain — sab settings sahi karne ke baad bhi 100% guarantee nahi.
  Agar phir bhi na chale, **mic button + Continuous mode (🔁)** hi sabse
  reliable tareeka hai
- Behtar kaam karne ke liye: phone Settings → System → Languages →
  **on-device speech recognition** pack download kar lo

## Jaan-boojhkar kya nahi kiya (aur kyun)
- **Google Calendar/Gmail/Notion/Slack/GitHub "workspace sync"** — har ek
  ka apna OAuth developer app register karna padta hai (client secret ke
  saath), jo ek public GitHub repo mein kabhi safe nahi rehti. Calendar
  aur Email abhi indirect tarike se kaam karte hain (native app khulta
  hai, tum confirm karte ho) — asli background API-level sync alag, bada
  project hai
- **Screen padhna/samajhna** — Android ka sabse fragile API hai (screen
  capture + threading bugs aasani se aate hain), isse rush nahi karna
  chahta — agli baar isi par pura focus dena behtar hoga
- **Groq AI switch** — do-alag-AI-provider architecture hai, stable Gemini
  flow ko risk mein nahi daala
- **ADB connection** — security risk hai personal assistant mein
- **Offline 70B LLM** — phone par practically possible nahi
- **Doosre app force-band karna, poora file/cloud search, screenshot,
  emotion analysis, CPU live stats, usage analytics** — sab alag reasons
  se scope se bahar (Android restrictions ya bada alag project) — poori
  list chat mein maangi toh bata dunga

## Phone se hi build kaise karein (koi computer nahi chahiye)

1. GitHub par ek naya empty repo banao.
2. Codespace kholo (Code → Codespaces → Create codespace on main)
3. Zip ko Explorer mein upload karo (right-click → Upload...)
4. Terminal mein:
   ```
   unzip -o maya-app.zip
   rm maya-app.zip
   git add .
   git commit -m "Update"
   git push
   ```
5. Repo ke **Actions** tab mein APK apne aap ban jaayegi (~2-3 min) —
   Artifacts se `maya-debug-apk` download karo, unzip karo, install karo
6. Pehli baar khulte hi apni free Gemini API key maangega —
   [aistudio.google.com/apikey](https://aistudio.google.com/apikey) se free
   milti hai
