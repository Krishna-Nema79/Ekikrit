package com.example.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.example.data.model.AppLanguage

data class AppStrings(
    // Top Bar & Branding
    val appTitle: String,
    val portalSubtitle: String,
    val ministryName: String,
    val studentMode: String,
    val reviewerDeskMode: String,
    val appGuide: String,
    val securityMenu: String,
    val auditTrailMenu: String,
    val languageMenu: String,
    val replayTourMenu: String,
    val selectLanguageTitle: String,

    // Navigation Tabs
    val tabDashboard: String,
    val tabSchemes: String,
    val tabWallet: String,
    val tabDbtRail: String,
    val tabReviewDesk: String,

    // Dashboard Hero & Student Profile
    val welcomePrefix: String,
    val nspPfmsActive: String,
    val portalName: String,
    val pvtgBadge: String,
    val aadhaarVerified: String,
    val statDisbursed: String,
    val statInPipeline: String,
    val statDigiLocker: String,
    val zeroPaperwork: String,

    // Filters
    val filterAll: String,
    val filterNeedsAction: String,
    val filterApproved: String,
    val filterInProgress: String,

    // Sections
    val fiveSchemesTitle: String,
    val fiveSchemesSubtitle: String,
    val upcomingDisbursementsTitle: String,
    val actionRequiredTitle: String,
    val viewDetails: String,
    val sanctionAmount: String,
    val deadline: String,
    val statusVerified: String,
    val statusPending: String,
    val statusDiscrepancy: String,

    // DigiLocker Wallet
    val digiLockerWalletTitle: String,
    val digiLockerWalletSubtitle: String,
    val digiLockerConnected: String,
    val digiLockerNotConnected: String,
    val connectDigiLockerBtn: String,
    val syncCredentialsBtn: String,
    val fetchMoreDocsBtn: String,
    val zeroPaperworkGuarantee: String,
    val attachedCredentialsTitle: String,
    val tapToInspect: String,
    val reusableBadge: String,

    // Common Actions
    val close: String,
    val cancel: String,
    val applyNow: String,
    val askJago: String
)

val EnglishStrings = AppStrings(
    appTitle = "Ekikrit",
    portalSubtitle = "Unified Tribal Scholarship Portal",
    ministryName = "Ministry of Tribal Affairs",
    studentMode = "Student",
    reviewerDeskMode = "Review Desk",
    appGuide = "App Guide",
    securityMenu = "Security & DPDP Posture",
    auditTrailMenu = "DPDP Audit Log Trail",
    languageMenu = "App Language",
    replayTourMenu = "Replay App Intro Tour",
    selectLanguageTitle = "Select App Language",

    tabDashboard = "Dashboard",
    tabSchemes = "5 Schemes",
    tabWallet = "Wallet",
    tabDbtRail = "DBT Rail",
    tabReviewDesk = "Review Desk",

    welcomePrefix = "Welcome",
    nspPfmsActive = "NSP & PFMS Active",
    portalName = "Unified Tribal Scholarship Portal",
    pvtgBadge = "PVTG: Birhor",
    aadhaarVerified = "Aadhaar Verified",
    statDisbursed = "Disbursed",
    statInPipeline = "In Pipeline",
    statDigiLocker = "DigiLocker",
    zeroPaperwork = "0 Paperwork",

    filterAll = "All",
    filterNeedsAction = "Needs Action",
    filterApproved = "Approved",
    filterInProgress = "Under Review",

    fiveSchemesTitle = "Five Unified Tribal Schemes",
    fiveSchemesSubtitle = "Single application, zero duplicate paperwork, synchronized verification",
    upcomingDisbursementsTitle = "Upcoming DBT Disbursements",
    actionRequiredTitle = "Action Required",
    viewDetails = "View Details",
    sanctionAmount = "Sanction Amount",
    deadline = "Deadline",
    statusVerified = "Verified",
    statusPending = "Under Verification",
    statusDiscrepancy = "Exception Review",

    digiLockerWalletTitle = "DigiLocker Document Wallet",
    digiLockerWalletSubtitle = "Pull Once • Reusable Across All 5 Schemes",
    digiLockerConnected = "DigiLocker Account Linked (Aadhaar Seeded)",
    digiLockerNotConnected = "DigiLocker Not Linked",
    connectDigiLockerBtn = "Connect DigiLocker Account",
    syncCredentialsBtn = "Sync Credentials",
    fetchMoreDocsBtn = "Fetch More Documents from DigiLocker",
    zeroPaperworkGuarantee = "Zero Paperwork Guarantee: All credentials carry digital signatures verified against National Root Authority.",
    attachedCredentialsTitle = "Attached Digital Credentials",
    tapToInspect = "Tap to Inspect Cryptographic Seal",
    reusableBadge = "Reusable (5 Schemes)",

    close = "Close",
    cancel = "Cancel",
    applyNow = "Apply Now",
    askJago = "Ask JAGO"
)

