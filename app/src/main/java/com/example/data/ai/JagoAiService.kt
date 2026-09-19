package com.example.data.ai

import android.util.Log
import com.example.data.eligibility.EligibilityEvaluation
import com.example.data.model.ApplicationEntity
import com.example.data.model.JagoMessage
import com.example.data.model.StudentEntity
import java.util.Locale

/**
 * Service for JAGO (Unified Multilingual Scholarship Demo Assistant).
 * Integrates contextual scholarship guidance, applicant status, and eligibility matching.
 *
 * Prototype & Privacy Guarantees:
 * - This service powers a local prototype for SIH26238 and does NOT connect to live
 *   government registries (UIDAI, DigiLocker, PFMS, e-District).
 * - Aadhaar numbers, bank account numbers, IFSC codes, and mobile numbers are strictly EXCLUDED from AI prompts.
 * - If Firebase Vertex AI or Gemini is configured in the environment, it uses the official SDK.
 * - If Firebase/Gemini credentials are absent (local demo mode), it falls back to a deterministic,
 *   transparent prototype multilingual guidance engine without fabricating fake live API data.
 */
class JagoAiService {

    companion object {
        private const val TAG = "JagoAiService"
    }

    suspend fun generateResponse(
        query: String,
        langCode: String,
        student: StudentEntity?,
        applications: List<ApplicationEntity>,
        pendingReviewCount: Int,
        unclaimedEvaluations: List<EligibilityEvaluation>
    ): JagoMessage {
        val sanitizedQuery = query.trim()

        // 1. Attempt live Gemini / Firebase AI call if available and initialized
        val liveResponse = tryLiveGeminiCall(sanitizedQuery, langCode, student, applications)
        if (liveResponse != null) {
            return liveResponse
        }

        // 2. Safe, transparent, student-aware local multilingual demo fallback
        return generateLocalContextualResponse(
            query = sanitizedQuery,
            langCode = langCode,
            student = student,
            applications = applications,
            pendingReviewCount = pendingReviewCount,
            unclaimedEvaluations = unclaimedEvaluations
        )
    }

    private suspend fun tryLiveGeminiCall(
        query: String,
        langCode: String,
        student: StudentEntity?,
        applications: List<ApplicationEntity>
    ): JagoMessage? {
        // Safe check: verify if Firebase Vertex AI is configured without throwing
        return try {
            val firebaseAppClass = Class.forName("com.google.firebase.FirebaseApp")
            val getInstanceMethod = firebaseAppClass.getMethod("getInstance")
            // If FirebaseApp is not initialized, this throws IllegalStateException
            val app = getInstanceMethod.invoke(null)
            if (app == null) return null

            // If Firebase is initialized, attempt reflective GenerativeModel invocation
            // to avoid hard compile-time binding while keeping full runtime safety
            val vertexClass = Class.forName("com.google.firebase.vertexai.FirebaseVertexAI")
            val getVertexMethod = vertexClass.getMethod("getInstance")
            val vertexInstance = getVertexMethod.invoke(null) ?: return null

            val getModelMethod = vertexClass.getMethod("generativeModel", String::class.java)
            val model = getModelMethod.invoke(vertexInstance, "gemini-1.5-flash") ?: return null

            // Build strictly sanitized context (ZERO Aadhaar, ZERO Bank info)
            val sanitizedStudentContext = if (student != null) {
                "Student: ${student.name.split(" ").firstOrNull() ?: "Student"}, State: ${student.state}, Category: ${student.category}, College: ${student.institutionName}, Applied Schemes: ${applications.joinToString { it.schemeName + " (" + it.currentStage + ")" }}"
            } else {
                "Beneficiary profile not yet registered."
            }

            val prompt = "Context: $sanitizedStudentContext. Language Code: $langCode. User Question: $query. Respond as JAGO, the demo assistant for the Ekikrit tribal scholarship prototype (SIH26238). Clarify that this is a local hackathon demonstration and keep response under 80 words."

            val generateContentMethod = model.javaClass.getMethod("generateContent", String::class.java)
            val result = generateContentMethod.invoke(model, prompt)
            val textMethod = result?.javaClass?.getMethod("getText")
            val text = textMethod?.invoke(result) as? String

            if (!text.isNullOrBlank()) {
                JagoMessage(
                    sender = "JAGO",
                    content = text.trim(),
                    quickChips = listOf("Check demo status", "Eligibility rules", "DigiLocker wallet demo")
                )
            } else {
                null
            }
        } catch (e: ClassNotFoundException) {
            try { Log.d(TAG, "Firebase Vertex AI SDK not initialized. Using local contextual engine.") } catch (_: Throwable) {}
            null
        } catch (e: Exception) {
            try { Log.d(TAG, "Gemini live call unavailable (${e.message ?: "no config"}). Falling back to local engine.") } catch (_: Throwable) {}
            null
        }
    }

