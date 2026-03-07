// ──────────────────────────────────────────────────────────────────────────────
// Utang Tracker — Borrower Remote Signing Web Form
// Replace the firebaseConfig below with your actual project config from:
// Firebase Console → Project Settings → Your apps → SDK setup and configuration
// ──────────────────────────────────────────────────────────────────────────────
import { initializeApp } from "https://www.gstatic.com/firebasejs/11.1.0/firebase-app.js";
import { getFirestore, doc, getDoc, updateDoc, serverTimestamp }
  from "https://www.gstatic.com/firebasejs/11.1.0/firebase-firestore.js";

const firebaseConfig = {
  apiKey: "AIzaSyCrsc_mtfE1Z62mFfIhEC07HaTDEbo_m28",
  authDomain: "utangtracker-81cfa.firebaseapp.com",
  projectId: "utangtracker-81cfa",
  storageBucket: "utangtracker-81cfa.firebasestorage.app",
  messagingSenderId: "223724815860",
  appId: "1:223724815860:web:d8531d6c5f7a31fd7d65a1"
};

const app = initializeApp(firebaseConfig);
const db  = getFirestore(app);

// ── Internationalization ──────────────────────────────────────────────────────
const i18n = {
  en: {
    hdrTitle:        "Kasunduan sa Utang",
    hdrSub:          "Debt Agreement — Remote Signing",
    lblPersonal:     "Personal Details",
    lblFullname:     "Full Name *",
    lblAddress:      "Home Address *",
    lblPhone:        "Phone Number *",
    lblIdtype:       "Government-issued ID Type *",
    lblIdnum:        "ID Number *",
    lblIdphotoNote:  "📷 Please send a clear photo of your government ID to the lender via Messenger, WhatsApp, or SMS so they can verify your identity.",
    lblSignature:    "Your Signature",
    lblSignHere:     "Sign above using your finger or stylus",
    lblReview:       "Review & Submit",
    btnClear:        "Clear",
    btnBack:         "Back",
    btnContinue:     "Continue →",
    btnSubmit:       "Submit Contract",
    errRequired:     "This field is required.",
    errIdFormat:     "ID number format is invalid for the selected ID type.",
    errIdPhotoRequired: "Please upload a photo of your government ID.",
    errNoSignature:  "Please draw your signature.",
    errSubmit:       "Submission failed. Please check your connection and try again.",
    reviewName:      "Name",
    reviewAddress:   "Address",
    reviewPhone:     "Phone",
    reviewId:        "ID",
    reviewIdNum:     "ID Number",
    reviewIdPhoto:   "ID Photo",
    reviewComakers:  "Co-makers",
    lblComakersHeader: "Alternative Contact / Co-maker",
    lblComakerOptional: "(Optional)",
    btnAddComaker:   "+ Add Co-maker",
    lblCmName:       "Co-maker Full Name *",
    lblCmPhone:      "Co-maker Phone Number *",
    lblCmAddress:    "Co-maker Address *",
    btnCmAdd:        "Add",
    btnCmCancel:     "Cancel",
    btnCmRemove:     "Remove",
    errCmRequired:   "Please fill in all co-maker fields.",
    lblCollateralHeader: "Collateral Items",
    lblCollateralSub: "List any items you are offering as collateral to secure this loan (e.g. phone, laptop, ATM card).",
    btnAddCollateral: "+ Add Item",
    lblCollateralItem: "Item description",
    btnColRemove:    "Remove",
    reviewCollateral: "Collateral",
    successTitle:    "Signed Successfully!",
    successSub:      "Your information has been submitted. The lender has been notified. You may close this page.",
    expiredTitle:    "Link has expired",
    expiredSub:      "This signing link is no longer valid. Please ask the lender to generate a new one.",
    errorTitle:      "Invalid link",
    errorSub:        "This contract link could not be found.",
    loadingText:     "Loading contract…",
  },
  tl: {
    hdrTitle:        "Kasunduan sa Utang",
    hdrSub:          "Kasunduan sa Utang — Remote na Pagpirma",
    lblPersonal:     "Personal na Detalye",
    lblFullname:     "Buong Pangalan *",
    lblAddress:      "Tirahan *",
    lblPhone:        "Numero ng Telepono *",
    lblIdtype:       "Uri ng Government ID *",
    lblIdnum:        "Numero ng ID *",
    lblIdphotoNote:  "📷 Mangyaring magpadala ng malinaw na larawan ng iyong government ID sa nagpautang sa pamamagitan ng Messenger, WhatsApp, o SMS para sa pag-verify ng iyong pagkakakilanlan.",
    lblSignature:    "Iyong Pirma",
    lblSignHere:     "Pumirma gamit ang daliri o stylus",
    lblReview:       "Suriin at Isumite",
    btnClear:        "I-clear",
    btnBack:         "Bumalik",
    btnContinue:     "Magpatuloy →",
    btnSubmit:       "Isumite ang Kasunduan",
    errRequired:     "Kinakailangan ang field na ito.",
    errIdFormat:     "Hindi wastong format ng ID number para sa napiling uri ng ID.",
    errNoSignature:  "Mangyaring gumawa ng iyong pirma.",
    errSubmit:       "Nabigo ang pagsusumite. Suriin ang iyong koneksyon at subukang muli.",
    reviewName:      "Pangalan",
    reviewAddress:   "Tirahan",
    reviewPhone:     "Telepono",
    reviewId:        "Uri ng ID",
    reviewIdNum:     "Numero ng ID",
    reviewComakers:  "Mga Co-maker",
    lblComakersHeader: "Alternatibong Kontak / Co-maker",
    lblComakerOptional: "(Opsyonal)",
    btnAddComaker:   "+ Magdagdag ng Co-maker",
    lblCmName:       "Buong Pangalan ng Co-maker *",
    lblCmPhone:      "Numero ng Telepono ng Co-maker *",
    lblCmAddress:    "Tirahan ng Co-maker *",
    btnCmAdd:        "Idagdag",
    btnCmCancel:     "Kanselahin",
    btnCmRemove:     "Alisin",
    errCmRequired:   "Pakipunan ang lahat ng field ng co-maker.",
    lblCollateralHeader: "Mga Collateral",
    lblCollateralSub: "Ilista ang mga item na iyong ino-offer bilang collateral (hal. telepono, laptop, ATM card).",
    btnAddCollateral: "+ Magdagdag ng Item",
    lblCollateralItem: "Paglalarawan ng item",
    btnColRemove:    "Alisin",
    reviewCollateral: "Collateral",
    successTitle:    "Matagumpay na Naipirma!",
    successSub:      "Naipadala na ang iyong impormasyon. Natanggap na ng nagpautang. Maaari mo nang isara ang pahinang ito.",
    expiredTitle:    "Nag-expire na ang link",
    expiredSub:      "Hindi na valid ang link na ito. Humingi ng bagong link sa nagpautang.",
    errorTitle:      "Di-wastong link",
    errorSub:        "Hindi mahanap ang kontratang ito.",
    loadingText:     "Nilo-load ang kasunduan…",
  },
  bis: {
    hdrTitle:        "Kasabutan sa Utang",
    hdrSub:          "Kasabutan sa Utang — Remote nga Pagpirma",
    lblPersonal:     "Personal nga Detalye",
    lblFullname:     "Tibuok nga Ngalan *",
    lblAddress:      "Adres sa Balay *",
    lblPhone:        "Numero sa Telepono *",
    lblIdtype:       "Klase sa Government ID *",
    lblIdnum:        "Numero sa ID *",
    lblIdphotoNote:  "📷 Palihug magpadala og klaro nga litrato sa imong government ID sa nagpautang pinaagi sa Messenger, WhatsApp, o SMS aron ma-verify ang imong pagkaila.",
    lblSignature:    "Imong Pirma",
    lblSignHere:     "Pirma gamit ang imong tudlo o stylus",
    lblReview:       "Surion ug Isumite",
    btnClear:        "I-clear",
    btnBack:         "Balik",
    btnContinue:     "Padayon →",
    btnSubmit:       "Isumite ang Kasabutan",
    errRequired:     "Kinahanglan kining field.",
    errIdFormat:     "Sayop ang format sa ID number para sa piniling klase sa ID.",
    errNoSignature:  "Palihug magdrowing sa imong pirma.",
    errSubmit:       "Nabigo ang pagsumite. I-check ang imong koneksyon ug sulayi pag-usab.",
    reviewName:      "Ngalan",
    reviewAddress:   "Adres",
    reviewPhone:     "Telepono",
    reviewId:        "Klase sa ID",
    reviewIdNum:     "Numero sa ID",
    reviewComakers:  "Mga Co-maker",
    lblComakersHeader: "Alternatibong Kontak / Co-maker",
    lblComakerOptional: "(Opsyonal)",
    btnAddComaker:   "+ Pagdugang og Co-maker",
    lblCmName:       "Tibuok Ngalan sa Co-maker *",
    lblCmPhone:      "Numero sa Telepono sa Co-maker *",
    lblCmAddress:    "Adres sa Co-maker *",
    btnCmAdd:        "Idugang",
    btnCmCancel:     "Kanselahon",
    btnCmRemove:     "Kuhaa",
    errCmRequired:   "Palihug pun-a ang tanan nga field sa co-maker.",
    lblCollateralHeader: "Mga Collateral",
    lblCollateralSub: "Ilista ang mga butang nga imong gi-offer isip collateral (hal. telepono, laptop, ATM card).",
    btnAddCollateral: "+ Pagdugang og Item",
    lblCollateralItem: "Deskripsyon sa butang",
    btnColRemove:    "Kuhaa",
    reviewCollateral: "Collateral",
    successTitle:    "Malampuson nga Napirma!",
    successSub:      "Naipadala na ang imong impormasyon. Gipahibalo na ang nagpautang. Mahimo na nimong isarado kining panid.",
    expiredTitle:    "Nag-expire na ang link",
    expiredSub:      "Di na valid kining link. Pangayo og bag-ong link sa nagpautang.",
    errorTitle:      "Di-valid nga link",
    errorSub:        "Dili makit-an kining kontrata.",
    loadingText:     "Gikarga ang kasabutan…",
  }
};

