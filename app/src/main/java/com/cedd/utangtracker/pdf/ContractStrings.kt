package com.cedd.utangtracker.pdf

/** All translatable strings used in the contract document (app view + PDF). */
data class ContractStrings(
    val title: String,
    val subtitle: String,
    val openingFn: (day: Int, month: String, year: Int, lender: String, borrower: String) -> String,
    // Section headers (number is added dynamically)
    val sec1: String,
    val sec2: String,
    val sec3Collateral: String,
    val sec4Signatures: String,
    // Section 1 bullets
    val amountBullet: (amount: String, words: String) -> String,
    val purposeBullet: (purpose: String) -> String,
    val interestBullet: (rate: Double) -> String,
    val interestNone: String,
    val dueBullet: (due: String) -> String,
    // Section 2 bullets
    val repay1: String,
    val repay2: String,
    val repay3: String,
    val repay4: String,
    // Section 3 collateral bullets
    val col1: (collateral: String) -> String,
    val col2: String,
    val col3: String,
    val col4: String,
    val col5: String,
    // Signatures section
    val sigIntro: String,
    val sigLender: String,
    val sigBorrower: String,
    val sigWitness: String,
    // Footer
    val footerDisclaimer: String,
    // Timestamp label shown in PDF when borrower signed remotely
    val borrowerSignedLabel: String,
) {
    companion object {
        fun forLang(lang: String): ContractStrings = when (lang) {
            "tl"  -> tagalog()
            "bis" -> bisaya()
            else  -> english()
        }

        // ── English ──────────────────────────────────────────────────────────
        fun english() = ContractStrings(
            title    = "KASUNDUAN SA PAGPAPAUTANG",
            subtitle = "(LOAN AGREEMENT)",
            openingFn = { day, month, year, lender, borrower ->
                "This Agreement is made this $day day of $month, $year, between the Lender " +
                "($lender) and the Borrower ($borrower). Both parties voluntarily agree to " +
                "the following terms:"
            },
            sec1 = "LOAN DETAILS",
            sec2 = "REPAYMENT TERMS",
            sec3Collateral  = "COLLATERAL",
            sec4Signatures  = "DIGITAL SIGNATURES",
            amountBullet  = { a, w -> "Principal Amount: $a ($w)." },
            purposeBullet = { p -> "Purpose: $p." },
            interestBullet = { r -> "Interest Rate: ${r}% per month." },
            interestNone  = "Interest Rate: None.",
            dueBullet     = { d -> "Due Date: Full payment is due on $d." },
            repay1 = "The Borrower agrees to pay the principal plus the monthly interest by the Due Date.",
            repay2 = "Monthly interest is calculated and summarized automatically by the app.",
            repay3 = "Payments can be made earlier than the Due Date without any penalty.",
            repay4 = "In case of default, the Lender reserves the right to take legal action to recover the debt.",
            col1 = { c -> "The Borrower provides the following as collateral to secure this loan: $c." },
            col2 = "In the event of default, the Lender has the right to claim possession of the stated collateral as partial or full settlement of the outstanding debt.",
            col3 = "The Borrower voluntarily surrenders rights to the collateral upon failure to pay, and agrees that the Lender may liquidate or convert the collateral to cash to recover the unpaid amount.",
            col4 = "Any amount recovered from the collateral that exceeds the total outstanding debt shall be returned to the Borrower.",
            col5 = "The Borrower agrees not to sell, transfer, or dispose of the stated collateral without the Lender's written consent while the debt remains unpaid.",
            sigIntro = "By signing below (including digital signatures), the parties acknowledge that they have read, understood, and agreed to all terms of this Agreement.",
            sigLender   = "Lender Signature",
            sigBorrower = "Borrower Signature",
            sigWitness  = "Witnessed by:",
            footerDisclaimer = "This is a valid private document. For notarization, consult a licensed notary public.",
            borrowerSignedLabel = "Borrower signed on:",
        )

        // ── Tagalog ──────────────────────────────────────────────────────────
        fun tagalog() = ContractStrings(
            title    = "KASUNDUAN SA PAGPAPAUTANG",
            subtitle = "(KASUNDUAN NG UTANG)",
            openingFn = { day, month, year, lender, borrower ->
                "Ang Kasunduang ito ay ginawa ngayon ika-$day ng $month, $year, sa pagitan ng " +
                "Nagpapautang ($lender) at ng Nangungutang ($borrower). Parehong boluntaryong " +
                "sumasang-ayon ang magkabilang panig sa mga sumusunod na tuntunin:"
            },
            sec1 = "MGA DETALYE NG UTANG",
            sec2 = "MGA TUNTUNIN SA PAGBABAYAD",
            sec3Collateral  = "COLLATERAL (PANGAKO)",
            sec4Signatures  = "MGA DIGITAL NA PIRMA",
            amountBullet  = { a, w -> "Halaga ng Utang: $a ($w)." },
            purposeBullet = { p -> "Layunin: $p." },
            interestBullet = { r -> "Interes: ${r}% bawat buwan." },
            interestNone  = "Interes: Wala.",
            dueBullet     = { d -> "Takdang Petsa: Ang buong bayad ay dapat mabayaran sa $d." },
            repay1 = "Sumasang-ayon ang Nangungutang na bayaran ang puhunan kasama ang buwanang interes bago mag takdang petsa.",
            repay2 = "Ang buwanang interes ay awtomatikong kinakalkula at nire-ulat ng app.",
            repay3 = "Maaaring magbayad nang mas maaga kaysa sa takdang petsa nang walang multa.",
            repay4 = "Sa kaso ng hindi pagbabayad, may karapatang magsagawa ng legal na hakbang ang Nagpapautang para mabawi ang utang.",
            col1 = { c -> "Ibinibigay ng Nangungutang ang sumusunod bilang pangako para masiguro ang utang na ito: $c." },
            col2 = "Kung mabigo ang Nangungutang na bayaran sa takdang petsa, may karapatang angkinin ng Nagpapautang ang nabanggit na collateral bilang bahagyang o buong kabayaran ng natitirang utang.",
            col3 = "Boluntaryong isinusuko ng Nangungutang ang karapatan sa collateral kung hindi makabayad, at sumasang-ayon na maaaring ibenta o i-convert ito ng Nagpapautang para mabawi ang hindi nabayarang halaga.",
            col4 = "Ang anumang halaga mula sa collateral na lalampas sa kabuuang utang ay ibabalik sa Nangungutang.",
            col5 = "Sumasang-ayon ang Nangungutang na hindi ibebenta, ilipat, o itatapon ang nabanggit na collateral nang walang nakasulat na pahintulot ng Nagpapautang habang hindi pa nababayaran ang utang.",
            sigIntro = "Sa pamamagitan ng pagpirma sa ibaba (kabilang ang mga digital na pirma), kinikilala ng magkabilang panig na nabasa, naunawaan, at sumasang-ayon sila sa lahat ng mga tuntunin ng Kasunduang ito.",
            sigLender   = "Pirma ng Nagpapautang",
            sigBorrower = "Pirma ng Nangungutang",
            sigWitness  = "Pinatotohanan ni:",
            footerDisclaimer = "Ito ay isang wastong pribadong dokumento. Para sa notaryo, kumonsulta sa lisensyadong notary public.",
            borrowerSignedLabel = "Petsa ng pagpirma ng Nangungutang:",
        )

        // ── Bisaya ───────────────────────────────────────────────────────────
        fun bisaya() = ContractStrings(
            title    = "KASABUTAN SA PAGPAPAUTANG",
            subtitle = "(KASABUTAN SA UTANG)",
            openingFn = { day, month, year, lender, borrower ->
                "Kining Kasabutan gihimo niining ika-$day adlaw sa buwan sa $month, $year, tali " +
                "sa Nagpautang ($lender) ug sa Nanghuwam ($borrower). Ang duha ka partido " +
                "boluntaryong nagkauyon sa mosunod nga mga kondisyon:"
            },
            sec1 = "MGA DETALYE SA UTANG",
            sec2 = "MGA KONDISYON SA PAGBAYAD",
            sec3Collateral  = "COLLATERAL (GARANTIYA)",
            sec4Signatures  = "MGA DIGITAL NGA PIRMA",
            amountBullet  = { a, w -> "Kantidad sa Utang: $a ($w)." },
            purposeBullet = { p -> "Katuyoan: $p." },
            interestBullet = { r -> "Interes: ${r}% matag buwan." },
            interestNone  = "Interes: Wala.",
            dueBullet     = { d -> "Petsa sa Bayad: Ang tibuok bayad kinahanglan mabayad sa $d." },
            repay1 = "Nagkauyon ang Nanghuwam nga bayran ang puhunan ug buwanang interes sa wala pa ang petsa sa bayad.",
            repay2 = "Ang buwanang interes awtomatikong giihap ug gi-report sa app.",
            repay3 = "Mahimong mobayad og sayo kaysa sa petsa sa bayad nga walay multa.",
            repay4 = "Sa kaso sa dili pagbayad, may katungod ang Nagpautang nga mobuhat og legal nga aksyon aron mabawi ang utang.",
            col1 = { c -> "Gihatag sa Nanghuwam ang mosunod isip garantiya alang niining utang: $c." },
            col2 = "Kung mapakyas ang Nanghuwam nga mobayad sa petsa sa bayad, may katungod ang Nagpautang nga kuhaon ang gihisgutang collateral isip partial o bug-os nga bayad sa nahibilin nga utang.",
            col3 = "Boluntaryong gisurrender sa Nanghuwam ang iyang katungod sa collateral kung dili makabayad, ug nagkauyon nga mahimong ibaligya o i-convert kini sa kwarta sa Nagpautang aron mabawi ang wala mabayrang kantidad.",
            col4 = "Ang bisan unsang kantidad gikan sa collateral nga molapas sa tibuok utang ibalik sa Nanghuwam.",
            col5 = "Nagkauyon ang Nanghuwam nga dili ibaligya, ipasa, o itapon ang gihisgutang collateral nga walay nakasulat nga pagtugot sa Nagpautang samtang wala pa mabayran ang utang.",
            sigIntro = "Pinaagi sa pagpirma sa ubos (lakip na ang mga digital nga pirma), giila sa duha ka partido nga gibasa, nasabtan, ug gikuyugan nila ang tanang mga kondisyon niining Kasabutan.",
            sigLender   = "Pirma sa Nagpautang",
            sigBorrower = "Pirma sa Nanghuwam",
            sigWitness  = "Gisaksi ni:",
            footerDisclaimer = "Pribadong dokumento. Para sa notaryo, kumonsulta sa lisensyadong notary public.",
            borrowerSignedLabel = "Petsa sa pagpirma sa Nanghuwam:",
        )
    }
}
