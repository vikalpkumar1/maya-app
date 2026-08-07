# Maya — apna AI assistant

Free, open-source Android AI assistant. Har user apni khud ki free Gemini API
key use karta hai, isliye poora app aur uska backend hamesha free rehta hai —
koi shared server cost nahi.

## Abhi kya kaam karta hai (v3)

- Text + voice chat — Gemini API se real AI replies (Hinglish)
- **🎧 "Hey Maya" background listening** — on karo toh app band hone ke baad
  bhi Maya sunti rehti hai (persistent notification ke saath — Android ka
  rule hai, chhupa nahi sakte), "maya" bolte hi command process hoti hai
- Mic button (app khula ho tab) — bolke poocho, Maya bolke jawab bhi deti hai
- **Continuous mode (🔁)** — har reply ke baad khud dobara sunna shuru
- **Adjustable personality (⚙ Settings)** — Humor % / Formality % sliders
- Chat history phone par save hoti hai
- Real device actions: app kholna, **naam se ya number se call/SMS**
  (Contacts permission chahiye — Settings se allow karo), alarm, calendar
  event, clipboard copy, Settings kholna, web search
- Dark glassmorphism theme, animated floating AI orb
- API key sirf phone par local — kabhi GitHub par commit nahi hoti

### Hotword ("Hey Maya") ke baare mein honest baat
Ye Siri/Google Assistant jaisi dedicated-hardware wale hotword se alag hai —
wo phone ke ek special low-power chip par chalta hai jo third-party apps ko
milta hi nahi. Maya isके bajaye Android ke built-in speech recognizer ko
loop mein chalati hai. Isliye:
- Zyada battery use hoga Siri ke comparison mein
- Jab tak "Hey Maya" listening on hai, ek notification hamesha dikhegi
  (Android ka privacy rule, iske bina background mic use hi nahi ho sakta)
- Accha kaam karne ke liye: phone Settings → System → Languages → **on-device
  speech recognition** pack download kar lo (offline + fast ho jayega),
  warna internet chahiye hoga

## Naam se call/SMS
Contacts permission Settings (⚙) se allow karni hogi. Uske baad "Mummy ko
call karo" jaisa bolne par Maya contact list mein naam dhoondh kar number
nikaal legi.

## Phone se hi build kaise karein (koi computer nahi chahiye)

1. GitHub par ek naya empty repo banao.
2. Is poore folder ko upload karo — GitHub.com ko phone browser mein "Desktop
   site" mode mein kholo, phir repo ke "Add file → Upload files" mein is zip
   ko unzip karke pura folder structure drag-drop karo (folder structure
   waisa hi rehna chahiye).
3. `main` branch par push/commit ho jaate hi `.github/workflows/build-apk.yml`
   automatically chalega aur APK build karega — GitHub Actions tab mein
   dekh sakte ho.
4. Us workflow run ke "Artifacts" section se `maya-debug-apk` download karo,
   unzip karo, aur `app-debug.apk` ko phone par install kar lo ("install
   unknown apps" allow karna padega).
5. App kholte hi apni free Gemini API key maangega — wo
   [aistudio.google.com/apikey](https://aistudio.google.com/apikey) se free
   milti hai, bas Google account se login karke generate karo.

## Roadmap — jo abhi nahi hai, agla milestone hai

Ek "sab kuch karne wala" AI ek din mein complete nahi hota — asli apps bhi
iteratively banti hain. Jo abhi baaki hai:

- **"Hey Maya" wake word** (bina button daba ke sunna) — openWakeWord ko
  TensorFlow Lite model + foreground service ke through integrate karna
- **Full device control** (notifications padhna/reply karna, settings
  toggle karna) — Accessibility Service chahiye, jo user ko manually
  Settings mein enable karna padta hai
- **Vision AI** — camera se document/plant/barcode padhna (Gemini vision
  endpoint se possible, agla add-on)
- **Student/Earn mode, scam detection, smart home** — ye sab upar wali
  chat+action foundation ke upar feature-by-feature add honge
- App icon aur launcher graphics abhi default hain

Jaise-jaise ye milestones add honge, wahi is repo mein commit hote jayenge.