// ── Philippine ID format validation ──────────────────────────────────────────
const PH_ID_PATTERNS = {
  "SSS":              /^\d{2}-?\d{7}-?\d$/,
  "GSIS":             /^\d{11}$/,
  "PhilHealth":       /^\d{2}-?\d{9}-?\d$/,
  "UMID":             /^\d{4}-?\d{7}-?\d$/,
  "Passport":         /^[A-Za-z]{1,2}\d{7}[A-Za-z]?$/,
  "TIN":              /^\d{3}-?\d{3}-?\d{3}(-?\d{3})?$/,
  "Driver's License": /^[A-Za-z]\d{2}-\d{2}-\d{6}$/,
  "PRC":              /^\d{7}$/,
  "Voter's ID":       /^\d{17}$/,
  "PhilSys (National ID)": /^\d{4}-?\d{7}-?\d$/,
  "Postal ID":        /.{6,}/
};

// ── State ─────────────────────────────────────────────────────────────────────
let lang       = "en";
let step       = 0;     // 0 = preview, 1 = details, 2 = sign+review
let token      = null;
let contractData = null;
let comakers        = [];    // [{fullName, mobileNumber, address}]
let collateralItems = [];    // [string, ...]

// Signature canvas state
let sigCanvas, sigCtx, isDrawing = false, sigHasContent = false;