val HindiStrings = AppStrings(
    appTitle = "एकीकृत",
    portalSubtitle = "एकीकृत जनजातीय छात्रवृत्ति पोर्टल",
    ministryName = "जनजातीय कार्य मंत्रालय, भारत सरकार",
    studentMode = "छात्र मोड",
    reviewerDeskMode = "समीक्षा डेस्क",
    appGuide = "ऐप गाइड",
    securityMenu = "सुरक्षा एवं डीपीआई संरक्षण",
    auditTrailMenu = "डीपीडीपी ऑडिट लॉग",
    languageMenu = "ऐप भाषा बदलें",
    replayTourMenu = "ऐप परिचय दोबारा देखें",
    selectLanguageTitle = "ऐप की भाषा चुनें",

    tabDashboard = "डैशबोर्ड",
    tabSchemes = "5 योजनाएं",
    tabWallet = "दस्तावेज़ वॉलेट",
    tabDbtRail = "डीबीटी रेल",
    tabReviewDesk = "समीक्षा डेस्क",

    welcomePrefix = "स्वागत है",
    nspPfmsActive = "एनएसपी एवं पीएफएमएस सक्रिय",
    portalName = "एकीकृत जनजातीय छात्रवृत्ति पोर्टल",
    pvtgBadge = "विशेष रूप से कमजोर जनजाति (बिरहोर)",
    aadhaarVerified = "आधार सत्यापित",
    statDisbursed = "खाते में प्राप्त",
    statInPipeline = "प्रक्रिया में",
    statDigiLocker = "डिजिलॉकर",
    zeroPaperwork = "0 कागजी कार्रवाई",

    filterAll = "सभी (5)",
    filterNeedsAction = "कार्रवाई जरूरी",
    filterApproved = "स्वीकृत",
    filterInProgress = "समीक्षाधीन",

    fiveSchemesTitle = "पांच एकीकृत जनजातीय छात्रवृत्ति योजनाएं",
    fiveSchemesSubtitle = "एकल आवेदन, बिना दोबारा दस्तावेज अपलोड किए, एकीकृत सत्यापन",
    upcomingDisbursementsTitle = "आगामी डीबीटी बैंक हस्तांतरण",
    actionRequiredTitle = "कार्रवाई आवश्यक",
    viewDetails = "विवरण देखें",
    sanctionAmount = "स्वीकृत राशि",
    deadline = "अंतिम तिथि",
    statusVerified = "सत्यापित",
    statusPending = "सत्यापनाधीन",
    statusDiscrepancy = "अपवाद समीक्षा",

    digiLockerWalletTitle = "डिजिलॉकर दस्तावेज़ वॉलेट",
    digiLockerWalletSubtitle = "एक बार लाएं • सभी 5 योजनाओं में पुनः उपयोग करें",
    digiLockerConnected = "डिजिलॉकर खाता लिंक है (आधार सीडेड)",
    digiLockerNotConnected = "डिजिलॉकर लिंक नहीं है",
    connectDigiLockerBtn = "डिजिलॉकर खाता जोड़ें",
    syncCredentialsBtn = "दस्तावेज़ सिंक करें",
    fetchMoreDocsBtn = "डिजिलॉकर से और दस्तावेज़ प्राप्त करें",
    zeroPaperworkGuarantee = "शून्य कागजी कार्रवाई गारंटी: सभी प्रमाण पत्र राष्ट्रीय रूट प्राधिकरण द्वारा डिजिटल रूप से हस्ताक्षरित हैं।",
    attachedCredentialsTitle = "संलग्न डिजिटल दस्तावेज़",
    tapToInspect = "डिजिटल मुहर देखने के लिए स्पर्श करें",
    reusableBadge = "पुनः प्रयोज्य (5 योजनाएं)",

    close = "बंद करें",
    cancel = "रद्द करें",
    applyNow = "आवेदन करें",
    askJago = "जागो से पूछें"
)