    private fun generateLocalContextualResponse(
        query: String,
        langCode: String,
        student: StudentEntity?,
        applications: List<ApplicationEntity>,
        pendingReviewCount: Int,
        unclaimedEvaluations: List<EligibilityEvaluation>
    ): JagoMessage {
        val q = query.lowercase(Locale.ROOT)
        val studentName = student?.name?.ifBlank { "Beneficiary" } ?: "Beneficiary"
        val firstName = studentName.split(" ").firstOrNull() ?: "Friend"

        val isHindi = langCode == "hi"
        val isOdia = langCode == "or"
        val isGondi = langCode == "gon"

        val responseText: String
        val quickChips: List<String>

        val hasFlaggedApp = applications.any { it.hasDiscrepancy } || pendingReviewCount > 0
        val topUnclaimed = unclaimedEvaluations.firstOrNull { it.isUnclaimed }

        if (q.contains("pending") || q.contains("status") || q.contains("flag") || q.contains("अधूरा") || q.contains("ସ୍ଥିତି") || q.contains("variance") || q.contains("discrepancy")) {
            responseText = when {
                isHindi -> if (hasFlaggedApp) {
                    "नमस्ते $firstName! इस प्रोटोटाइप डेमो में नमूना रिकॉर्ड के आधार पर आय विचलन सिमुलेट किया गया है। यह गैर-अवरोधक अपवाद के रूप में Reviewer Desk कतार में भेजा गया है ताकि छात्र का आवेदन रुके बिना सहिष्णुता नियम का प्रदर्शन हो सके।"
                } else {
                    "नमस्ते $firstName! इस प्रोटोटाइप में आपके सभी डेमो आवेदन बिना किसी विचलन के सामान्य रूप से आगे बढ़ रहे हैं।"
                }
                isOdia -> if (hasFlaggedApp) {
                    "ନମସ୍କାର $firstName! ଏହି ଡେମୋ ପ୍ରୋଟୋଟାଇପ୍ରେ ନମୁନା ତଥ୍ୟ ଆଧାରରେ ଆୟ ତାରତମ୍ୟ ସିମ୍ୟୁଲେଟ୍ କରାଯାଇଛି। ଛାତ୍ରଙ୍କୁ ନ ଅଟକାଇ Reviewer Desk ରେ ଏହାର ସମାଧାନ ପ୍ରକ୍ରିୟା ପ୍ରଦର୍ଶନ କରାଯାଉଛି।"
                } else {
                    "ନମସ୍କାର $firstName! ଏହି ପ୍ରୋଟୋଟାଇପ୍ରେ ଆପଣଙ୍କ ଡେମୋ ଆବେଦନରେ କୌଣସି ଅସୁବିଧା ନାହିଁ।"
                }
                isGondi -> if (hasFlaggedApp) {
                    "जोहार $firstName! इके डेमो प्रोटोटाइप ते नमूना आमदनी फरक जांच मंता। अर्जी बंद आयो, Reviewer Desk ते नियम जांच डेमो दिसंतोर।"
                } else {
                    "जोहार $firstName! प्रोटोटाइप ते नीवा सप्पो डेमो अर्जी ठीक-ठाक मंता। कूनो रुकावट सिला।"
                }
                else -> if (hasFlaggedApp) {
                    "Hello $firstName! In this demo prototype, an income variance is simulated against sample records. The prototype auto-routes this non-blocking exception to the Reviewer Desk queue under tolerance rules for demonstration without penalizing the applicant."
                } else {
                    "Great news, $firstName! In this prototype, your demo applications are progressing normally with no pending discrepancies flagged."
                }
            }
            quickChips = listOf("Why was income flagged?", "When will amount disburse?", "Am I eligible for other schemes?")

        } else if (q.contains("eligible") || q.contains("top class") || q.contains("पात्र") || q.contains("ଯୋଗ୍ୟ") || q.contains("unreached") || q.contains("scheme")) {
            if (topUnclaimed != null) {
                responseText = when {
                    isHindi -> "नमस्ते $firstName! आपके नमूना प्रोफाइल (${student?.institutionName ?: "your institution"}) के आधार पर लोकल प्रोटोटाइप पात्रता इंजन ने '${topUnclaimed.schemeName}' (${topUnclaimed.estimatedGrant}) से मिलान किया है। आप डैशबोर्ड पर 1-क्लिक आवेदन डेमो देख सकते हैं।"
                    isOdia -> "ନମସ୍କାର $firstName! ଆପଣଙ୍କ ନମୁନା ପ୍ରୋଫାଇଲ୍ (${student?.institutionName ?: "your institution"}) ଆଧାରରେ ପ୍ରୋଟୋଟାଇପ୍ ଇଞ୍ଜିନ୍ '${topUnclaimed.schemeName}' (${topUnclaimed.estimatedGrant}) ପାଇଁ ମେଳ ଦେଖାଇଛି। ଆପଣ ଡ୍ୟାସବୋର୍ଡରୁ ଏହାର ଡେମୋ ପରୀକ୍ଷଣ କରିପାରିବେ।"
                    isGondi -> "जोहार $firstName! नीवा नमूना दाखिला (${student?.institutionName ?: "your institution"}) लेका प्रोटोटाइप इंजन '${topUnclaimed.schemeName}' (${topUnclaimed.estimatedGrant}) पात्रता दिसंतोर। Dashboard ते 1-क्लिक डेमो कूट।"
                    else -> "Hello $firstName! Based on your sample profile at ${student?.institutionName ?: "your institution"}, the local prototype eligibility engine matched '${topUnclaimed.schemeName}' (${topUnclaimed.estimatedGrant}). You can test the 1-click application workflow on your Dashboard."
                }
            } else {
                responseText = when {
                    isHindi -> "नमस्ते $firstName! इस डेमो डेटासेट में आपके प्रोफाइल स्तर (${student?.institutionName ?: "your institution"}) की सभी उपयुक्त प्रोटोटाइप योजनाओं के लिए आवेदन दर्ज हैं।"
                    isOdia -> "ନମସ୍କାର $firstName! ଏହି ଡେମୋ ତଥ୍ୟ ଅନୁସାରେ ଆପଣଙ୍କ ପ୍ରୋଫାଇଲ୍ (${student?.institutionName ?: "your institution"}) ପାଇଁ ସମସ୍ତ ମେଳ ଖାଉଥିବା ଯୋଜନା ଯୋଡା ହୋଇଛି।"
                    isGondi -> "जोहार $firstName! डेमो डेटा लेका नीकु लागू आदो सप्पो योजना (${student?.institutionName ?: "your institution"}) ते अर्जी जुड़ल मंता।"
                    else -> "Hello $firstName! In this demo dataset, all matching prototype schemes for your sample profile (${student?.institutionName ?: "your institution"}) are already applied or claimed."
                }
            }
            quickChips = listOf("Check my applications", "Which documents are needed?", "DBT Payment status")

        } else if (q.contains("disburs") || q.contains("payment") || q.contains("dbt") || q.contains("भुगतान") || q.contains("ଟଙ୍କା") || q.contains("bank")) {
            val maskedBank = student?.bankAccountMasked?.ifBlank { "Aadhaar-seeded Bank Account" } ?: "Aadhaar-seeded Bank Account"
            responseText = when {
                isHindi -> "प्रस्तावित प्रणाली में छात्रवृत्ति आधार-सीडेड $maskedBank में PFMS-DBT के माध्यम से भेजी जाती है। कृपया ध्यान दें कि इस प्रोटोटाइप में प्रदर्शित सभी डीबीटी भुगतान रिकॉर्ड और समय-सीमा केवल सिमुलेटेड डेमो डेटा हैं।"
                isOdia -> "ପ୍ରସ୍ତାବିତ ମଡେଲରେ ଛାତ୍ରବୃତ୍ତି $maskedBank କୁ PFMS-DBT ଦ୍ୱାରା ଯିବାର ବ୍ୟବସ୍ଥା ରହିଛି। ଧ୍ୟାନ ଦିଅନ୍ତୁ ଯେ ଏହି ପ୍ରୋଟୋଟାଇପ୍ରେ ଦର୍ଶାଯାଇଥିବା ସମସ୍ତ DBT ତଥ୍ୟ କେବଳ ସିମ୍ୟୁଲେଟେଡ୍ ଡେମୋ ଅଟେ।"
                isGondi -> "असली मॉडल ते पैसा $maskedBank ते PFMS-DBT रेल ते वायताल। ध्यान कीम, प्रोटोटाइप ते दिसंतो DBT पैसा अउर टाइमलाइन केवल सिमुलेटेड डेमो डेटा आंदु।"
                else -> "In the target architecture, scholarships are credited to an Aadhaar-linked $maskedBank via the PFMS-DBT rail. Please note that all DBT disbursement records, amounts, and timelines shown in this prototype are simulated demo data."
            }
            quickChips = listOf("View DBT Statement", "Track Application Status", "DigiLocker Wallet")

        } else if (q.contains("document") || q.contains("digilocker") || q.contains("कागजात") || q.contains("ଦସ୍ତାବିଜ") || q.contains("wallet")) {
            val isLinked = student?.isDigiLockerLinked == true
            responseText = when {
                isHindi -> if (isLinked) {
                    "आपके वॉलेट में नमूना डेमो प्रमाण-पत्र (आधार, एसटी जाति, आय, अंकपत्र) जुड़े हैं। यह एकीकृत सिंगल-वॉलेट मॉडल को दर्शाता है कि कैसे बिना दोबारा कागज अपलोड किए सभी योजनाओं में काम हो सकता है।"
                } else {
                    "इस प्रोफाइल के लिए डिजीलॉकर डेमो लिंक अभी बंद है। सिमुलेटेड प्रमाण-पत्र सिंक देखने के लिए दस्तावेज़ टैब से इसे कनेक्ट करें।"
                }
                isOdia -> if (isLinked) {
                    "ଆପଣଙ୍କ ୱାଲେଟରେ ନମୁନା ଡେମୋ ପ୍ରମାଣପତ୍ର ରହିଛି। ଏହା ପ୍ରଦର୍ଶନ କରେ ଯେ ଗୋଟିଏ ୱାଲେଟ୍ ମାଧ୍ୟମରେ କିପରି ବିଭିନ୍ନ ଯୋଜନାରେ ଦସ୍ତାବିଜ ପୁନଃ-ବ୍ୟବହାର ହୋଇପାରିବ।"
                } else {
                    "ଏହି ପ୍ରୋଫାଇଲ୍ ପାଇଁ DigiLocker ଡେମୋ ସଂଯୋଗ ବନ୍ଦ ଅଛି। ଡେମୋ ପ୍ରମାଣପତ୍ର ସିଙ୍କ୍ ପରୀକ୍ଷା କରିବା ପାଇଁ ୱାଲେଟ୍ ଟ୍ୟାବ୍ ବ୍ୟବହାର କରନ୍ତୁ।"
                }
                isGondi -> if (isLinked) {
                    "नीवा वॉलेट ते नमूना डेमो कागजात (आधार, एसटी, आमदनी, मार्कशीट) मंतुर। इके 1-वॉलेट डेमो मॉडल दिसंतोर।"
                } else {
                    "इके DigiLocker डेमो लिंक बंद मंता। वॉलेट टैब ते वसी डेमो सिंक जांच कीम।"
                }
                else -> if (isLinked) {
                    "Your wallet currently holds sample demo credentials (Aadhaar, ST caste, income, marksheet). This illustrates Ekikrit's single-wallet concept, showing how simulated credentials can be reused across multiple schemes without paper re-upload."
                } else {
                    "DigiLocker demo link is currently toggled off for this profile. You can connect it from the Documents tab to test simulated credential synchronization."
                }
            }
            quickChips = listOf("Open DigiLocker Tab", "Check Consent Status", "Apply for Schemes")

        } else {
            responseText = when {
                isHindi -> "जोहार $firstName! मैं जागो (JAGO) हूँ, एकीकृत जनजातीय छात्रवृत्ति प्रोटोटाइप (SIH26238) का डेमो सहायक। आज मैं आपके डेमो में क्या सहायता करूँ? आप सिमुलेटेड सत्यापन, पात्रता मिलान या डीबीटी पूर्वावलोकन की जांच कर सकते हैं।"
                isOdia -> "ଜୋହାର $firstName! ମୁଁ ଜାଗୋ (JAGO), ଏକୀକୃତ ଜନଜାତି ଛାତ୍ରବୃତ୍ତି ପ୍ରୋଟୋଟାଇପ୍ (SIH26238) ର ଡେମୋ ସହାୟକ। ଆପଣ ସିମ୍ୟୁଲେଟେଡ୍ ଯାଞ୍ଚ, ଯୋଜନା ଯୋଗ୍ୟତା ବା DBT ସିମୁଲେସନ୍ ବିଷୟରେ ପଚାରିପାରିବେ।"
                isGondi -> "जोहार $firstName! नन्ना जागो (JAGO), Ekikrit आदिवासी स्कॉलरशिप प्रोटोटाइप (SIH26238) साटी डेमो सहायक। नन्ना नीकु डेमो अर्जी, पात्रता अउर DBT टाइमलाइन बारे ते मदत कीकतोन।"
                else -> "Johar $firstName! I am JAGO, the demo assistant for the Ekikrit tribal scholarship prototype (SIH26238). How can I assist your demonstration today? You can explore simulated verification, test eligibility matching, or preview DBT timelines."
            }
            quickChips = listOf("What's pending on my application?", "Am I eligible for any scheme?", "When will amount disburse?", "Which documents are linked?")
        }

        return JagoMessage(
            sender = "JAGO",
            content = responseText,
            timestamp = "Just now",
            quickChips = quickChips
        )
    }
}