// ── Boot ──────────────────────────────────────────────────────────────────────
window.addEventListener("DOMContentLoaded", async () => {
  const path = window.location.pathname;
  const match = path.match(/\/sign\/([^/]+)/);
  if (!match) { showError(); return; }
  token = match[1];

  try {
    const snap = await getDoc(doc(db, "contract_requests", token));
    if (!snap.exists()) { showError(); return; }

    contractData = snap.data();
    const now = Date.now();

    if (contractData.status === "completed") {
      showScreen("screen-success"); applyLang(); return;
    }
    if (contractData.status === "expired" || contractData.expiresAt < now) {
      showScreen("screen-expired"); applyLang(); return;
    }

    renderContractPreview();
    setupSignaturePad();
    showScreen("screen-main");
    setStep(0);
    applyLang();
  } catch (e) {
    console.error(e);
    showError();
  }
});

// ── Screens ───────────────────────────────────────────────────────────────────
function showScreen(id) {
  ["screen-loading","screen-expired","screen-error","screen-success","screen-main"]
    .forEach(s => document.getElementById(s).style.display = "none");
  document.getElementById(id).style.display = "";
}
function showError() { showScreen("screen-error"); applyLang(); }

// ── Language ──────────────────────────────────────────────────────────────────
window.setLang = function(l) {
  lang = l;
  document.querySelectorAll(".lang-btn").forEach((b, i) => {
    b.classList.toggle("active", ["en","tl","bis"][i] === l);
  });
  applyLang();
  // Re-render the contract document in the new language (only when contract data is loaded)
  if (contractData && step === 0) renderContractPreview();
};

function applyLang() {
  const t = i18n[lang];
  const set = (id, val) => { const el = document.getElementById(id); if (el) el.textContent = val; };
  set("hdr-title",         t.hdrTitle);
  set("hdr-sub",           t.hdrSub);
  set("lbl-personal",      t.lblPersonal);
  set("lbl-fullname",      t.lblFullname);
  set("lbl-address",       t.lblAddress);
  set("lbl-phone",         t.lblPhone);
  set("lbl-idtype",        t.lblIdtype);
  set("lbl-idnum",         t.lblIdnum);
  set("lbl-idphoto-note",   t.lblIdphotoNote);
  set("lbl-comakers-header", t.lblComakersHeader + " ");
  set("btn-add-comaker",   t.btnAddComaker);
  set("lbl-collateral-header", t.lblCollateralHeader + " ");
  set("lbl-collateral-sub",    t.lblCollateralSub);
  set("btn-add-collateral",    t.btnAddCollateral);
  set("lbl-signature",     t.lblSignature);
  set("lbl-sign-here",     t.lblSignHere);
  set("lbl-review",        t.lblReview);
  set("btn-clear-sig",     t.btnClear);
  set("btn-back",          t.btnBack);
  set("loading-text",      t.loadingText);
  set("txt-expired-title", t.expiredTitle);
  set("txt-expired-sub",   t.expiredSub);
  set("txt-error-title",   t.errorTitle);
  set("txt-error-sub",     t.errorSub);
  set("txt-success-title", t.successTitle);
  set("txt-success-sub",   t.successSub);
  const nextBtn = document.getElementById("btn-next");
  if (nextBtn) nextBtn.textContent = step < 2 ? t.btnContinue : t.btnSubmit;
  renderComakers();
  renderCollateral();
}