val OdiaStrings = AppStrings(
    appTitle = "ଏକୀକୃତ",
    portalSubtitle = "ଏକୀକୃତ ଜନଜାତି ଛାତ୍ରବୃତ୍ତି ପୋର୍ଟାଲ୍",
    ministryName = "ଜନଜାତି ବ୍ୟାପାର ମନ୍ତ୍ରଣାଳୟ",
    studentMode = "ଛାତ୍ର ମୋଡ୍",
    reviewerDeskMode = "ସମୀକ୍ଷା ଡେସ୍କ",
    appGuide = "ଆପ୍ ଗାଇଡ୍",
    securityMenu = "ସୁରକ୍ଷା ଓ ଡିପିଡିପି ସ୍ଥିତି",
    auditTrailMenu = "ଡିପିଡିପି ଅଡିଟ୍ ଲଗ୍ ଟ୍ରେଲ୍",
    languageMenu = "ଆପ୍ ଭାଷା ବଦଳାନ୍ତୁ",
    replayTourMenu = "ଆପ୍ ଟୁର୍ ପୁନର୍ବାର ଦେଖନ୍ତୁ",
    selectLanguageTitle = "ଆପ୍ ଭାଷା ଚୟନ କରନ୍ତୁ",

    tabDashboard = "ଡ୍ୟାସବୋର୍ଡ",
    tabSchemes = "୫ ଯୋଜନା",
    tabWallet = "ୱାଲେଟ୍",
    tabDbtRail = "ଡିବିଟି ରେଳ",
    tabReviewDesk = "ସମୀକ୍ଷା ଡେସ୍କ",

    welcomePrefix = "ସ୍ୱାଗତ",
    nspPfmsActive = "NSP ଓ PFMS ସକ୍ରିୟ",
    portalName = "ଏକୀକୃତ ଜନଜାତି ଛାତ୍ରବୃତ୍ତି ପୋର୍ଟାଲ୍",
    pvtgBadge = "PVTG: ବିରହୋର",
    aadhaarVerified = "ଆଧାର ଯାଞ୍ଚ ହୋଇଛି",
    statDisbursed = "ଜମା ହୋଇଛି",
    statInPipeline = "ପ୍ରକ୍ରିୟାରେ ଅଛି",
    statDigiLocker = "ଡିଜିଲକର୍",
    zeroPaperwork = "୦ କାଗଜପତ୍ର",

    filterAll = "ସମସ୍ତ (୫)",
    filterNeedsAction = "କାର୍ଯ୍ୟାନୁଷ୍ଠାନ ଆବଶ୍ୟକ",
    filterApproved = "ଅନୁମୋଦିତ",
    filterInProgress = "ଯାଞ୍ଚ ଚାଲିଛି",

    fiveSchemesTitle = "ପାଞ୍ଚଟି ଏକୀକୃତ ଜନଜାତି ଯୋଜନା",
    fiveSchemesSubtitle = "ଗୋଟିଏ ଆବେଦନ, କୌଣସି ଅତିରିକ୍ତ କାଗଜପତ୍ର ବିନା, ସ୍ୱୟଂଚାଳିତ ଯାଞ୍ଚ",
    upcomingDisbursementsTitle = "ଆଗାମୀ DBT ବ୍ୟାଙ୍କ୍ ପେମେଣ୍ଟ",
    actionRequiredTitle = "କାର୍ଯ୍ୟାନୁଷ୍ଠାନ ଆବଶ୍ୟକ",
    viewDetails = "ବିବରଣୀ ଦେଖନ୍ତୁ",
    sanctionAmount = "ମଞ୍ଜୁର ରାଶି",
    deadline = "ଶେଷ ତାରିଖ",
    statusVerified = "ଯାଞ୍ଚ ହୋଇଛି",
    statusPending = "ଯାଞ୍ଚ ଚାଲିଛି",
    statusDiscrepancy = "ବ୍ୟତିକ୍ରମ ସମୀକ୍ଷା",

    digiLockerWalletTitle = "ଡିଜିଲକର୍ ଦସ୍ତାବିଜ୍ ୱାଲେଟ୍",
    digiLockerWalletSubtitle = "ଥରେ ଆଣନ୍ତୁ • ସମସ୍ତ ୫ ଯୋଜନାରେ ବ୍ୟବହାର କରନ୍ତୁ",
    digiLockerConnected = "ଡିଜିଲକର୍ ସଂଯୋଗ ହୋଇଛି (ଆଧାର ସିଡିଂ ସମ୍ପୂର୍ଣ୍ଣ)",
    digiLockerNotConnected = "ଡିଜିଲକର୍ ସଂଯୁକ୍ତ ନୁହେଁ",
    connectDigiLockerBtn = "ଡିଜିଲକର୍ ଆକାଉଣ୍ଟ ସଂଯୋଗ କରନ୍ତୁ",
    syncCredentialsBtn = "ଦସ୍ତାବିଜ୍ ସିଙ୍କ୍ କରନ୍ତୁ",
    fetchMoreDocsBtn = "ଡିଜିଲକରରୁ ଅଧିକ ଦସ୍ତାବିଜ୍ ଆଣନ୍ତୁ",
    zeroPaperworkGuarantee = "ଶୂନ୍ୟ କାଗଜପତ୍ର ଗ୍ୟାରେଣ୍ଟି: ସମସ୍ତ ପ୍ରମାଣପତ୍ର ଜାତୀୟ ରୁଟ୍ କର୍ତ୍ତୃପକ୍ଷଙ୍କ ଦ୍ୱାରା ଡିଜିଟାଲ୍ ସ୍ୱାକ୍ଷରିତ।",
    attachedCredentialsTitle = "ସଂଲଗ୍ନ ଡିଜିଟାଲ୍ ଦସ୍ତାବିଜ୍",
    tapToInspect = "ଡିଜିଟାଲ୍ ସିଲ୍ ଯାଞ୍ଚ ପାଇଁ ସ୍ପର୍ଶ କରନ୍ତୁ",
    reusableBadge = "ପୁନଃ ବ୍ୟବହାର୍ଯ୍ୟ (୫ ଯୋଜନା)",

    close = "ବନ୍ଦ କରନ୍ତୁ",
    cancel = "ବାତିଲ କରନ୍ତୁ",
    applyNow = "ଆବେଦନ କରନ୍ତୁ",
    askJago = "ଜାଗୋଙ୍କୁ ପଚାରନ୍ତୁ"
)