// ── Contract document translations ───────────────────────────────────────────
const contractI18n = {
  en: {
    title:    "KASUNDUAN SA PAGPAPAUTANG",
    subtitle: "(LOAN AGREEMENT)",
    opening: (day, month, year, lender, borrower) =>
      `This Agreement is made this <strong>${day}</strong> day of <strong>${month}, ${year}</strong>, ` +
      `between the Lender (<strong>${lender}</strong>) and the Borrower (<strong>${borrower}</strong>). ` +
      `Both parties voluntarily agree to the following terms:`,
    sec1: "LOAN DETAILS",
    sec2: "REPAYMENT TERMS",
    sec3: "COLLATERAL",
    sec4: "DIGITAL SIGNATURES",
    amountBullet: (a, w) => `Principal Amount: <strong>${a}</strong> (${w}).`,
    purposeBullet: (p) => `Purpose: <strong>${p}</strong>.`,
    interestBullet: (r) => `Interest Rate: <strong>${r}% per month</strong>.`,
    interestNone: "Interest Rate: <strong>None</strong>.",
    dueBullet: (d) => `Due Date: Full payment is due on <strong>${d}</strong>.`,
    repay1: "The Borrower agrees to pay the principal plus the monthly interest by the Due Date.",
    repay2: "Monthly interest is calculated and summarized automatically by the app.",
    repay3: "Payments can be made earlier than the Due Date without any penalty.",
    repay4: "In case of default, the Lender reserves the right to take legal action to recover the debt.",
    col1: (c) => `The Borrower provides the following as collateral to secure this loan: <strong>${c}</strong>.`,
    col2: "In the event of default, the Lender has the right to claim possession of the stated collateral as partial or full settlement of the outstanding debt.",
    col3: "The Borrower voluntarily surrenders rights to the collateral upon failure to pay, and agrees that the Lender may liquidate or convert the collateral to cash to recover the unpaid amount.",
    col4: "Any amount recovered from the collateral that exceeds the total outstanding debt shall be returned to the Borrower.",
    col5: "The Borrower agrees not to sell, transfer, or dispose of the stated collateral without the Lender's written consent while the debt remains unpaid.",
    sigIntro: "By signing below (including digital signatures), the parties acknowledge that they have read, understood, and agreed to all terms of this Agreement.",
    footer: (num) => `Contract No: ${num} &nbsp;|&nbsp; This is a valid private document.`,
  },
  tl: {
    title:    "KASUNDUAN SA PAGPAPAUTANG",
    subtitle: "(KASUNDUAN NG UTANG)",
    opening: (day, month, year, lender, borrower) =>
      `Ang Kasunduang ito ay ginawa ngayon ika-<strong>${day}</strong> ng <strong>${month}, ${year}</strong>, ` +
      `sa pagitan ng Nagpapautang (<strong>${lender}</strong>) at ng Nangungutang (<strong>${borrower}</strong>). ` +
      `Parehong boluntaryong sumasang-ayon ang magkabilang panig sa mga sumusunod na tuntunin:`,
    sec1: "MGA DETALYE NG UTANG",
    sec2: "MGA TUNTUNIN SA PAGBABAYAD",
    sec3: "COLLATERAL (PANGAKO)",
    sec4: "MGA DIGITAL NA PIRMA",
    amountBullet: (a, w) => `Halaga ng Utang: <strong>${a}</strong> (${w}).`,
    purposeBullet: (p) => `Layunin: <strong>${p}</strong>.`,
    interestBullet: (r) => `Interes: <strong>${r}% bawat buwan</strong>.`,
    interestNone: "Interes: <strong>Wala</strong>.",
    dueBullet: (d) => `Takdang Petsa: Ang buong bayad ay dapat mabayaran sa <strong>${d}</strong>.`,
    repay1: "Sumasang-ayon ang Nangungutang na bayaran ang puhunan kasama ang buwanang interes bago mag takdang petsa.",
    repay2: "Ang buwanang interes ay awtomatikong kinakalkula at nire-ulat ng app.",
    repay3: "Maaaring magbayad nang mas maaga kaysa sa takdang petsa nang walang multa.",
    repay4: "Sa kaso ng hindi pagbabayad, may karapatang magsagawa ng legal na hakbang ang Nagpapautang para mabawi ang utang.",
    col1: (c) => `Ibinibigay ng Nangungutang ang sumusunod bilang pangako para masiguro ang utang na ito: <strong>${c}</strong>.`,
    col2: "Kung mabigo ang Nangungutang na bayaran sa takdang petsa, may karapatang angkinin ng Nagpapautang ang nabanggit na collateral bilang bahagyang o buong kabayaran ng natitirang utang.",
    col3: "Boluntaryong isinusuko ng Nangungutang ang karapatan sa collateral kung hindi makabayad, at sumasang-ayon na maaaring ibenta o i-convert ito ng Nagpapautang para mabawi ang hindi nabayarang halaga.",
    col4: "Ang anumang halaga mula sa collateral na lalampas sa kabuuang utang ay ibabalik sa Nangungutang.",
    col5: "Sumasang-ayon ang Nangungutang na hindi ibebenta, ilipat, o itatapon ang nabanggit na collateral nang walang nakasulat na pahintulot ng Nagpapautang habang hindi pa nababayaran ang utang.",
    sigIntro: "Sa pamamagitan ng pagpirma sa ibaba (kabilang ang mga digital na pirma), kinikilala ng magkabilang panig na nabasa, naunawaan, at sumasang-ayon sila sa lahat ng mga tuntunin ng Kasunduang ito.",
    footer: (num) => `Contract No: ${num} &nbsp;|&nbsp; Ito ay isang wastong pribadong dokumento.`,
  },
  bis: {
    title:    "KASABUTAN SA PAGPAPAUTANG",
    subtitle: "(KASABUTAN SA UTANG)",
    opening: (day, month, year, lender, borrower) =>
      `Kining Kasabutan gihimo niining ika-<strong>${day}</strong> adlaw sa buwan sa <strong>${month}, ${year}</strong>, ` +
      `tali sa Nagpautang (<strong>${lender}</strong>) ug sa Nanghuwam (<strong>${borrower}</strong>). ` +
      `Ang duha ka partido boluntaryong nagkauyon sa mosunod nga mga kondisyon:`,
    sec1: "MGA DETALYE SA UTANG",
    sec2: "MGA KONDISYON SA PAGBAYAD",
    sec3: "COLLATERAL (GARANTIYA)",
    sec4: "MGA DIGITAL NGA PIRMA",
    amountBullet: (a, w) => `Kantidad sa Utang: <strong>${a}</strong> (${w}).`,
    purposeBullet: (p) => `Katuyoan: <strong>${p}</strong>.`,
    interestBullet: (r) => `Interes: <strong>${r}% matag buwan</strong>.`,
    interestNone: "Interes: <strong>Wala</strong>.",
    dueBullet: (d) => `Petsa sa Bayad: Ang tibuok bayad kinahanglan mabayad sa <strong>${d}</strong>.`,
    repay1: "Nagkauyon ang Nanghuwam nga bayran ang puhunan ug buwanang interes sa wala pa ang petsa sa bayad.",
    repay2: "Ang buwanang interes awtomatikong giihap ug gi-report sa app.",
    repay3: "Mahimong mobayad og sayo kaysa sa petsa sa bayad nga walay multa.",
    repay4: "Sa kaso sa dili pagbayad, may katungod ang Nagpautang nga mobuhat og legal nga aksyon aron mabawi ang utang.",
    col1: (c) => `Gihatag sa Nanghuwam ang mosunod isip garantiya alang niining utang: <strong>${c}</strong>.`,
    col2: "Kung mapakyas ang Nanghuwam nga mobayad sa petsa sa bayad, may katungod ang Nagpautang nga kuhaon ang gihisgutang collateral isip partial o bug-os nga bayad sa nahibilin nga utang.",
    col3: "Boluntaryong gisurrender sa Nanghuwam ang iyang katungod sa collateral kung dili makabayad, ug nagkauyon nga mahimong ibaligya o i-convert kini sa kwarta sa Nagpautang aron mabawi ang wala mabayrang kantidad.",
    col4: "Ang bisan unsang kantidad gikan sa collateral nga molapas sa tibuok utang ibalik sa Nanghuwam.",
    col5: "Nagkauyon ang Nanghuwam nga dili ibaligya, ipasa, o itapon ang gihisgutang collateral nga walay nakasulat nga pagtugot sa Nagpautang samtang wala pa mabayran ang utang.",
    sigIntro: "Pinaagi sa pagpirma sa ubos (lakip na ang mga digital nga pirma), giila sa duha ka partido nga gibasa, nasabtan, ug gikuyugan nila ang tanang mga kondisyon niining Kasabutan.",
    footer: (num) => `Contract No: ${num} &nbsp;|&nbsp; Pribadong dokumento.`,
  },
};

// ── Amount to words ───────────────────────────────────────────────────────────
function numberToWords(n) {
  if (n === 0) return "Zero";
  const ones = ["","One","Two","Three","Four","Five","Six","Seven","Eight","Nine",
    "Ten","Eleven","Twelve","Thirteen","Fourteen","Fifteen","Sixteen","Seventeen","Eighteen","Nineteen"];
  const tens = ["","","Twenty","Thirty","Forty","Fifty","Sixty","Seventy","Eighty","Ninety"];
  function below1000(x) {
    if (x < 20)  return ones[x];
    if (x < 100) return tens[Math.floor(x/10)] + (x%10 > 0 ? " "+ones[x%10] : "");
    return ones[Math.floor(x/100)]+" Hundred"+(x%100 > 0 ? " "+below1000(x%100) : "");
  }
  if (n >= 1_000_000_000) return below1000(Math.floor(n/1e9))+" Billion"+(n%1e9>0?" "+numberToWords(n%1e9):"");
  if (n >= 1_000_000)     return below1000(Math.floor(n/1e6))+" Million"+(n%1e6>0?" "+numberToWords(n%1e6):"");
  if (n >= 1_000)         return below1000(Math.floor(n/1e3))+" Thousand"+(n%1e3>0?" "+numberToWords(n%1e3):"");
  return below1000(n);
}
function amountToWords(amount) {
  const whole    = Math.floor(amount);
  const centavos = Math.round((amount - whole) * 100);
  return centavos > 0
    ? `${numberToWords(whole)} Pesos and ${numberToWords(centavos)} Centavos`
    : `${numberToWords(whole)} Pesos`;
}