val GondiStrings = AppStrings(
    appTitle = "एकिकृत",
    portalSubtitle = "एकिकृत जनजातीय छात्रवृत्ति पोर्टल",
    ministryName = "जनजातीय कार्य मंत्रालय",
    studentMode = "विद्यार्थी मोड",
    reviewerDeskMode = "समीक्षा डेस्क",
    appGuide = "ऐप गाइड",
    securityMenu = "सुरक्षा व्यवस्था",
    auditTrailMenu = "डीपीडीपी ऑडिट विवरण",
    languageMenu = "भाषा बदलावा",
    replayTourMenu = "ऐप परिचय फेरून बघा",
    selectLanguageTitle = "भाषा निवडा",

    tabDashboard = "डैशबोर्ड",
    tabSchemes = "5 योजना",
    tabWallet = "दस्तावेज वॉलेट",
    tabDbtRail = "डीबीटी रेल",
    tabReviewDesk = "समीक्षा डेस्क",

    welcomePrefix = "जोहार",
    nspPfmsActive = "एनएसपी एवं पीएफएमएस सक्रिय",
    portalName = "एकिकृत जनजातीय छात्रवृत्ति पोर्टल",
    pvtgBadge = "पीवीटीजी (बिरहोर समुदाय)",
    aadhaarVerified = "आधार तपासलेल",
    statDisbursed = "जमा रक्कम",
    statInPipeline = "प्रक्रियेत",
    statDigiLocker = "डिजिलॉकर",
    zeroPaperwork = "0 कागदपत्र",

    filterAll = "सगळे (5)",
    filterNeedsAction = "तातडीचे काम",
    filterApproved = "मंजूर",
    filterInProgress = "तपासणी चालू",

    fiveSchemesTitle = "पाच एकिकृत आदिवासी योजना",
    fiveSchemesSubtitle = "एकच अर्ज, पुन्हा कागदपत्रे नाही, थेट पडताळणी",
    upcomingDisbursementsTitle = "येणारे डीबीटी बँक पैसे",
    actionRequiredTitle = "तातडीचे काम",
    viewDetails = "माहिती बघा",
    sanctionAmount = "मंजूर रक्कम",
    deadline = "अंतिम तारीख",
    statusVerified = "सत्यापित",
    statusPending = "तपासणीत",
    statusDiscrepancy = "विशेष तपासणी",

    digiLockerWalletTitle = "डिजिलॉकर दस्तावेज वॉलेट",
    digiLockerWalletSubtitle = "एकदा आणा • सर्व 5 योजनांसाठी वापरा",
    digiLockerConnected = "डिजिलॉकर जोडलेले आहे (आधार लिंक)",
    digiLockerNotConnected = "डिजिलॉकर जोडलेले नाही",
    connectDigiLockerBtn = "डिजिलॉकर जोडा",
    syncCredentialsBtn = "दस्तावेज सिंक करा",
    fetchMoreDocsBtn = "डिजिलॉकरमधून आणखी कागदपत्रे आणा",
    zeroPaperworkGuarantee = "कागदपत्र मुक्त हमी: सर्व प्रमाणपत्रे अधिकृतरीत्या डिजिटल स्वाक्षरीसह सुरक्षित आहेत.",
    attachedCredentialsTitle = "जोडलेली डिजिटल कागदपत्रे",
    tapToInspect = "डिजिटल सील तपासण्यासाठी क्लिक करा",
    reusableBadge = "पुनर्वापर (5 योजना)",

    close = "बंद करा",
    cancel = "रद्द करा",
    applyNow = "अर्ज करा",
    askJago = "जागोला विचारा"
)

fun getAppStrings(language: AppLanguage): AppStrings {
    return when (language) {
        AppLanguage.ENGLISH -> EnglishStrings
        AppLanguage.HINDI -> HindiStrings
        AppLanguage.ODIA -> OdiaStrings
        AppLanguage.GONDI -> GondiStrings
    }
}

val LocalAppStrings = compositionLocalOf { EnglishStrings }