// ── Contract Preview — full document ─────────────────────────────────────────
function renderContractPreview() {
  const d  = contractData;
  const t  = contractI18n[lang] || contractI18n.en;
  const fmt = ts => ts
    ? new Date(ts).toLocaleDateString("en-PH", { year:"numeric", month:"long", day:"numeric" })
    : "N/A";

  const created  = new Date(d.createdAt || Date.now());
  const day      = created.getDate();
  const month    = created.toLocaleDateString("en-PH", { month:"long" });
  const year     = created.getFullYear();
  const fmtAmt   = "₱" + d.amount.toLocaleString("en-PH", { minimumFractionDigits:2 });
  const amtWords = amountToWords(d.amount);
  const dueStr   = fmt(d.dateDue);
  const hasCollateral = d.collateral && d.collateral.trim().length > 0;
  const sigNum   = hasCollateral ? "4" : "3";

  function section(num, title) {
    return `<div class="doc-section-title">${num}. ${title}</div>`;
  }
  function bullet(text) {
    return `<div class="doc-bullet"><span class="doc-bullet-dot">•</span><span>${text}</span></div>`;
  }

  let html = `
    <div class="doc-title">${t.title}</div>
    <div class="doc-subtitle">${t.subtitle}</div>
    <hr class="doc-hr"/>
    <p class="doc-body">${t.opening(day, month, year, d.lenderName, d.borrowerName)}</p>

    ${section("1", t.sec1)}
    ${bullet(t.amountBullet(fmtAmt, amtWords))}
    ${bullet(t.purposeBullet(d.purpose))}
    ${d.interestRate > 0 ? bullet(t.interestBullet(d.interestRate)) : bullet(t.interestNone)}
    ${bullet(t.dueBullet(dueStr))}

    ${section("2", t.sec2)}
    ${bullet(t.repay1)}
    ${bullet(t.repay2)}
    ${bullet(t.repay3)}
    ${bullet(t.repay4)}
  `;

  if (hasCollateral) {
    html += `
      ${section("3", t.sec3)}
      ${bullet(t.col1(d.collateral))}
      ${bullet(t.col2)}
      ${bullet(t.col3)}
      ${bullet(t.col4)}
      ${bullet(t.col5)}
    `;
  }

  html += `
    ${section(sigNum, t.sec4)}
    <p class="doc-body">${t.sigIntro}</p>
    <hr class="doc-hr"/>
    <p class="doc-footer">${t.footer(d.contractNumber)}</p>
  `;

  document.getElementById("contract-doc").innerHTML = html;
}

// ── Steps ─────────────────────────────────────────────────────────────────────
// step 0: Contract preview (sec-preview)
// step 1: Personal details (sec-details)
// step 2: Signature + review (sec-sign + sec-review)
function setStep(n) {
  step = n;
  const sections = ["sec-preview","sec-details","sec-sign","sec-review"];
  sections.forEach((id, i) => {
    document.getElementById(id).style.display =
      (i === n || (n === 2 && i === 3)) ? "" : "none";  // step 2 shows both sign+review
  });
  for (let i = 0; i < 3; i++) {
    document.getElementById("step-" + i).classList.toggle("done", i <= n);
  }
  document.getElementById("btn-back").style.display = n > 0 ? "" : "none";
  const nextBtn = document.getElementById("btn-next");
  const t = i18n[lang];
  nextBtn.textContent = n < 2 ? t.btnContinue : t.btnSubmit;
  if (n === 2) buildReview();
}

window.nextStep = function() { if (validate()) { if (step < 2) setStep(step + 1); else submit(); } };
window.prevStep = function() { if (step > 0) setStep(step - 1); };

// ── Validation ────────────────────────────────────────────────────────────────
function validate() {
  const t = i18n[lang];
  clearErrors();
  if (step === 1) {
    let ok = true;
    if (!v("inp-fullname", "err-fullname", t.errRequired)) ok = false;
    if (!v("inp-address",  "err-address",  t.errRequired)) ok = false;
    if (!v("inp-phone",    "err-phone",    t.errRequired)) ok = false;
    if (!v("sel-idtype",   "err-idtype",   t.errRequired)) ok = false;
    const idType = document.getElementById("sel-idtype").value;
    const idNum  = document.getElementById("inp-idnum").value.trim();
    if (!idNum) {
      setErr("inp-idnum", "err-idnum", t.errRequired); ok = false;
    } else if (idType && PH_ID_PATTERNS[idType] && !PH_ID_PATTERNS[idType].test(idNum)) {
      setErr("inp-idnum", "err-idnum", t.errIdFormat); ok = false;
    }
    // Validate any partially-filled comaker rows
    for (let i = 0; i < comakers.length; i++) {
      const cm = comakers[i];
      if (!cm.fullName.trim() || !cm.mobileNumber.trim() || !cm.address.trim()) {
        const el = document.getElementById(`cm-name-${i}`) || document.getElementById("btn-add-comaker");
        if (el) { el.scrollIntoView({ behavior: "smooth", block: "center" }); }
        const errDiv = document.createElement("div");
        errDiv.className = "field-error";
        errDiv.style.marginTop = "4px";
        errDiv.textContent = t.errCmRequired;
        const row = document.querySelector(`.cm-row:nth-child(${i + 1})`);
        if (row && !row.querySelector(".field-error")) row.appendChild(errDiv);
        ok = false;
        break;
      }
    }
    return ok;
  }
  if (step === 2) {
    if (!sigHasContent) {
      document.getElementById("err-signature").textContent = t.errNoSignature;
      return false;
    }
    return true;
  }
  return true;
}
function v(inputId, errId, msg) {
  const el = document.getElementById(inputId);
  if (!el.value.trim()) { setErr(inputId, errId, msg); return false; }
  return true;
}
function setErr(inputId, errId, msg) {
  document.getElementById(inputId).classList.add("error");
  document.getElementById(errId).textContent = msg;
}
function clearErrors() {
  document.querySelectorAll(".field-error").forEach(e => e.textContent = "");
  document.querySelectorAll(".error").forEach(e => e.classList.remove("error"));
}

// ── Review panel ──────────────────────────────────────────────────────────────
function buildReview() {
  const t = i18n[lang];
  const rows = [
    [t.reviewName,    document.getElementById("inp-fullname").value],
    [t.reviewAddress, document.getElementById("inp-address").value],
    [t.reviewPhone,   document.getElementById("inp-phone").value],
    [t.reviewId,      document.getElementById("sel-idtype").value],
    [t.reviewIdNum,   document.getElementById("inp-idnum").value],
  ];
  let html = rows.map(([k,v]) =>
    `<div class="review-item"><span class="review-key">${k}</span><span>${v}</span></div>`
  ).join("");
  if (comakers.length > 0) {
    html += `<div class="review-item" style="flex-direction:column;align-items:flex-start">` +
            `<span class="review-key" style="margin-bottom:6px">${t.reviewComakers}</span>`;
    comakers.forEach((cm, i) => {
      html += `<div style="font-size:13px;background:var(--surface);border:1px solid var(--border);border-radius:6px;padding:8px 10px;margin-bottom:6px;width:100%;box-sizing:border-box">` +
              `<strong>${i + 1}. ${escHtml(cm.fullName)}</strong><br>` +
              `📞 ${escHtml(cm.mobileNumber)}<br>` +
              `📍 ${escHtml(cm.address)}</div>`;
    });
    html += `</div>`;
  }
  const filledCollateral = collateralItems.filter(x => x.trim());
  if (filledCollateral.length > 0) {
    html += `<div class="review-item" style="flex-direction:column;align-items:flex-start">` +
            `<span class="review-key" style="margin-bottom:6px">${t.reviewCollateral}</span>`;
    filledCollateral.forEach((item, i) => {
      html += `<div style="font-size:13px;background:var(--surface);border:1px solid var(--border);border-radius:6px;padding:6px 10px;margin-bottom:4px;width:100%;box-sizing:border-box">` +
              `${i + 1}. ${escHtml(item)}</div>`;
    });
    html += `</div>`;
  }
  document.getElementById("review-rows").innerHTML = html;
}

// ── Comakers ──────────────────────────────────────────────────────────────────
window.addComakerRow = function() {
  const t = i18n[lang];
  const idx = comakers.length;
  comakers.push({ fullName: "", mobileNumber: "", address: "" });
  renderComakers();
  // Focus the first field of the new row
  const el = document.getElementById(`cm-name-${idx}`);
  if (el) el.focus();
};

window.removeComakerRow = function(idx) {
  comakers.splice(idx, 1);
  renderComakers();
};

function renderComakers() {
  const t = i18n[lang];
  const list = document.getElementById("comaker-list");
  if (!list) return;
  if (comakers.length === 0) { list.innerHTML = ""; return; }
  list.innerHTML = comakers.map((cm, i) => `
    <div class="cm-row" style="border:1px solid var(--border);border-radius:8px;padding:12px;margin-bottom:10px;background:var(--surface)">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
        <span style="font-weight:600;font-size:13px;color:var(--text)">Co-maker ${i + 1}</span>
        <button class="btn btn-outline btn-sm" type="button" onclick="removeComakerRow(${i})" style="padding:2px 10px;font-size:12px">${t.btnCmRemove}</button>
      </div>
      <div class="form-group" style="margin-bottom:8px">
        <label style="font-size:13px">${t.lblCmName}</label>
        <input type="text" id="cm-name-${i}" value="${escHtml(cm.fullName)}" oninput="updateComaker(${i},'fullName',this.value)" autocomplete="off" />
      </div>
      <div class="form-group" style="margin-bottom:8px">
        <label style="font-size:13px">${t.lblCmPhone}</label>
        <input type="tel" id="cm-phone-${i}" value="${escHtml(cm.mobileNumber)}" oninput="updateComaker(${i},'mobileNumber',this.value)" autocomplete="off" placeholder="e.g. 09171234567" />
      </div>
      <div class="form-group" style="margin-bottom:0">
        <label style="font-size:13px">${t.lblCmAddress}</label>
        <input type="text" id="cm-addr-${i}" value="${escHtml(cm.address)}" oninput="updateComaker(${i},'address',this.value)" autocomplete="off" />
      </div>
    </div>
  `).join("");
}

window.updateComaker = function(idx, field, value) {
  if (comakers[idx]) comakers[idx][field] = value;
};

// ── Collateral ────────────────────────────────────────────────────────────────
window.addCollateralRow = function() {
  const idx = collateralItems.length;
  collateralItems.push("");
  renderCollateral();
  const el = document.getElementById(`col-item-${idx}`);
  if (el) el.focus();
};

window.removeCollateralRow = function(idx) {
  collateralItems.splice(idx, 1);
  renderCollateral();
};

function renderCollateral() {
  const t = i18n[lang];
  const list = document.getElementById("collateral-list");
  if (!list) return;
  if (collateralItems.length === 0) { list.innerHTML = ""; return; }
  list.innerHTML = collateralItems.map((item, i) => `
    <div style="display:flex;gap:8px;align-items:center;margin-bottom:8px">
      <input type="text" id="col-item-${i}" value="${escHtml(item)}"
        oninput="updateCollateral(${i},this.value)"
        placeholder="${t.lblCollateralItem}"
        style="flex:1;padding:10px 12px;border:1px solid var(--border);border-radius:8px;font-size:14px;background:var(--surface);color:var(--text)" />
      <button class="btn btn-outline btn-sm" type="button" onclick="removeCollateralRow(${i})"
        style="padding:2px 10px;font-size:12px;white-space:nowrap">${t.btnColRemove}</button>
    </div>
  `).join("");
}

window.updateCollateral = function(idx, value) {
  if (collateralItems[idx] !== undefined) collateralItems[idx] = value;
};

function escHtml(str) {
  return (str || "").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");
}

// ── Signature Pad ─────────────────────────────────────────────────────────────
function setupSignaturePad() {
  sigCanvas = document.getElementById("sig-canvas");
  sigCtx = sigCanvas.getContext("2d");
  const resize = () => {
    const r = sigCanvas.getBoundingClientRect();
    sigCanvas.width  = r.width  * devicePixelRatio;
    sigCanvas.height = r.height * devicePixelRatio;
    sigCtx.scale(devicePixelRatio, devicePixelRatio);
    sigCtx.strokeStyle = "#000";
    sigCtx.lineWidth = 2.5;
    sigCtx.lineCap = "round";
    sigCtx.lineJoin = "round";
  };
  resize();
  new ResizeObserver(resize).observe(sigCanvas);

  const pos = e => {
    const r = sigCanvas.getBoundingClientRect();
    const src = e.touches ? e.touches[0] : e;
    return { x: src.clientX - r.left, y: src.clientY - r.top };
  };
  sigCanvas.addEventListener("mousedown",  e => { isDrawing = true; const p = pos(e); sigCtx.beginPath(); sigCtx.moveTo(p.x, p.y); });
  sigCanvas.addEventListener("mousemove",  e => { if (!isDrawing) return; const p = pos(e); sigCtx.lineTo(p.x, p.y); sigCtx.stroke(); sigHasContent = true; });
  sigCanvas.addEventListener("mouseup",    () => { isDrawing = false; });
  sigCanvas.addEventListener("touchstart", e => { e.preventDefault(); isDrawing = true; const p = pos(e); sigCtx.beginPath(); sigCtx.moveTo(p.x, p.y); }, { passive: false });
  sigCanvas.addEventListener("touchmove",  e => { e.preventDefault(); if (!isDrawing) return; const p = pos(e); sigCtx.lineTo(p.x, p.y); sigCtx.stroke(); sigHasContent = true; }, { passive: false });
  sigCanvas.addEventListener("touchend",   () => { isDrawing = false; });
}

window.clearSignature = function() {
  sigCtx.clearRect(0, 0, sigCanvas.width, sigCanvas.height);
  sigHasContent = false;
};

// ── Submit ────────────────────────────────────────────────────────────────────
async function submit() {
  const t = i18n[lang];
  const nextBtn = document.getElementById("btn-next");
  nextBtn.disabled = true;

  try {
    // Convert canvas to base64 PNG data-URI and write everything to Firestore
    const signatureBase64 = sigCanvas.toDataURL("image/png");

    await updateDoc(doc(db, "contract_requests", token), {
      status:               "completed",
      borrowerFullName:     document.getElementById("inp-fullname").value.trim(),
      borrowerAddress:      document.getElementById("inp-address").value.trim(),
      borrowerPhone:        document.getElementById("inp-phone").value.trim(),
      borrowerIdType:       document.getElementById("sel-idtype").value,
      borrowerIdNumber:     document.getElementById("inp-idnum").value.trim(),
      borrowerSignatureBase64: signatureBase64,
      comakers:             comakers.map(cm => ({
        fullName:      cm.fullName.trim(),
        mobileNumber:  cm.mobileNumber.trim(),
        address:       cm.address.trim()
      })),
      borrowerCollateral:   collateralItems.filter(x => x.trim()).join(", "),
      completedAt:          serverTimestamp()
    });

    showScreen("screen-success");
    applyLang();
  } catch (e) {
    console.error(e);
    document.getElementById("err-submit").textContent = t.errSubmit;
    nextBtn.disabled = false;
  }
}
